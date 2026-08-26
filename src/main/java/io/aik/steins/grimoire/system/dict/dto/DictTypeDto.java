package io.aik.steins.grimoire.system.dict.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 字典类型 -anchor
 *
 * @author a I k .
 */
@Data
@Schema(description = "字典类型")
public class DictTypeDto {

    @Schema(description = "ID，新增时为空")
    private Long id;

    @NotBlank(message = "字典编码不能为空")
    @Size(max = 64, message = "字典编码不能超过64字符")
    @Schema(description = "字典编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String dictCode;

    @NotBlank(message = "字典名称不能为空")
    @Size(max = 128, message = "字典名称不能超过128字符")
    @Schema(description = "字典名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String dictName;

    @Size(max = 512, message = "描述不能超过512字符")
    @Schema(description = "描述")
    private String description;

    @Schema(description = "状态：1-启用 0-禁用", example = "1")
    private Integer status;
}
