package io.aik.steins.grimoire.knowledge.common.dto;

import io.aik.steins.grimoire.core.dto.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * -anchor 标签查询条件
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since 2026/09/22
 * -
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "标签查询条件")
public class TagQuery extends PageQuery {

    @Schema(description = "标签名称关键字")
    private String tagName;
}
