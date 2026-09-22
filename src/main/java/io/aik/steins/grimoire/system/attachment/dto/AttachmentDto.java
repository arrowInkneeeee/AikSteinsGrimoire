package io.aik.steins.grimoire.system.attachment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * -anchor 附件挂载项（差量入参的元素）
 *
 * <p>形状权威：api-contract.md §2.5.3。同一份入参结构同时服务
 * {@code POST /grimoire/attachment/save} 与 {@code KnowledgeDto.attachments}。</p>
 *
 * <p><b>{@code attachName} 必填且不得回填</b>：它是用户可见文件名的权威归属（挂载层），
 * 后端<b>不得</b>用 {@code aik_sys_file.original_name} 兜底——那是首次上传者的名字，
 * md5 秒传命中后会把别人的文件名给第二个挂载者（SDD §2.5.2）。</p>
 *
 * @author a I k .
 */
@Data
@Schema(description = "附件挂载项")
public class AttachmentDto {

    /**
     * 挂载行ID：更新既有挂载时带上；新增挂载时为空
     */
    @Schema(description = "挂载行ID，新增挂载时为空")
    private Long id;

    /**
     * 文件对象ID（挂载锚点），必填且必须存在
     */
    @NotNull(message = "文件ID不能为空")
    @Schema(description = "文件对象ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long fileId;

    /**
     * 用户可见文件名（权威），必填
     */
    @NotBlank(message = "附件名称不能为空")
    @Size(max = 256, message = "附件名称不能超过256字符")
    @Schema(description = "用户可见文件名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String attachName;

    /**
     * 描述
     */
    @Size(max = 512, message = "描述不能超过512字符")
    @Schema(description = "描述")
    private String description;

    /**
     * 排序号，缺省 0
     */
    @Schema(description = "排序号，缺省 0", example = "0")
    private Integer sortOrder;
}
