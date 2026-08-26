package io.aik.steins.grimoire.system.param.dto;

import io.aik.steins.grimoire.core.dto.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统参数查询参数 -anchor
 *
 * @author a I k .
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "系统参数查询参数")
public class SystemParamQuery extends PageQuery {

    @Schema(description = "参数键（模糊）")
    private String paramKey;

    @Schema(description = "参数分组")
    private String paramGroup;
}
