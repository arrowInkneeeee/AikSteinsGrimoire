package io.aik.steins.grimoire.system.dict.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 字典项 -anchor
 *
 * @author a I k .
 */
@Data
@Schema(description = "字典项")
public class DictItemDto {

    @Schema(description = "ID，新增时为空")
    private Long id;

    @NotBlank(message = "字典类型编码不能为空")
    @Schema(description = "字典类型编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String dictCode;

    @NotBlank(message = "字典项编码不能为空")
    @Schema(description = "字典项编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String itemCode;

    @NotBlank(message = "字典项名称不能为空")
    @Schema(description = "字典项名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String itemName;

    @Schema(description = "排序号", example = "0")
    private Integer sortOrder;

    @Schema(description = "状态：1-启用 0-禁用", example = "1")
    private Integer status;

    @Schema(description = "备注")
    private String remark;
}
