package io.aik.steins.grimoire.system.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 文件重命名请求 -anchor
 *
 * @author a I k .
 */
@Data
@Schema(description = "文件重命名请求")
public class FileRenameDto {

    @NotNull(message = "文件 ID 不能为空")
    @Schema(description = "文件 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @NotBlank(message = "新文件名不能为空")
    @Schema(description = "新文件名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String originalName;
}
