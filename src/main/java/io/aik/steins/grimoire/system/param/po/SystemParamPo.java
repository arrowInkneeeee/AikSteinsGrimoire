package io.aik.steins.grimoire.system.param.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.aik.steins.grimoire.core.po.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * -anchor 系统参数
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since 2026/05/15
 * -
 */
@Data
@SuperBuilder
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description = "系统参数")
@TableName("aik_sys_param")
public class SystemParamPo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    public SystemParamPo() {
        super();
    }

    /**
     * 主键ID
     */
    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * 参数键
     */
    @Schema(description = "参数键")
    @TableField("param_key")
    private String paramKey;

    /**
     * 参数值
     */
    @Schema(description = "参数值")
    @TableField("param_value")
    private String paramValue;

    /**
     * 描述
     */
    @Schema(description = "描述")
    @TableField("description")
    private String description;

    /**
     * 参数分组
     */
    @Schema(description = "参数分组")
    @TableField("param_group")
    private String paramGroup;

    /**
     * 是否可编辑：1-是 0-否
     */
    @Schema(description = "是否可编辑：1-是 0-否")
    @TableField("editable")
    private Integer editable;
}
