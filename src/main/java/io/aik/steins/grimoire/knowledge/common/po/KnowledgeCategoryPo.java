package io.aik.steins.grimoire.knowledge.common.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.aik.steins.grimoire.core.po.BaseEntity;
import io.aik.steins.grimoire.knowledge.common.constant.KnowledgeConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * -anchor 知识分类
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since 2026/05/18
 * -
 */
@Data
@SuperBuilder
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description = "知识分类")
@TableName(KnowledgeConstant.TABLE_PREFIX + "category")
public class KnowledgeCategoryPo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    public KnowledgeCategoryPo() {
        super();
    }

    /**
     * 主键ID
     */
    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * 父分类ID，0表示根节点
     */
    @Schema(description = "父分类ID")
    @TableField("parent_id")
    private Long parentId;

    /**
     * 分类名称
     */
    @Schema(description = "分类名称")
    @TableField("category_name")
    private String categoryName;

    /**
     * 分类编码
     */
    @Schema(description = "分类编码")
    @TableField("category_code")
    private String categoryCode;

    /**
     * 排序号
     */
    @Schema(description = "排序号")
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 状态：1-启用 0-禁用
     */
    @Schema(description = "状态：1-启用 0-禁用")
    @TableField("status")
    private Integer status;
}
