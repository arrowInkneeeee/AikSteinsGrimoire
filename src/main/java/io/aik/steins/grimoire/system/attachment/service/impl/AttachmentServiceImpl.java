package io.aik.steins.grimoire.system.attachment.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.aik.steins.grimoire.core.utils.AssertUtils;
import io.aik.steins.grimoire.system.attachment.constant.AttachmentBizType;
import io.aik.steins.grimoire.system.attachment.dao.SysAttachmentMapper;
import io.aik.steins.grimoire.system.attachment.dto.AttachmentDto;
import io.aik.steins.grimoire.system.attachment.po.SysAttachmentPo;
import io.aik.steins.grimoire.system.attachment.service.AttachmentService;
import io.aik.steins.grimoire.system.attachment.vo.AttachmentVo;
import io.aik.steins.grimoire.system.file.dao.FileMapper;
import io.aik.steins.grimoire.system.file.po.FileRecordPo;
import io.aik.steins.grimoire.system.file.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 通用附件挂载服务实现 -anchor
 *
 * <p>设计权威：SDD §2.5.7（挂载写入与卸载契约）、§4.3（文件删除流程）、§4.5（挂载与卸载流程）。</p>
 *
 * <p><b>与文件模块的边界（本类只读 {@code aik_sys_file}）</b>：本服务需要两件文件层的事实——
 * ① 「锁文件行」的加锁读（写入路径与卸载路径的<b>第一条</b>语句，两条路径锁序必须对称）；
 * ② 按 fileId 批量取文件元数据（{@code fileSize} / {@code fileType}）。
 * 二者在 {@link FileService} 上均无对应方法，故这里经 {@link FileMapper} 以<b>只读</b>方式取得
 * （{@code selectByIdForUpdate} / {@code selectBatchIds}）。
 * 文件层的一切<b>写</b>操作（物理删行 + 删盘）仍只有 {@link FileService#remove(Long)} 一个所有者——
 * 本类不重复实现、也不直接写 {@code aik_sys_file}。若将来把这两个只读能力收进 {@code FileService}，
 * 本类应改为经它调用（登记为边界观察项，随 t6 的文件模块收口）。</p>
 *
 * @author a I k .
 */
@Slf4j
@Service("system.AttachmentService")
@RequiredArgsConstructor
public class AttachmentServiceImpl implements AttachmentService {

    /**
     * 附件名称长度上限（与 {@code aik_sys_attachment.attach_name VARCHAR(256)} 对齐）
     */
    private static final int ATTACH_NAME_MAX_LENGTH = 256;

    /**
     * 描述长度上限（与 {@code aik_sys_attachment.description VARCHAR(512)} 对齐）
     */
    private static final int DESCRIPTION_MAX_LENGTH = 512;

    private final SysAttachmentMapper sysAttachmentMapper;
    private final FileMapper fileMapper;
    private final FileService fileService;

    @Override
    public List<AttachmentVo> listByBiz(String bizType, Long bizId) {
        AttachmentBizType.validate(bizType);
        AssertUtils.notNull(bizId, "业务ID不能为空");

        //anchor 只取有效挂载行：@TableLogic 会自动把 del_flag = 0 追加进 WHERE，
        //       墓碑行不可能进入业务列表（api-contract §2.5.2 的硬约束）
        List<SysAttachmentPo> mounts = sysAttachmentMapper.selectList(
                new LambdaQueryWrapper<SysAttachmentPo>()
                        .eq(SysAttachmentPo::getBizType, bizType)
                        .eq(SysAttachmentPo::getBizId, bizId)
                        .orderByAsc(SysAttachmentPo::getSortOrder)
                        .orderByAsc(SysAttachmentPo::getId));
        if (mounts.isEmpty()) {
            return Collections.emptyList();
        }

        //anchor 文件层元数据【一次性批量取】（WHERE id IN (...)），禁止逐条 selectById 造成 N+1
        Map<Long, FileRecordPo> fileMap = loadFiles(
                mounts.stream().map(SysAttachmentPo::getFileId).collect(Collectors.toSet()));

        List<AttachmentVo> result = new ArrayList<>(mounts.size());
        for (SysAttachmentPo mount : mounts) {
            AttachmentVo vo = AttachmentVo.of(mount);
            FileRecordPo file = fileMap.get(mount.getFileId());
            if (file != null) {
                //anchor 文件层属性（内容属性，不泄露存储布局）：字节数与 MIME
                vo.setFileSize(file.getFileSize() == null ? null : String.valueOf(file.getFileSize()));
                vo.setFileType(file.getFileType());
            }
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(String bizType, Long bizId, List<AttachmentDto> attachments) {
        //anchor 服务层兜一次入参校验：knowledge 侧入口直接调本方法，不经 Controller 的 @Validated
        AttachmentBizType.validate(bizType);
        AssertUtils.notNull(bizId, "业务ID不能为空");
        AssertUtils.notNull(attachments, "附件列表不能为空");

        List<AttachmentDto> newItems = normalizeItems(attachments);

        //anchor ① 逐项加锁校验 fileId 存在（按 fileId 升序加锁，与卸载路径锁序对称，降低死锁面）
        lockAndAssertFilesExist(newItems);

        //anchor ② 一次读回该业务的全部挂载行（含墓碑）：oldSet 由内存过滤 del_flag = 0 得到，
        //       墓碑集合供"复活"分支使用
        List<SysAttachmentPo> rows = sysAttachmentMapper.selectAllByBiz(bizType, bizId);
        Map<Long, SysAttachmentPo> rowById = new HashMap<>(rows.size());
        Map<Long, SysAttachmentPo> rowByFileId = new HashMap<>(rows.size());
        for (SysAttachmentPo row : rows) {
            rowById.put(row.getId(), row);
            //anchor uk_biz_file(biz_type,biz_id,file_id) 保证同一业务下 fileId 不重复，故不会覆盖
            rowByFileId.put(row.getFileId(), row);
        }

        //anchor ③ newSet：按 id 优先、id 缺失（或陈旧）时按 fileId 定位既有行；缺失则新增
        Set<Long> newFileIds = new LinkedHashSet<>();
        for (AttachmentDto item : newItems) {
            newFileIds.add(item.getFileId());
            SysAttachmentPo row = locateRow(rowById, rowByFileId, item);
            if (row == null) {
                insertNew(bizType, bizId, item);
            } else if (!isEffective(row)) {
                //anchor upsert 的复活分支：命中墓碑行 → 置回 del_flag = 0 并刷新元数据
                reviveRow(row, item);
            } else {
                //anchor 交集：按需更新（幂等——原样重复提交不产生任何 UPDATE）
                updateRowIfChanged(row, item);
            }
        }

        //anchor ① oldSet − newSet → 卸载：保留挂载行、置 del_flag = 1
        //       （统一用 deleteById，由 @TableLogic 转成 UPDATE；不手写 set(del_flag, 1)）
        for (SysAttachmentPo row : rowByFileId.values()) {
            if (!isEffective(row) || newFileIds.contains(row.getFileId())) {
                continue;
            }
            sysAttachmentMapper.deleteById(row.getId());
            log.info("挂载卸载：attachId={}, bizType={}, bizId={}, fileId={}",
                    row.getId(), bizType, bizId, row.getFileId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long attachId) {
        AssertUtils.notNull(attachId, "挂载行ID不能为空");

        //anchor 定位挂载行：@TableLogic 自动过滤已卸载行，故"不存在"与"已卸载"同一条路径
        SysAttachmentPo mount = sysAttachmentMapper.selectById(attachId);
        AssertUtils.notNull(mount, "附件挂载不存在或已卸载");
        Long fileId = mount.getFileId();

        //anchor ① 第一条【锁】语句即锁定文件行——与挂载写入路径锁序对称
        //       （无锁的 selectById 能读到"即将被删除的文件行"，会插入悬空挂载，SDD §2.5.7）
        FileRecordPo file = fileMapper.selectByIdForUpdate(fileId);

        //anchor ② 标记卸载【本】挂载行（deleteById 由 @TableLogic 转成 UPDATE del_flag = 1）
        int affected = sysAttachmentMapper.deleteById(attachId);
        if (affected == 0) {
            //anchor 并发：本行已被另一事务卸载。另一事务已负责计数与文件层删除，此处幂等返回
            log.info("挂载行已被并发卸载，本事务按幂等 no-op 处理：attachId={}", attachId);
            return;
        }
        log.info("挂载卸载：attachId={}, fileId={}", attachId, fileId);

        if (file == null) {
            //anchor 文件行已不存在（并发卸载的最后一跳）：本行只是残留引用，卸载即修正，无需委派
            log.warn("挂载行指向的文件记录已不存在，仅卸载挂载行：attachId={}, fileId={}", attachId, fileId);
            return;
        }

        //anchor ③ 只数【有效】挂载（本行已在 ② 变为 del_flag = 1，故不计入）
        Long remaining = sysAttachmentMapper.selectCount(
                new LambdaQueryWrapper<SysAttachmentPo>()
                        .eq(SysAttachmentPo::getFileId, fileId));
        if (remaining != null && remaining > 0) {
            //anchor 仍有其它挂载点在共享 → 文件保留，正常返回（不得抛异常：这是"删其一"的正常路径）
            log.info("文件仍被其它挂载点引用，仅卸载挂载行：fileId={}, 剩余有效挂载={}", fileId, remaining);
            return;
        }

        //anchor ④ 无有效挂载 → 委派 FileService 完成唯一所有者职责：物理删文件记录 + 提交后删盘
        //       （本类不得重复实现删记录 / 删盘）
        fileService.remove(fileId);
    }

    /**
     * 入参规范化与校验 -anchor
     *
     * @param attachments 原始入参列表（非 null）
     * @return 校验通过的列表（空列表原样返回，语义 = 卸载该业务全部挂载）
     */
    private List<AttachmentDto> normalizeItems(List<AttachmentDto> attachments) {
        if (attachments.isEmpty()) {
            return Collections.emptyList();
        }

        List<AttachmentDto> items = new ArrayList<>(attachments.size());
        Set<Long> seenFileIds = new HashSet<>(attachments.size());
        for (AttachmentDto item : attachments) {
            AssertUtils.notNull(item, "附件项不能为空");
            AssertUtils.notNull(item.getFileId(), "文件ID不能为空");
            AssertUtils.notEmpty(item.getAttachName(), "附件名称不能为空");
            AssertUtils.isTrue(item.getAttachName().length() <= ATTACH_NAME_MAX_LENGTH,
                    "附件名称不能超过" + ATTACH_NAME_MAX_LENGTH + "字符");
            AssertUtils.isTrue(item.getDescription() == null
                            || item.getDescription().length() <= DESCRIPTION_MAX_LENGTH,
                    "描述不能超过" + DESCRIPTION_MAX_LENGTH + "字符");
            //anchor 一个业务下同一文件只能有一行（uk_biz_file）：入参内重复会让 insert 直接撞唯一键
            AssertUtils.isTrue(seenFileIds.add(item.getFileId()),
                    "同一文件不可重复挂载：fileId=" + item.getFileId());
            items.add(item);
        }
        return items;
    }

    /**
     * 逐项加锁校验 {@code fileId} 存在 -anchor
     *
     * <p>加锁读是<b>必须</b>的：无锁读能读到"即将被并发删除的文件行"，会插入指向不存在文件的挂载行。
     * 按 {@code fileId} 升序加锁以降低多事务交叉锁序的死锁面（SDD §2.5.7、§6.4 R4）。</p>
     */
    private void lockAndAssertFilesExist(List<AttachmentDto> items) {
        if (items.isEmpty()) {
            return;
        }
        List<Long> fileIds = items.stream()
                .map(AttachmentDto::getFileId)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        for (Long fileId : fileIds) {
            FileRecordPo file = fileMapper.selectByIdForUpdate(fileId);
            AssertUtils.notNull(file, "挂载的文件不存在：fileId=" + fileId);
        }
    }

    /**
     * 批量取文件记录 -anchor
     */
    private Map<Long, FileRecordPo> loadFiles(Set<Long> fileIds) {
        if (fileIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<FileRecordPo> files = fileMapper.selectBatchIds(fileIds);
        Map<Long, FileRecordPo> fileMap = new HashMap<>(files.size());
        for (FileRecordPo file : files) {
            fileMap.put(file.getId(), file);
        }
        return fileMap;
    }

    /**
     * 定位入参对应的既有挂载行 -anchor
     *
     * <p>按 {@code id} 优先（id 缺失或该业务下不存在该 id 时降级按 {@code fileId} 匹配）。
     * 若 id 命中但该行的 {@code file_id} 与入参 {@code fileId} 不一致，属自相矛盾的入参——
     * 放行会让 {@code uk_biz_file} 的语义被破坏，故直接拒绝。</p>
     *
     * @return 既有挂载行（可能是墓碑行）；无匹配返回 {@code null}
     */
    private SysAttachmentPo locateRow(Map<Long, SysAttachmentPo> rowById,
                                      Map<Long, SysAttachmentPo> rowByFileId,
                                      AttachmentDto item) {
        if (item.getId() != null) {
            SysAttachmentPo row = rowById.get(item.getId());
            if (row != null) {
                AssertUtils.isTrue(Objects.equals(row.getFileId(), item.getFileId()),
                        "挂载行与文件不匹配：attachId=" + item.getId());
                return row;
            }
            log.info("入参 attachId 在该业务下不存在，降级按 fileId 定位：attachId={}, fileId={}",
                    item.getId(), item.getFileId());
        }
        return rowByFileId.get(item.getFileId());
    }

    /**
     * 新增挂载行 -anchor
     *
     * <p>并发撞唯一键（另一事务刚插入同一三元组）时降级为"复活并重试一次"（SDD §2.5.7 第 4 条）。</p>
     */
    private void insertNew(String bizType, Long bizId, AttachmentDto item) {
        SysAttachmentPo po = new SysAttachmentPo();
        po.setId(IdUtil.getSnowflakeNextId());
        po.setFileId(item.getFileId());
        po.setBizType(bizType);
        po.setBizId(bizId);
        po.setAttachName(item.getAttachName());
        po.setDescription(item.getDescription());
        po.setSortOrder(resolveSortOrder(item));
        po.setDelFlag(SysAttachmentPo.DEL_FLAG_EFFECTIVE);
        try {
            sysAttachmentMapper.insert(po);
            log.info("挂载新增：attachId={}, bizType={}, bizId={}, fileId={}",
                    po.getId(), bizType, bizId, item.getFileId());
        } catch (DuplicateKeyException e) {
            log.warn("挂载新增撞唯一键 uk_biz_file，降级为复活并重试一次：bizType={}, bizId={}, fileId={}",
                    bizType, bizId, item.getFileId());
            SysAttachmentPo concurrent = sysAttachmentMapper.selectByBizAndFileForUpdate(
                    bizType, bizId, item.getFileId());
            AssertUtils.notNull(concurrent, "挂载保存失败，请重试");
            reviveRow(concurrent, item);
        }
    }

    /**
     * 复活墓碑行并刷新元数据 -anchor
     *
     * <p>两步同事务：先用手写 SQL 把 {@code del_flag} 置回 0（MyBatis-Plus 的 update 选不中墓碑行），
     * 再用 {@code updateById} 刷新 {@code attachName} / {@code description} / {@code sortOrder}，
     * 并由 {@code BaseMetaObjectHandler} 填充 {@code modify_time} / {@code modify_by}。</p>
     */
    private void reviveRow(SysAttachmentPo row, AttachmentDto item) {
        sysAttachmentMapper.reviveById(row.getId());
        applyFields(row, item);
        sysAttachmentMapper.updateById(row);
        log.info("挂载复活：attachId={}, fileId={}", row.getId(), item.getFileId());
    }

    /**
     * 交集行：仅在字段确有变化时更新（保证"原样重复提交"零 UPDATE） -anchor
     */
    private void updateRowIfChanged(SysAttachmentPo row, AttachmentDto item) {
        Integer sortOrder = resolveSortOrder(item);
        boolean changed = !Objects.equals(row.getAttachName(), item.getAttachName())
                || !Objects.equals(row.getDescription(), item.getDescription())
                || !Objects.equals(row.getSortOrder(), sortOrder);
        if (!changed) {
            return;
        }
        applyFields(row, item);
        sysAttachmentMapper.updateById(row);
        log.info("挂载元数据更新：attachId={}, fileId={}", row.getId(), item.getFileId());
    }

    /**
     * 把入参字段写入挂载行实体 -anchor
     */
    private void applyFields(SysAttachmentPo row, AttachmentDto item) {
        row.setAttachName(item.getAttachName());
        row.setDescription(item.getDescription());
        row.setSortOrder(resolveSortOrder(item));
        row.setDelFlag(SysAttachmentPo.DEL_FLAG_EFFECTIVE);
    }

    /**
     * 排序号缺省值 -anchor
     */
    private Integer resolveSortOrder(AttachmentDto item) {
        return item.getSortOrder() == null ? 0 : item.getSortOrder();
    }

    /**
     * 是否有效挂载（{@code del_flag = 0}） -anchor
     */
    private boolean isEffective(SysAttachmentPo row) {
        return !SysAttachmentPo.DEL_FLAG_UNLOADED.equals(row.getDelFlag());
    }
}
