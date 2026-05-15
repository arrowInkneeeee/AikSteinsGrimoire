package io.aik.steins.grimoire.system.common.vo;

import cn.hutool.core.bean.BeanUtil;
import io.aik.steins.grimoire.system.common.po.SystemParamPo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统参数视图 -anchor
 *
 * @author a I k .
 */
@Data
@Schema(description = "系统参数视图")
public class SystemParamVo {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "参数键")
    private String paramKey;

    @Schema(description = "参数值")
    private String paramValue;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "参数分组")
    private String paramGroup;

    @Schema(description = "是否可编辑")
    private Integer editable;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    public static SystemParamVo of(SystemParamPo po) {
        if (po == null) {
            return null;
        }
        SystemParamVo vo = new SystemParamVo();
        BeanUtil.copyProperties(po, vo);
        return vo;
    }
}
