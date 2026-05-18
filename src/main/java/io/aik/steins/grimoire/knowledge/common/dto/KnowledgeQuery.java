package io.aik.steins.grimoire.knowledge.common.dto;

import io.aik.steins.grimoire.core.dto.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * -anchor 知识条目查询条件
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since 2026/05/18
 * -
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "知识条目查询条件")
public class KnowledgeQuery extends PageQuery {

    @Schema(description = "标题关键字")
    private String title;

    @Schema(description = "类型：1-笔记 2-组件 3-方案 4-片段")
    private Integer type;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "状态：1-启用 0-禁用")
    private Integer status;
}
