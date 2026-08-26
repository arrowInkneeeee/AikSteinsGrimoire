package io.aik.steins.grimoire.system.dict.po;

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
 * -anchor 字典类型
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
@Schema(description = "字典类型")
@TableName("aik_sys_dict_type")
public class DictTypePo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    public DictTypePo() {
        super();
    }

    /**
     * 主键ID
     */
    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * 字典编码
     */
    @Schema(description = "字典编码")
    @TableField("dict_code")
    private String dictCode;

    /**
     * 字典名称
     */
    @Schema(description = "字典名称")
    @TableField("dict_name")
    private String dictName;

    /**
     * 描述
     */
    @Schema(description = "描述")
    @TableField("description")
    private String description;

    /**
     * 状态：1-启用 0-禁用
     */
    @Schema(description = "状态：1-启用 0-禁用")
    @TableField("status")
    private Integer status;
}
