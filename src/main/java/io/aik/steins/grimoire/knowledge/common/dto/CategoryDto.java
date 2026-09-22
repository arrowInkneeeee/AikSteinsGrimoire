package io.aik.steins.grimoire.knowledge.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * -anchor 分类 DTO（新增 / 修改共用）
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since 2026/09/22
 * -
 */
@Data
@Schema(description = "分类DTO")
public class CategoryDto {

    @Schema(description = "主键ID（修改时必填）")
    private Long id;

    @Schema(description = "分类名称（必填，≤128）")
    private String categoryName;

    @Schema(description = "分类编码（可空，Service 层校验全局唯一）")
    private String categoryCode;

    @Schema(description = "父分类ID（可空，默认 0 = 根节点）")
    private Long parentId;

    @Schema(description = "排序号（默认 0）")
    private Integer sortOrder;

    @Schema(description = "状态：1-启用 0-禁用（默认 1）")
    private Integer status;
}
