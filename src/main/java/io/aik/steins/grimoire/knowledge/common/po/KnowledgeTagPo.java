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
 * -anchor 知识标签
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
@Schema(description = "知识标签")
@TableName(KnowledgeConstant.TABLE_PREFIX + "tag")
public class KnowledgeTagPo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    public KnowledgeTagPo() {
        super();
    }

    /**
     * 主键ID
     */
    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * 标签名称
     */
    @Schema(description = "标签名称")
    @TableField("tag_name")
    private String tagName;

    /**
     * 标签颜色
     */
    @Schema(description = "标签颜色")
    @TableField("tag_color")
    private String tagColor;

    /**
     * 使用次数
     */
    @Schema(description = "使用次数")
    @TableField("use_count")
    private Integer useCount;
}
