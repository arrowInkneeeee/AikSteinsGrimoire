package io.aik.steins.grimoire.system.dict.vo;

import cn.hutool.core.bean.BeanUtil;
import io.aik.steins.grimoire.system.dict.po.DictTypePo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 字典类型视图 -anchor
 *
 * @author a I k .
 */
@Data
@Schema(description = "字典类型视图")
public class DictTypeVo {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "字典编码")
    private String dictCode;

    @Schema(description = "字典名称")
    private String dictName;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "状态：1-启用 0-禁用")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "修改时间")
    private LocalDateTime modifyTime;

    public static DictTypeVo of(DictTypePo po) {
        if (po == null) {
            return null;
        }
        DictTypeVo vo = new DictTypeVo();
        BeanUtil.copyProperties(po, vo);
        return vo;
    }
}
