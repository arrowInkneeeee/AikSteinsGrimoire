package io.aik.steins.grimoire.system.attachment.service;

import io.aik.steins.grimoire.system.attachment.dto.AttachmentDto;
import io.aik.steins.grimoire.system.attachment.vo.AttachmentVo;

import java.util.List;

/**
 * 通用附件挂载服务 -anchor
 *
 * <p>附件域的<b>唯一</b>挂载读写入口（SDD §2.5.7 / §4.5）：业务模块（如 knowledge）
 * <b>不得</b>直连 {@code SysAttachmentMapper}，也不得在响应体里直接暴露 {@code SysAttachmentPo}。</p>
 *
 * <p>文件层（{@code aik_sys_file}）的物理删除与删盘<b>不</b>由本服务实现——它是
 * {@code FileService#remove(Long)} 的职责（唯一所有者）。本服务只在「数到 0 个有效挂载」时委派它。</p>
 *
 * @author a I k .
 */
public interface AttachmentService {

    /**
     * 按业务查询有效挂载列表
     *
     * <p>只返回 {@code del_flag = 0} 的行（硬约束：{@link AttachmentVo} 不含 {@code del_flag}，
     * 前端无从自行过滤，返回墓碑行等于"用户删掉的附件重新出现在列表里"）；
     * 排序 {@code ORDER BY sort_order ASC, id ASC}；{@code fileSize} / {@code fileType} 由
     * {@code aik_sys_file} <b>按 fileId 批量取</b>（禁止 N+1）。</p>
     *
     * @param bizType 业务类型（白名单，未识别值抛 BusinessException）
     * @param bizId   业务主键
     * @return 有效挂载视图列表；无挂载返回空列表
     */
    List<AttachmentVo> listByBiz(String bizType, Long bizId);

    /**
     * 保存某业务的挂载列表（<b>差量</b>语义）
     *
     * <p>仅卸载「原有但新列表已无」的行（保留挂载行、置 {@code del_flag = 1}），
     * 仅新增 / 复活「新列表有」的行；<b>禁止全删再全插</b>——{@code uk_biz_file} 不含
     * {@code del_flag}，全删产生的卸载行仍占键值，会让紧接着的全插撞唯一键。</p>
     *
     * <p>写入前逐项以<b>加锁读</b>校验 {@code fileId} 存在（按 fileId 升序加锁），
     * 悬空挂载禁止产生。</p>
     *
     * @param bizType     业务类型（白名单）
     * @param bizId       业务主键
     * @param attachments 附件列表；{@code null} 视为参数错误，<b>空列表</b>表示卸载该业务全部挂载
     */
    void save(String bizType, Long bizId, List<AttachmentDto> attachments);

    /**
     * 卸载单个挂载点（附件列表的"删除"入口）
     *
     * <p>同一事务内：<b>先</b>锁文件行 → 标记卸载本挂载行 → 重新计数有效挂载 →
     * 仅在「无任何有效挂载」时委派 {@code FileService#remove(fileId)} 完成物理删除文件记录 + 删盘。</p>
     *
     * <p>仍有其它有效挂载时<b>正常返回、不抛异常</b>（共享文件必须保留）。</p>
     *
     * @param attachId 挂载行ID
     */
    void remove(Long attachId);
}
