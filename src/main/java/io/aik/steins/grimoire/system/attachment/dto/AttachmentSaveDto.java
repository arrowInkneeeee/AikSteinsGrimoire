package io.aik.steins.grimoire.system.attachment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * -anchor 附件挂载保存入参（{@code POST /grimoire/attachment/save}）
 *
 * <p>形状权威：api-contract.md §2.5.2。语义是<b>差量</b>（不是整组替换）：
 * 仅卸载「原有但新列表已无」的行，仅新增 / 复活「新列表有」的行；<b>空列表 = 卸载该业务全部挂载</b>。</p>
 *
 * <p>{@code bizType} 由调用方给出并受白名单校验；knowledge 侧入口（{@code KnowledgeDto.attachments}）
 * 固定传 {@code AttachmentBizType.KNOWLEDGE}，<b>不由前端传</b>。</p>
 *
 * @author a I k .
 */
@Data
@Schema(description = "附件挂载保存入参")
public class AttachmentSaveDto {

    /**
     * 业务类型（白名单取值，当前仅 knowledge）
     */
    @NotBlank(message = "业务类型不能为空")
    @Size(max = 32, message = "业务类型不能超过32字符")
    @Schema(description = "业务类型：knowledge", requiredMode = Schema.RequiredMode.REQUIRED)
    private String bizType;

    /**
     * 业务主键
     */
    @NotNull(message = "业务ID不能为空")
    @Schema(description = "业务主键", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long bizId;

    /**
     * 附件列表（元素级校验经 {@code @Valid} 级联）
     */
    @NotNull(message = "附件列表不能为空")
    @Valid
    @Schema(description = "附件列表；空列表 = 卸载该业务全部挂载", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<AttachmentDto> attachments;
}
