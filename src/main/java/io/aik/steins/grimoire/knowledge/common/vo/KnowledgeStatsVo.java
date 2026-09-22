package io.aik.steins.grimoire.knowledge.common.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * -anchor 知识库统计 VO
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since 2026/09/22
 * -
 */
@Data
@Schema(description = "知识库统计")
public class KnowledgeStatsVo {

    @Schema(description = "启用知识条目数")
    private Integer knowledgeCount;

    @Schema(description = "启用分类数")
    private Integer categoryCount;

    @Schema(description = "标签总数（标签表无 status 字段，统计全部）")
    private Integer tagCount;

    @Schema(description = "有效附件挂载数（del_flag = 0）")
    private Integer attachmentCount;

    @Schema(description = "按类型分布")
    private List<TypeCountVo> typeDistribution;

    @Schema(description = "最近修改 5 条")
    private List<RecentItemVo> recentEdited;
}
