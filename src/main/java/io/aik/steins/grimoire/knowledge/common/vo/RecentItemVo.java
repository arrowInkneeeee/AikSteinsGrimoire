package io.aik.steins.grimoire.knowledge.common.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * -anchor 最近编辑条目
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since 2026/09/22
 * -
 */
@Data
@Schema(description = "最近编辑条目")
public class RecentItemVo {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "修改时间")
    private LocalDateTime modifyTime;
}
