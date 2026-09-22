package io.aik.steins.grimoire.knowledge.common.vo;

import io.aik.steins.grimoire.system.attachment.vo.AttachmentVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * -anchor 知识条目详情 VO（扁平化，不暴露 PO）
 *
 * <p>包含主信息 + 分类路径 + 标签 + 附件</p>
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

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "编码")
    private String code;

    @Schema(description = "类型：1-笔记 2-组件 3-方案 4-片段")
    private Integer type;

    @Schema(description = "类型描述")
    private String typeDesc;

    @Schema(description = "摘要")
    private String summary;

    @Schema(description = "正文")
    private String content;

    @Schema(description = "来源项目")
    private String sourceProject;

    @Schema(description = "来源路径")
    private String sourcePath;

    @Schema(description = "资源路径")
    private String resourcePath;

    @Schema(description = "扩展字段JSON（原样透传，解析归前端）")
    private String extJson;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "分类路径（根到当前，如 'Java > Spring'）")
    private String categoryPath;

    @Schema(description = "状态：1-启用 0-禁用")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "修改时间")
    private LocalDateTime modifyTime;

    @Schema(description = "标签列表")
    private List<TagBriefVo> tags;

    @Schema(description = "附件列表")
    private List<AttachmentVo> attachments;
}
