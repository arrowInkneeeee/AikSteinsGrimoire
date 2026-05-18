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
 * -anchor 知识标签关联
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
@Schema(description = "知识标签关联")
@TableName(KnowledgeConstant.TABLE_PREFIX + "tag_relation")
public class KnowledgeTagRelationPo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    public KnowledgeTagRelationPo() {
        super();
    }

    /**
     * 主键ID
     */
    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * 标签ID
     */
    @Schema(description = "标签ID")
    @TableField("tag_id")
    private Long tagId;

    /**
     * 知识条目ID
     */
    @Schema(description = "知识条目ID")
    @TableField("knowledge_id")
    private Long knowledgeId;
}
