package io.aik.steins.grimoire.knowledge.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * -anchor 知识条目 DTO
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since 2026/05/18
 * -
 */
@Data
@Schema(description = "知识条目DTO")
public class KnowledgeDto {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "编码")
    private String code;

    @Schema(description = "类型：1-笔记 2-组件 3-方案 4-片段")
    private Integer type;

    @Schema(description = "摘要")
    private String summary;

    @Schema(description = "正文")
    private String content;

    @Schema(description = "来源项目")
    private String sourceProject;

    @Schema(description = "来源路径")
    private String sourcePath;

    @Schema(description = "资源路径")
    private String resourcePath;

    @Schema(description = "扩展字段JSON")
    private String extJson;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "状态：1-启用 0-禁用")
    private Integer status;

    @Schema(description = "标签ID列表")
    private List<Long> tagIds;
}
