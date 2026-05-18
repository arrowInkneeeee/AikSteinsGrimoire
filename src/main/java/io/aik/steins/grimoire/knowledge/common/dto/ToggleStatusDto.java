package io.aik.steins.grimoire.knowledge.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 切换状态请求 -anchor
 *
 * @author a I k .
 */
@Data
@Schema(description = "切换状态请求")
public class ToggleStatusDto {

    @NotNull(message = "ID 不能为空")
    @Schema(description = "主键 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @NotNull(message = "状态不能为空")
    @Schema(description = "目标状态", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;
}
