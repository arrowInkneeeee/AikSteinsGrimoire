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
 * -anchor 知识附件
 *
 * <p>知识条目的附属文件（设计图、架构图、文档等）</p>
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
@Schema(description = "知识附件")
@TableName(KnowledgeConstant.TABLE_PREFIX + "attachment")
public class KnowledgeAttachmentPo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    public KnowledgeAttachmentPo() {
        super();
    }

    /**
     * 主键ID
     */
    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * 知识条目ID
     */
    @Schema(description = "知识条目ID")
    @TableField("knowledge_id")
    private Long knowledgeId;

    /**
     * 附件名称
     */
    @Schema(description = "附件名称")
    @TableField("attach_name")
    private String attachName;

    /**
     * 附件URL/存储路径
     */
    @Schema(description = "附件URL/存储路径")
    @TableField("attach_url")
    private String attachUrl;

    /**
     * 描述
     */
    @Schema(description = "描述")
    @TableField("description")
    private String description;

    /**
     * 排序号
     */
    @Schema(description = "排序号")
    @TableField("sort_order")
    private Integer sortOrder;
}
