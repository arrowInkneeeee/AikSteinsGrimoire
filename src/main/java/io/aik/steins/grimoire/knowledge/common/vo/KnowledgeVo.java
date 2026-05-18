package io.aik.steins.grimoire.knowledge.common.vo;

import io.aik.steins.grimoire.knowledge.common.po.KnowledgeAttachmentPo;
import io.aik.steins.grimoire.knowledge.common.po.KnowledgePo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * -anchor 知识条目详情聚合 VO
 *
 * <p>包含主信息 + 标签 + 附件</p>
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since 2026/05/18
 * -
 */
@Data
@Schema(description = "知识条目详情")
public class KnowledgeVo {

    @Schema(description = "主信息")
    private KnowledgePo knowledge;

    @Schema(description = "标签列表")
    private List<String> tags;

    @Schema(description = "附件列表")
    private List<KnowledgeAttachmentPo> attachments;
}
