package io.aik.steins.grimoire.system.attachment.vo;

import cn.hutool.core.bean.BeanUtil;
import io.aik.steins.grimoire.system.attachment.po.SysAttachmentPo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * -anchor 附件挂载视图
 *
 * <p>形状权威：api-contract.md §2.5.3，<b>恰为 8 字段</b>，且是附件响应的唯一形态——
 * 任何 VO 都<b>不得</b>直接暴露 {@link SysAttachmentPo} 实体（SDD §4.5）。</p>
 *
 * <p><b>明确不含</b>：{@code url} / {@code attachUrl} / {@code knowledgeId} / {@code filePath} /
 * {@code storedName} / {@code md5} / {@code originalName}（首次上传者的名字，md5 秒传后会串名）
 * / {@code bizType} / {@code bizId}（三端点都按业务发起，无消费方）。</p>
 *
 * <p>字段名用 {@code id}（= 挂载行主键，即 SDD 正文所称的 {@code attachId}，下载与卸载都用它），
 * <b>不得</b>同时定义 {@code id} 与 {@code attachId}。</p>
 *
 * <p>{@code fileSize} / {@code fileType} 是【文件层】属性，由 {@code fileId} 关联
 * {@code aik_sys_file} 带出，本 VO 的工厂方法只负责挂载层字段，二者由挂载服务在
 * <b>一次批量查询</b>后回填（禁止 N+1）。</p>
 *
 * @author a I k .
 */
@Data
@Schema(description = "附件挂载视图")
public class AttachmentVo {

    /**
     * 挂载行主键（下载 / 卸载都用它）
     */
    @Schema(description = "挂载行ID")
    private Long id;

    /**
     * 文件对象ID：下载 / 预览的唯一定位锚点
     */
    @Schema(description = "文件对象ID")
    private Long fileId;

    /**
     * 用户可见文件名（权威归属挂载层）
     */
    @Schema(description = "用户可见文件名")
    private String attachName;

    /**
     * 字节数（Long→String 序列化约定，§2.1.3）——文件层属性
     */
    @Schema(description = "文件大小（字节，字符串形式）")
    private String fileSize;

    /**
     * MIME 类型——文件层属性（前端据此选类型图标 / 判断能否内联预览）
     */
    @Schema(description = "文件MIME类型")
    private String fileType;

    /**
     * 描述
     */
    @Schema(description = "描述")
    private String description;

    /**
     * 排序号
     */
    @Schema(description = "排序号")
    private Integer sortOrder;

    /**
     * 创建时间（绑定关系的建立时间）
     */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /**
     * 挂载层字段映射（不含 fileSize / fileType，二者由调用方批量取回后回填）
     *
     * @param po 挂载行
     * @return 视图对象；入参为 {@code null} 时返回 {@code null}
     */
    public static AttachmentVo of(SysAttachmentPo po) {
        if (po == null) {
            return null;
        }
        AttachmentVo vo = new AttachmentVo();
        BeanUtil.copyProperties(po, vo);
        return vo;
    }
}
