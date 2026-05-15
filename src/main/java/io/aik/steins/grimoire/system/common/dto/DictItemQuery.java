package io.aik.steins.grimoire.system.common.dto;

import io.aik.steins.grimoire.core.dto.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典项查询参数 -anchor
 *
 * @author a I k .
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "字典项查询参数")
public class DictItemQuery extends PageQuery {

    @Schema(description = "字典类型编码（精确）")
    private String dictCode;

    @Schema(description = "字典项编码（模糊）")
    private String itemCode;

    @Schema(description = "字典项名称（模糊）")
    private String itemName;

    @Schema(description = "状态：1-启用 0-禁用")
    private Integer status;
}
