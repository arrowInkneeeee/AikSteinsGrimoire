package io.aik.steins.grimoire.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 通用 ID 传输对象 -anchor
 *
 * @author a I k .
 */
@Data
@Schema(description = "ID 传输对象")
public class IdDto {

    @NotNull(message = "ID 不能为空")
    @Schema(description = "主键 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;
}
