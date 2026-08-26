package io.aik.steins.grimoire.system.param.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 系统参数 -anchor
 *
 * @author a I k .
 */
@Data
@Schema(description = "系统参数")
public class SystemParamDto {

    @Schema(description = "ID，新增时为空")
    private Long id;

    @NotBlank(message = "参数键不能为空")
    @Schema(description = "参数键", requiredMode = Schema.RequiredMode.REQUIRED)
    private String paramKey;

    @NotBlank(message = "参数值不能为空")
    @Schema(description = "参数值", requiredMode = Schema.RequiredMode.REQUIRED)
    private String paramValue;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "参数分组")
    private String paramGroup;

    @Schema(description = "是否可编辑：1-是 0-否", example = "1")
    private Integer editable;
}
