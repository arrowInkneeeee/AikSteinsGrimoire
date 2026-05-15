package io.aik.steins.grimoire.system.common.dto;

import io.aik.steins.grimoire.core.dto.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典类型查询参数 -anchor
 *
 * @author a I k .
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "字典类型查询参数")
public class DictTypeQuery extends PageQuery {

    @Schema(description = "字典编码（模糊）")
    private String dictCode;

    @Schema(description = "字典名称（模糊）")
    private String dictName;

    @Schema(description = "状态：1-启用 0-禁用")
    private Integer status;
}
