package io.aik.steins.grimoire.system.file.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.aik.steins.grimoire.core.config.FileStorageConfig;
import io.aik.steins.grimoire.core.exception.BusinessException;
import io.aik.steins.grimoire.core.storage.FileStorageStrategy;
import io.aik.steins.grimoire.core.utils.AssertUtils;
import io.aik.steins.grimoire.system.file.dao.FileMapper;
import io.aik.steins.grimoire.system.file.dto.FileQuery;
import io.aik.steins.grimoire.system.file.po.FileRecordPo;
import io.aik.steins.grimoire.system.file.service.FileService;
import io.aik.steins.grimoire.system.file.vo.FileVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * 文件 Service 实现 -anchor
 *
 * @author a I k .
 */
@Slf4j
@Service("system.FileService")
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final FileMapper fileMapper;
    private final FileStorageConfig fileStorageConfig;
    private final FileStorageStrategy fileStorageStrategy;

    @Override
    public FileVo upload(MultipartFile file) {
        AssertUtils.notNull(file, "文件不能为空");
        AssertUtils.isTrue(!file.isEmpty(), "文件不能为空");
        AssertUtils.isTrue(file.getSize() <= fileStorageConfig.getMaxSize(),
                "文件大小不能超过" + (fileStorageConfig.getMaxSize() / 1024 / 1024) + "MB");

        //anchor 校验文件类型白名单
        if (Boolean.TRUE.equals(fileStorageConfig.getTypeCheckEnabled())
                && StrUtil.isNotBlank(fileStorageConfig.getAllowTypes())) {
            String contentType = StrUtil.nullToEmpty(file.getContentType()).toLowerCase();
            String ext = StrUtil.nullToEmpty(FileUtil.extName(file.getOriginalFilename())).toLowerCase();
            boolean allowed = fileStorageConfig.getAllowTypeSet().contains(contentType)
                    || fileStorageConfig.getAllowTypeSet().contains(ext);
            AssertUtils.isTrue(allowed, "不支持的文件类型");
        }

        String originalName = file.getOriginalFilename();
        AssertUtils.notEmpty(originalName, "文件名不能为空");

        try {
            //anchor 一次性读入字节：算 md5 与写盘共用同一份内容，不再两次调用 getInputStream
            byte[] content = file.getBytes();

            //anchor 计算MD5（内容寻址锚点）
            String md5 = DigestUtil.md5Hex(content);

            //anchor 秒传查重：命中即复用既有文件记录并【跳过写盘】
            //       秒传的收益是少写字节，不是少写一行库——磁盘零写入，孤儿文件无从产生。
            //       注意：真库 idx_md5 是非唯一索引，无唯一键可撞，故此处【没有】也没有必要写
            //       DuplicateKeyException 补偿；并发窗口下可能落两行同 md5，由 SDD §2.3.2 查询① 对账检出。
            FileRecordPo existing = fileMapper.selectLatestByMd5(md5);
            if (existing != null) {
                log.info("md5 命中，复用既有文件记录（跳过写盘）：fileId={}, md5={}", existing.getId(), md5);
                return FileVo.of(existing);
            }

            //anchor 未命中：使用策略上传（写入磁盘 / 对象存储）
            String storedPath = fileStorageStrategy.upload(new ByteArrayInputStream(content), originalName);

            //anchor 保存记录
            FileRecordPo po = new FileRecordPo();
            po.setId(IdUtil.getSnowflakeNextId());
            po.setOriginalName(originalName);
            po.setStoredName(FileUtil.getName(storedPath));
            po.setFilePath(storedPath);
            po.setFileSize(file.getSize());
            po.setFileType(file.getContentType());
            po.setStorageType(fileStorageConfig.getUse());
            po.setMd5(md5);
            po.setDownloadCount(0);
            fileMapper.insert(po);

            return FileVo.of(po);
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException("文件上传失败");
        } catch (Exception e) {
            log.error("文件存储失败", e);
            throw new BusinessException("文件存储失败：" + e.getMessage());
        }
    }

    @Override
    public void download(Long id, Long attachId, HttpServletResponse response, boolean preview) {
        FileRecordPo po = fileMapper.selectById(id);
        AssertUtils.notNull(po, "文件不存在");

        //anchor 响应文件名：挂载模式取挂载层 attachName（权威归属挂载层），台账模式回落 originalName
        String downloadName = resolveDownloadName(id, attachId, po);

        try {
            //anchor 使用策略下载：文件名由服务端决定，绝不使用前端传入的字符串（响应头注入面）
            fileStorageStrategy.download(response, po.getFilePath(), downloadName, preview);

            //anchor 下载次数原子自增，消除"读-改-写"的丢更新
            fileMapper.update(null, new LambdaUpdateWrapper<FileRecordPo>()
                    .eq(FileRecordPo::getId, id)
                    .setSql("download_count = download_count + 1"));
        } catch (Exception e) {
            log.error("文件下载失败", e);
            throw new BusinessException("文件下载失败");
        }
    }

    @Override
    public IPage<FileVo> findPage(FileQuery query) {
        LambdaQueryWrapper<FileRecordPo> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(query.getOriginalName()), FileRecordPo::getOriginalName, query.getOriginalName())
                .orderByDesc(FileRecordPo::getCreateTime);

        return fileMapper.selectPage(query.toPage(), wrapper).convert(FileVo::of);
    }

    @Override
    public FileVo findById(Long id) {
        FileRecordPo po = fileMapper.selectById(id);
        AssertUtils.notNull(po, "文件不存在");
        return FileVo.of(po);
    }

    @Override
    public void rename(Long id, String originalName) {
        AssertUtils.notNull(id, "文件ID不能为空");
        AssertUtils.notEmpty(originalName, "文件名不能为空");
        FileRecordPo po = fileMapper.selectById(id);
        AssertUtils.notNull(po, "文件不存在");

        //anchor 只修改显示名称，磁盘存储名和路径不变
        po.setOriginalName(originalName);
        fileMapper.updateById(po);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        //anchor ① 第一条语句即锁定文件行——与挂载写入路径锁序一致，
        //       串行化「卸载最后一个引用」与「新建挂载」，堵住悬空挂载窗口
        FileRecordPo po = fileMapper.selectByIdForUpdate(id);
        if (po == null) {
            //anchor 幂等 no-op：不抛异常、不发 DELETE、不删盘（SDD §2.5.7 / §4.3 台账分支）
            log.info("文件不存在，删除按幂等 no-op 处理：fileId={}", id);
            return;
        }

        //anchor ② 仍被有效挂载引用则拒绝——这是管理端台账入口的守卫，不是卸载流程
        //       （「标记卸载挂载行 → 计数 → 为 0 才删文件」由统一挂载服务负责，本方法不含该步骤）
        long effectiveMounts = fileMapper.countEffectiveMounts(id);
        AssertUtils.isTrue(effectiveMounts == 0, "文件仍被其它挂载点引用，不允许删除");

        //anchor ③ 物理删除文件记录（本表无 del_flag，物理删除是唯一删除语义）
        fileMapper.deleteById(id);

        //anchor ④ 删盘必须在事务提交之后：先删盘再回滚会造成不可恢复的悬空引用
        removeDiskAfterCommit(po.getFilePath());
    }

    /**
     * 解析下载响应文件名 -anchor
     *
     * <p>带 attachId（挂载模式）：一次查询同时校验「挂载行存在 + {@code del_flag = 0} +
     * {@code file_id} 与本文件一致」，任一不满足即拒绝；文件名取挂载行 {@code attach_name}。</p>
     * <p>不带 attachId（台账模式）：不校验挂载，文件名取 {@code original_name}。</p>
     */
    private String resolveDownloadName(Long fileId, Long attachId, FileRecordPo po) {
        if (attachId == null) {
            return po.getOriginalName();
        }
        String attachName = fileMapper.selectAttachNameForDownload(attachId, fileId);
        AssertUtils.isTrue(StrUtil.isNotBlank(attachName), "附件挂载不存在或与文件不匹配");
        return attachName;
    }

    /**
     * 事务提交后再删盘 -anchor
     *
     * <p>无事务上下文时（例如直接调用单测）立即删盘：此时不存在"回滚"，与"提交后删盘"语义等价。</p>
     */
    private void removeDiskAfterCommit(String filePath) {
        if (StrUtil.isBlank(filePath)) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    removeDiskQuietly(filePath);
                }
            });
        } else {
            removeDiskQuietly(filePath);
        }
    }

    /**
     * 删除磁盘文件 -anchor
     *
     * <p>失败只记日志、不抛异常：提交后删盘失败只残留孤儿磁盘文件（可恢复），
     * 与"先删盘再回滚"造成的悬空引用（不可恢复）代价不对称，故顺序不可反。</p>
     */
    private void removeDiskQuietly(String filePath) {
        try {
            fileStorageStrategy.remove(filePath);
        } catch (Exception e) {
            log.error("磁盘文件删除失败（仅残留孤儿文件，可由 SDD §2.3.2 查询③ 对账清理）：filePath={}", filePath, e);
        }
    }
}
