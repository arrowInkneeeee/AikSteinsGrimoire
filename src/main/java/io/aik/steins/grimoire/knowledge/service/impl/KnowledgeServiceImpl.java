package io.aik.steins.grimoire.knowledge.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.aik.steins.grimoire.core.exception.BusinessException;
import io.aik.steins.grimoire.core.utils.AssertUtils;
import io.aik.steins.grimoire.knowledge.common.constant.KnowledgeConstant;
import io.aik.steins.grimoire.knowledge.common.dto.KnowledgeDto;
import io.aik.steins.grimoire.knowledge.common.dto.KnowledgeQuery;
import io.aik.steins.grimoire.knowledge.common.enums.KnowledgeTypeEnum;
import io.aik.steins.grimoire.knowledge.dao.KnowledgeCategoryMapper;
import io.aik.steins.grimoire.knowledge.dao.KnowledgeTagMapper;
import io.aik.steins.grimoire.knowledge.dao.KnowledgeTagRelationMapper;
import io.aik.steins.grimoire.knowledge.common.po.KnowledgeCategoryPo;
import io.aik.steins.grimoire.system.attachment.constant.AttachmentBizType;
import io.aik.steins.grimoire.system.attachment.dto.AttachmentDto;
import io.aik.steins.grimoire.system.attachment.service.AttachmentService;
import io.aik.steins.grimoire.system.attachment.vo.AttachmentVo;
import io.aik.steins.grimoire.knowledge.common.po.KnowledgePo;
import io.aik.steins.grimoire.knowledge.common.po.KnowledgeTagPo;
import io.aik.steins.grimoire.knowledge.common.po.KnowledgeTagRelationPo;
import io.aik.steins.grimoire.system.attachment.po.SysAttachmentPo;
import io.aik.steins.grimoire.knowledge.common.vo.KnowledgeListVo;
import io.aik.steins.grimoire.knowledge.common.vo.KnowledgeStatsVo;
import io.aik.steins.grimoire.knowledge.common.vo.KnowledgeVo;
import io.aik.steins.grimoire.knowledge.common.vo.RecentItemVo;
import io.aik.steins.grimoire.knowledge.common.vo.TagBriefVo;
import io.aik.steins.grimoire.knowledge.common.vo.TypeCountVo;
import io.aik.steins.grimoire.system.attachment.dao.SysAttachmentMapper;
import io.aik.steins.grimoire.knowledge.dao.KnowledgeMapper;
import io.aik.steins.grimoire.knowledge.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * -anchor 知识条目 Service 实现
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since 2026/05/18
 * -
 */
@Service
@RequiredArgsConstructor
public class KnowledgeServiceImpl extends ServiceImpl<KnowledgeMapper, KnowledgePo> implements KnowledgeService {

    private final KnowledgeCategoryMapper knowledgeCategoryMapper;
    private final KnowledgeTagMapper knowledgeTagMapper;
    private final KnowledgeTagRelationMapper knowledgeTagRelationMapper;
    /** 附件挂载的唯一读写入口——本模块不得直连 SysAttachmentMapper（SDD §2.5.7） */
    private final AttachmentService attachmentService;
    private final SysAttachmentMapper sysAttachmentMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(KnowledgeDto dto) {
        AssertUtils.notEmpty(dto.getTitle(), "标题不能为空");
        AssertUtils.notNull(dto.getType(), "类型不能为空");

        KnowledgePo po = new KnowledgePo();
        po.setId(IdUtil.getSnowflakeNextId());
        po.setTitle(dto.getTitle());
        po.setCode(dto.getCode());
        po.setType(dto.getType());
        po.setSummary(dto.getSummary());
        po.setContent(dto.getContent());
        po.setSourceProject(dto.getSourceProject());
        po.setSourcePath(dto.getSourcePath());
        po.setResourcePath(dto.getResourcePath());
        po.setExtJson(dto.getExtJson());
        po.setCategoryId(dto.getCategoryId());
        po.setStatus(dto.getStatus() != null ? dto.getStatus() : KnowledgeConstant.STATUS_ENABLE);

        baseMapper.insert(po);

        //anchor 保存标签关联
        saveTagRelations(po.getId(), dto.getTagIds());

        //anchor 保存附件挂载（差量语义；bizType 固定为 KNOWLEDGE，不由前端传）
        //       attachments 为 null 表示"本次不涉及附件"，空列表才是"卸载全部"
        saveAttachments(po.getId(), dto.getAttachments());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(KnowledgeDto dto) {
        AssertUtils.notNull(dto.getId(), "ID不能为空");
        KnowledgePo exist = baseMapper.selectById(dto.getId());
        AssertUtils.notNull(exist, "知识条目不存在");

        KnowledgePo po = new KnowledgePo();
        po.setId(dto.getId());
        po.setTitle(dto.getTitle());
        po.setCode(dto.getCode());
        po.setType(dto.getType());
        po.setSummary(dto.getSummary());
        po.setContent(dto.getContent());
        po.setSourceProject(dto.getSourceProject());
        po.setSourcePath(dto.getSourcePath());
        po.setResourcePath(dto.getResourcePath());
        po.setExtJson(dto.getExtJson());
        po.setCategoryId(dto.getCategoryId());
        po.setStatus(dto.getStatus());

        baseMapper.updateById(po);

        //anchor 更新标签关联：先删后增
        knowledgeTagRelationMapper.delete(new LambdaQueryWrapper<KnowledgeTagRelationPo>()
                .eq(KnowledgeTagRelationPo::getKnowledgeId, dto.getId()));
        saveTagRelations(dto.getId(), dto.getTagIds());

        //anchor 更新附件挂载（差量语义；禁止全删再全插——uk_biz_file 不含 del_flag，
        //       全删产生的卸载行仍占键值，会让紧接着的全插撞唯一键）
        saveAttachments(dto.getId(), dto.getAttachments());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        AssertUtils.notNull(id, "ID不能为空");
        KnowledgePo exist = baseMapper.selectById(id);
        AssertUtils.notNull(exist, "知识条目不存在");

        //anchor 删除标签关联
        knowledgeTagRelationMapper.delete(new LambdaQueryWrapper<KnowledgeTagRelationPo>()
                .eq(KnowledgeTagRelationPo::getKnowledgeId, id));

        //anchor 删除附件挂载：逐条走统一删除入口（删挂载行 → 查引用计数 → 无则删文件记录 + 提交后删盘）
        //       禁止用一条 delete(wrapper) 批量删挂载：它会绕开统一入口，
        //       留下"挂载行没了、文件记录和磁盘文件都还在"的文件层墓碑
        for (AttachmentVo attachment : attachmentService.listByBiz(AttachmentBizType.KNOWLEDGE, id)) {
            attachmentService.remove(attachment.getId());
        }

        //anchor 删除主表
        baseMapper.deleteById(id);
    }

    @Override
    public IPage<KnowledgeListVo> findPage(KnowledgeQuery query) {
        LambdaQueryWrapper<KnowledgePo> wrapper = new LambdaQueryWrapper<>();

        //anchor 标题关键字（LIKE）
        wrapper.like(StrUtil.isNotBlank(query.getTitle()), KnowledgePo::getTitle, query.getTitle());

        //anchor 跨字段关键字：title + summary + content，OR 连接
        if (StrUtil.isNotBlank(query.getKeyword())) {
            String kw = query.getKeyword();
            wrapper.and(w -> w.like(KnowledgePo::getTitle, kw)
                    .or().like(KnowledgePo::getSummary, kw)
                    .or().like(KnowledgePo::getContent, kw));
        }

        wrapper.eq(query.getType() != null, KnowledgePo::getType, query.getType());

        //anchor 分类过滤：includeChildren=true 时扩展到全部子孙分类
        if (query.getCategoryId() != null) {
            if (Boolean.TRUE.equals(query.getIncludeChildren())) {
                Map<Long, KnowledgeCategoryPo> allCats = loadCategoryMap();
                List<Long> categoryIds = collectDescendantIds(query.getCategoryId(), allCats);
                categoryIds.add(query.getCategoryId());
                wrapper.in(KnowledgePo::getCategoryId, categoryIds);
            } else {
                wrapper.eq(KnowledgePo::getCategoryId, query.getCategoryId());
            }
        }

        //anchor 按标签过滤：先查关联表取 knowledge_id 集合，再 IN
        if (query.getTagId() != null) {
            List<KnowledgeTagRelationPo> tagRels = knowledgeTagRelationMapper.selectList(
                    new LambdaQueryWrapper<KnowledgeTagRelationPo>()
                            .eq(KnowledgeTagRelationPo::getTagId, query.getTagId()));
            if (tagRels.isEmpty()) {
                //anchor 短路：没有任何知识条目关联该标签，直接返回空页
                return new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(
                        query.getCurrent(), query.getSize());
            }
            List<Long> knowledgeIds = tagRels.stream()
                    .map(KnowledgeTagRelationPo::getKnowledgeId).collect(Collectors.toList());
            wrapper.in(KnowledgePo::getId, knowledgeIds);
        }

        //anchor 状态过滤（默认 1 = 仅启用；管理端显式传 null 查全部）
        wrapper.eq(query.getStatus() != null, KnowledgePo::getStatus, query.getStatus());
        wrapper.orderByDesc(KnowledgePo::getCreateTime);

        IPage<KnowledgePo> page = baseMapper.selectPage(query.toPage(), wrapper);

        //anchor 批量预加载分类、标签关联、标签——避免逐条查询（N+1 修复）
        Map<Long, KnowledgeCategoryPo> categoryMap = loadCategoryMap();
        List<Long> knowledgeIds = page.getRecords().stream()
                .map(KnowledgePo::getId).collect(Collectors.toList());
        Map<Long, List<KnowledgeTagPo>> tagsByKnowledge = batchLoadTags(knowledgeIds);

        return page.convert(po -> {
            KnowledgeListVo vo = new KnowledgeListVo();
            vo.setId(po.getId());
            vo.setTitle(po.getTitle());
            vo.setCode(po.getCode());
            vo.setType(po.getType());
            vo.setTypeDesc(KnowledgeTypeEnum.of(po.getType()) != null
                    ? KnowledgeTypeEnum.of(po.getType()).getDesc() : "");
            vo.setSummary(po.getSummary());
            vo.setCategoryId(po.getCategoryId());
            vo.setStatus(po.getStatus());
            vo.setCreateTime(po.getCreateTime());

            //anchor 分类名称 + 面包屑路径
            if (po.getCategoryId() != null) {
                KnowledgeCategoryPo category = categoryMap.get(po.getCategoryId());
                if (category != null) {
                    vo.setCategoryName(category.getCategoryName());
                    vo.setCategoryPath(buildCategoryPath(po.getCategoryId(), categoryMap));
                }
            }

            //anchor 标签列表（TagBriefVo：含 id / tagName / tagColor）
            vo.setTags(convertToTagBriefVoList(tagsByKnowledge.getOrDefault(
                    po.getId(), Collections.emptyList())));

            return vo;
        });
    }

    @Override
    public KnowledgeVo findById(Long id) {
        AssertUtils.notNull(id, "ID不能为空");
        KnowledgePo po = baseMapper.selectById(id);
        AssertUtils.notNull(po, "知识条目不存在");

        //anchor 扁平化组装（不再嵌套 KnowledgePo）
        KnowledgeVo vo = new KnowledgeVo();
        vo.setId(po.getId());
        vo.setTitle(po.getTitle());
        vo.setCode(po.getCode());
        vo.setType(po.getType());
        vo.setTypeDesc(KnowledgeTypeEnum.of(po.getType()) != null
                ? KnowledgeTypeEnum.of(po.getType()).getDesc() : "");
        vo.setSummary(po.getSummary());
        vo.setContent(po.getContent());
        vo.setSourceProject(po.getSourceProject());
        vo.setSourcePath(po.getSourcePath());
        vo.setResourcePath(po.getResourcePath());
        vo.setExtJson(po.getExtJson());
        vo.setCategoryId(po.getCategoryId());
        vo.setStatus(po.getStatus());
        vo.setCreateTime(po.getCreateTime());
        vo.setModifyTime(po.getModifyTime());

        //anchor 分类名称 + 面包屑路径
        Map<Long, KnowledgeCategoryPo> categoryMap = loadCategoryMap();
        if (po.getCategoryId() != null) {
            KnowledgeCategoryPo category = categoryMap.get(po.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getCategoryName());
                vo.setCategoryPath(buildCategoryPath(po.getCategoryId(), categoryMap));
            }
        }

        //anchor 标签列表（TagBriefVo：含 id / tagName / tagColor）
        List<KnowledgeTagRelationPo> relations = knowledgeTagRelationMapper.selectList(
                new LambdaQueryWrapper<KnowledgeTagRelationPo>()
                        .eq(KnowledgeTagRelationPo::getKnowledgeId, id));
        if (!relations.isEmpty()) {
            List<Long> tagIds = relations.stream()
                    .map(KnowledgeTagRelationPo::getTagId).collect(Collectors.toList());
            List<KnowledgeTagPo> tags = knowledgeTagMapper.selectBatchIds(tagIds);
            vo.setTags(convertToTagBriefVoList(tags));
        }

        //anchor 附件列表（经统一挂载服务：只返回 del_flag = 0、按 sort_order ASC, id ASC 排序、
        //       文件元数据按 fileId 批量取，禁 N+1）
        vo.setAttachments(attachmentService.listByBiz(AttachmentBizType.KNOWLEDGE, id));

        return vo;
    }

    @Override
    public void toggleStatus(Long id, Integer status) {
        AssertUtils.notNull(id, "ID不能为空");
        AssertUtils.notNull(status, "状态不能为空");
        KnowledgePo exist = baseMapper.selectById(id);
        AssertUtils.notNull(exist, "知识条目不存在");

        KnowledgePo po = new KnowledgePo();
        po.setId(id);
        po.setStatus(status);
        baseMapper.updateById(po);
    }

    @Override
    public KnowledgeStatsVo stats() {
        KnowledgeStatsVo stats = new KnowledgeStatsVo();

        //anchor 启用知识条目数
        stats.setKnowledgeCount(Math.toIntExact(baseMapper.selectCount(
                new LambdaQueryWrapper<KnowledgePo>().eq(KnowledgePo::getStatus, 1))));

        //anchor 启用分类数
        stats.setCategoryCount(Math.toIntExact(knowledgeCategoryMapper.selectCount(
                new LambdaQueryWrapper<KnowledgeCategoryPo>().eq(KnowledgeCategoryPo::getStatus, 1))));

        //anchor 标签总数（标签表无 status 字段）
        stats.setTagCount(Math.toIntExact(knowledgeTagMapper.selectCount(null)));

        //anchor 有效附件挂载数（del_flag = 0）
        stats.setAttachmentCount(Math.toIntExact(sysAttachmentMapper.selectCount(
                new LambdaQueryWrapper<SysAttachmentPo>().eq(SysAttachmentPo::getDelFlag, 0))));

        //anchor 按类型分布（GROUP BY type WHERE status = 1）
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<KnowledgePo> typeQw =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        typeQw.select("type", "COUNT(*) AS cnt")
                .eq("status", 1)
                .groupBy("type");
        List<Map<String, Object>> typeRows = baseMapper.selectMaps(typeQw);
        List<TypeCountVo> typeDistribution = new ArrayList<>();
        for (Map<String, Object> row : typeRows) {
            TypeCountVo tc = new TypeCountVo();
            tc.setType(((Number) row.get("type")).intValue());
            KnowledgeTypeEnum typeEnum = KnowledgeTypeEnum.of(tc.getType());
            tc.setTypeDesc(typeEnum != null ? typeEnum.getDesc() : "");
            tc.setCount(((Number) row.get("cnt")).intValue());
            typeDistribution.add(tc);
        }
        stats.setTypeDistribution(typeDistribution);

        //anchor 最近修改 5 条（按 modify_time DESC，不限 status——管理视角）
        List<KnowledgePo> recentList = baseMapper.selectList(
                new LambdaQueryWrapper<KnowledgePo>()
                        .orderByDesc(KnowledgePo::getModifyTime)
                        .last("LIMIT 5"));
        List<RecentItemVo> recentEdited = recentList.stream().map(po -> {
            RecentItemVo item = new RecentItemVo();
            item.setId(po.getId());
            item.setTitle(po.getTitle());
            item.setModifyTime(po.getModifyTime());
            return item;
        }).collect(Collectors.toList());
        stats.setRecentEdited(recentEdited);

        return stats;
    }

    @Override
    public List<KnowledgeListVo> findAll() {
        LambdaQueryWrapper<KnowledgePo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgePo::getStatus, KnowledgeConstant.STATUS_ENABLE);
        wrapper.orderByDesc(KnowledgePo::getCreateTime);

        List<KnowledgePo> allPos = baseMapper.selectList(wrapper);

        //anchor 批量预加载分类、标签关联——与 findPage 同一套 N+1 修复逻辑
        Map<Long, KnowledgeCategoryPo> categoryMap = loadCategoryMap();
        List<Long> knowledgeIds = allPos.stream()
                .map(KnowledgePo::getId).collect(Collectors.toList());
        Map<Long, List<KnowledgeTagPo>> tagsByKnowledge = batchLoadTags(knowledgeIds);

        return allPos.stream().map(po -> {
            KnowledgeListVo vo = new KnowledgeListVo();
            vo.setId(po.getId());
            vo.setTitle(po.getTitle());
            vo.setCode(po.getCode());
            vo.setType(po.getType());
            vo.setTypeDesc(KnowledgeTypeEnum.of(po.getType()) != null
                    ? KnowledgeTypeEnum.of(po.getType()).getDesc() : "");
            vo.setSummary(po.getSummary());
            vo.setCategoryId(po.getCategoryId());
            vo.setStatus(po.getStatus());
            vo.setCreateTime(po.getCreateTime());

            if (po.getCategoryId() != null) {
                KnowledgeCategoryPo category = categoryMap.get(po.getCategoryId());
                if (category != null) {
                    vo.setCategoryName(category.getCategoryName());
                    vo.setCategoryPath(buildCategoryPath(po.getCategoryId(), categoryMap));
                }
            }

            vo.setTags(convertToTagBriefVoList(tagsByKnowledge.getOrDefault(
                    po.getId(), Collections.emptyList())));

            return vo;
        }).collect(Collectors.toList());
    }

    //anchor ========== 私有辅助方法 ==========

    /**
     * 加载全部分类到 Map（分类表极小，一次 selectList 即可）
     */
    private Map<Long, KnowledgeCategoryPo> loadCategoryMap() {
        List<KnowledgeCategoryPo> allCategories = knowledgeCategoryMapper.selectList(null);
        return allCategories.stream()
                .collect(Collectors.toMap(KnowledgeCategoryPo::getId, c -> c));
    }

    /**
     * 递归收集指定分类的全部子孙 ID（不含自身）
     */
    private List<Long> collectDescendantIds(Long parentId,
            Map<Long, KnowledgeCategoryPo> categoryMap) {
        List<Long> result = new ArrayList<>();
        for (KnowledgeCategoryPo cat : categoryMap.values()) {
            if (parentId.equals(cat.getParentId())) {
                result.add(cat.getId());
                result.addAll(collectDescendantIds(cat.getId(), categoryMap));
            }
        }
        return result;
    }

    /**
     * 构建分类面包屑路径（如 "魔典卷轴 > 子分类"）
     */
    private String buildCategoryPath(Long categoryId,
            Map<Long, KnowledgeCategoryPo> categoryMap) {
        List<String> names = new ArrayList<>();
        Long currentId = categoryId;
        while (currentId != null && currentId != 0L) {
            KnowledgeCategoryPo cat = categoryMap.get(currentId);
            if (cat == null) {
                break;
            }
            names.add(cat.getCategoryName());
            currentId = cat.getParentId();
        }
        Collections.reverse(names);
        return String.join(" > ", names);
    }

    /**
     * 标签 PO 列表 → TagBriefVo 列表
     */
    private List<TagBriefVo> convertToTagBriefVoList(List<KnowledgeTagPo> tags) {
        if (tags == null || tags.isEmpty()) {
            return Collections.emptyList();
        }
        return tags.stream().map(tag -> {
            TagBriefVo brief = new TagBriefVo();
            brief.setId(tag.getId());
            brief.setTagName(tag.getTagName());
            brief.setTagColor(tag.getTagColor());
            return brief;
        }).collect(Collectors.toList());
    }

    /**
     * 批量加载知识条目的标签（按 knowledgeId 分组）——用于 findPage 的 N+1 修复
     */
    private Map<Long, List<KnowledgeTagPo>> batchLoadTags(List<Long> knowledgeIds) {
        if (knowledgeIds == null || knowledgeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        //anchor 一次查回全部标签关联
        List<KnowledgeTagRelationPo> allRelations = knowledgeTagRelationMapper.selectList(
                new LambdaQueryWrapper<KnowledgeTagRelationPo>()
                        .in(KnowledgeTagRelationPo::getKnowledgeId, knowledgeIds));
        if (allRelations.isEmpty()) {
            return Collections.emptyMap();
        }
        //anchor 收集全部 tagId，一次批量取标签
        List<Long> allTagIds = allRelations.stream()
                .map(KnowledgeTagRelationPo::getTagId).distinct().collect(Collectors.toList());
        Map<Long, KnowledgeTagPo> tagMap = knowledgeTagMapper.selectBatchIds(allTagIds)
                .stream().collect(Collectors.toMap(KnowledgeTagPo::getId, t -> t));
        //anchor 按 knowledgeId 分组
        Map<Long, List<KnowledgeTagPo>> result = new java.util.HashMap<>();
        for (KnowledgeTagRelationPo rel : allRelations) {
            KnowledgeTagPo tag = tagMap.get(rel.getTagId());
            if (tag != null) {
                result.computeIfAbsent(rel.getKnowledgeId(), k -> new ArrayList<>()).add(tag);
            }
        }
        return result;
    }

    /**
     * 保存标签关联
     *
     * @param knowledgeId 知识条目ID
     * @param tagIds      标签ID列表
     */
    private void saveTagRelations(Long knowledgeId, List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        List<KnowledgeTagRelationPo> relations = new ArrayList<>();
        for (Long tagId : tagIds) {
            KnowledgeTagRelationPo relation = new KnowledgeTagRelationPo();
            relation.setId(IdUtil.getSnowflakeNextId());
            relation.setTagId(tagId);
            relation.setKnowledgeId(knowledgeId);
            relations.add(relation);
        }
        //anchor 批量插入
        for (KnowledgeTagRelationPo relation : relations) {
            knowledgeTagRelationMapper.insert(relation);
        }
    }

    /**
     * 保存附件挂载（差量语义）-anchor
     *
     * <p>{@code attachments} 为 {@code null} 表示"本次不涉及附件"（不动作）；
     * <b>空列表</b>表示"卸载该业务全部挂载"。{@code bizType} 固定为
     * {@link AttachmentBizType#KNOWLEDGE}，<b>不由前端传</b>。</p>
     *
     * @param knowledgeId 知识条目ID
     * @param attachments 附件列表；{@code null} = 本次不涉及
     */
    private void saveAttachments(Long knowledgeId, List<AttachmentDto> attachments) {
        if (attachments == null) {
            return;
        }
        attachmentService.save(AttachmentBizType.KNOWLEDGE, knowledgeId, attachments);
    }
}
