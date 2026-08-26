package io.aik.steins.grimoire.system.dict.vo;

import cn.hutool.core.bean.BeanUtil;
import io.aik.steins.grimoire.system.dict.po.DictItemPo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 字典项视图 -anchor
 *
 * @author a I k .
 */
@Data
@Schema(description = "字典项视图")
public class DictItemVo {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "字典类型编码")
    private String dictCode;

    @Schema(description = "字典项编码")
    private String itemCode;

    @Schema(description = "字典项名称")
    private String itemName;

    @Schema(description = "排序号")
    private Integer sortOrder;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "修改时间")
    private LocalDateTime modifyTime;

    public static DictItemVo of(DictItemPo po) {
        if (po == null) {
            return null;
        }
        DictItemVo vo = new DictItemVo();
        BeanUtil.copyProperties(po, vo);
        return vo;
    }
}
