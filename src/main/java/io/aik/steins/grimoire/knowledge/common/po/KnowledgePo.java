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
 * -anchor 知识条目
 *
 * <p>统一知识库主表，通过 type 字段区分知识类型</p>
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
@Schema(description = "知识条目")
@TableName(KnowledgeConstant.TABLE_PREFIX + "knowledge")
public class KnowledgePo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    public KnowledgePo() {
        super();
    }

    /**
     * 主键ID
     */
    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * 标题（组件名/方案名/笔记标题/片段描述）
     */
    @Schema(description = "标题")
    @TableField("title")
    private String title;

    /**
     * 编码（组件编码/方案编码）
     */
    @Schema(description = "编码")
    @TableField("code")
    private String code;

    /**
     * 类型：1-笔记 2-组件 3-方案 4-片段
     */
    @Schema(description = "类型：1-笔记 2-组件 3-方案 4-片段")
    @TableField("type")
    private Integer type;

    /**
     * 摘要/用途描述
     */
    @Schema(description = "摘要")
    @TableField("summary")
    private String summary;

    /**
     * 正文（笔记内容/代码片段内容/方案描述）
     */
    @Schema(description = "正文")
    @TableField("content")
    private String content;

    /**
     * 来源项目
     */
    @Schema(description = "来源项目")
    @TableField("source_project")
    private String sourceProject;

    /**
     * 来源路径
     */
    @Schema(description = "来源路径")
    @TableField("source_path")
    private String sourcePath;

    /**
     * 资源路径（指向 components/ 或 solutions/ 下的包路径）
     */
    @Schema(description = "资源路径")
    @TableField("resource_path")
    private String resourcePath;

    /**
     * 扩展字段JSON（不同类型特有属性）
     */
    @Schema(description = "扩展字段JSON")
    @TableField("ext_json")
    private String extJson;

    /**
     * 分类ID
     */
    @Schema(description = "分类ID")
    @TableField("category_id")
    private Long categoryId;

    /**
     * 状态：1-启用 0-禁用
     */
    @Schema(description = "状态：1-启用 0-禁用")
    @TableField("status")
    private Integer status;
}
