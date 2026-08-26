package io.aik.steins.grimoire.system.dict.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 字典类型及字典项聚合 -anchor
 *
 * @author a I k .
 */
@Data
@Schema(description = "字典类型及字典项聚合")
public class DictTypeItemsVo {

    @Schema(description = "字典类型编码")
    private String dictCode;

    @Schema(description = "字典类型名称")
    private String dictName;

    @Schema(description = "字典项列表")
    private List<DictItemVo> items;
}
