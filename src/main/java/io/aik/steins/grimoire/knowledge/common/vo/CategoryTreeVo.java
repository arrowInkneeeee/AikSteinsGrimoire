package io.aik.steins.grimoire.knowledge.common.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * -anchor 分类树节点 VO（findTree 返回）
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since 2026/09/22
 * -
 */
@Data
@Schema(description = "分类树节点")
public class CategoryTreeVo {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "父分类ID（根节点为 0）")
    private Long parentId;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "分类编码")
    private String categoryCode;

    @Schema(description = "排序号")
    private Integer sortOrder;

    @Schema(description = "状态：1-启用 0-禁用")
    private Integer status;

    @Schema(description = "直接挂载的启用条目数")
    private Integer directCount;

    @Schema(description = "自身 + 所有子孙的启用条目数（自底向上上卷）")
    private Integer totalCount;

    @Schema(description = "子节点列表")
    private List<CategoryTreeVo> children;
}
