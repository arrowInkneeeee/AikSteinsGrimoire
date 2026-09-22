package io.aik.steins.grimoire.knowledge.common.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * -anchor 标签 VO
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since 2026/09/22
 * -
 */
@Data
@Schema(description = "标签")
public class TagVo {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "标签名称")
    private String tagName;

    @Schema(description = "标签颜色")
    private String tagColor;

    @Schema(description = "使用次数（读时聚合，只统计 status=1 的知识条目）")
    private Integer useCount;
}
