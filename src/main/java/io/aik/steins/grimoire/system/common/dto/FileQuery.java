package io.aik.steins.grimoire.system.common.dto;

import io.aik.steins.grimoire.core.dto.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件查询参数 -anchor
 *
 * @author a I k .
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "文件查询参数")
public class FileQuery extends PageQuery {

    @Schema(description = "原始文件名（模糊）")
    private String originalName;
}
