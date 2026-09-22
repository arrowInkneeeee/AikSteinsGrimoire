package io.aik.steins.grimoire.knowledge.common.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * -anchor 类型分布统计项
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since 2026/09/22
 * -
 */
@Data
@Schema(description = "类型分布统计项")
public class TypeCountVo {

    @Schema(description = "类型编码")
    private Integer type;

    @Schema(description = "类型描述")
    private String typeDesc;

    @Schema(description = "数量")
    private Integer count;
}
