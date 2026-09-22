package io.aik.steins.grimoire.knowledge.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * -anchor 标签 DTO（新增 / 修改共用）
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since 2026/09/22
 * -
 */
@Data
@Schema(description = "标签DTO")
public class TagDto {

    @Schema(description = "主键ID（修改时必填）")
    private Long id;

    @Schema(description = "标签名称（必填，≤64，全局唯一）")
    private String tagName;

    @Schema(description = "标签颜色（可空，格式 #RGB 或 #RRGGBB）")
    private String tagColor;
}
