package io.aik.steins.grimoire.knowledge.common.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * -anchor 知识条目列表 VO
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since 2026/05/18
 * -
 */
@Data
@Schema(description = "知识条目列表项")
public class KnowledgeListVo {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "编码")
    private String code;

    @Schema(description = "类型：1-笔记 2-组件 3-方案 4-片段")
    private Integer type;

    @Schema(description = "类型描述")
    private String typeDesc;

    @Schema(description = "摘要")
    private String summary;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "标签列表")
    private List<String> tags;

    @Schema(description = "状态：1-启用 0-禁用")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
