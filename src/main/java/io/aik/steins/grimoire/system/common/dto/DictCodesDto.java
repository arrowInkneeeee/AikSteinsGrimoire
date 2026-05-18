package io.aik.steins.grimoire.system.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 字典编码批量查询请求 -anchor
 *
 * @author a I k .
 */
@Data
@Schema(description = "字典编码批量查询请求")
public class DictCodesDto {

    @NotEmpty(message = "字典编码列表不能为空")
    @Schema(description = "字典编码列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> dictCodes;
}
