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
import io.aik.steins.grimoire.system.attachment.dao.SysAttachmentMapper;
import io.aik.steins.grimoire.knowledge.dao.KnowledgeTagMapper;
import io.aik.steins.grimoire.knowledge.dao.KnowledgeTagRelationMapper;
import io.aik.steins.grimoire.knowledge.common.po.KnowledgeCategoryPo;
import io.aik.steins.grimoire.system.attachment.po.SysAttachmentPo;
import io.aik.steins.grimoire.knowledge.common.po.KnowledgePo;
import io.aik.steins.grimoire.knowledge.common.po.KnowledgeTagPo;
import io.aik.steins.grimoire.knowledge.common.po.KnowledgeTagRelationPo;
import io.aik.steins.grimoire.knowledge.common.vo.KnowledgeListVo;
import io.aik.steins.grimoire.knowledge.common.vo.KnowledgeVo;
import io.aik.steins.grimoire.knowledge.dao.KnowledgeMapper;
import io.aik.steins.grimoire.knowledge.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
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

        //anchor 删除附件
        sysAttachmentMapper.delete(new LambdaQueryWrapper<SysAttachmentPo>()
                .eq(SysAttachmentPo::getKnowledgeId, id));

        //anchor 删除主表
        baseMapper.deleteById(id);
    }

    @Override
    public IPage<KnowledgeListVo> findPage(KnowledgeQuery query) {
        LambdaQueryWrapper<KnowledgePo> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(query.getTitle()), KnowledgePo::getTitle, query.getTitle())
                .eq(query.getType() != null, KnowledgePo::getType, query.getType())
                .eq(query.getCategoryId() != null, KnowledgePo::getCategoryId, query.getCategoryId())
                .eq(query.getStatus() != null, KnowledgePo::getStatus, query.getStatus())
                .orderByDesc(KnowledgePo::getCreateTime);

        IPage<KnowledgePo> page = baseMapper.selectPage(query.toPage(), wrapper);

        return page.convert(po -> {
            KnowledgeListVo vo = new KnowledgeListVo();
            vo.setId(po.getId());
            vo.setTitle(po.getTitle());
            vo.setCode(po.getCode());
            vo.setType(po.getType());
            vo.setTypeDesc(KnowledgeTypeEnum.of(po.getType()) != null ? KnowledgeTypeEnum.of(po.getType()).getDesc() : "");
            vo.setSummary(po.getSummary());
            vo.setStatus(po.getStatus());
            vo.setCreateTime(po.getCreateTime());

            //anchor 查询分类名称
            if (po.getCategoryId() != null) {
                KnowledgeCategoryPo category = knowledgeCategoryMapper.selectById(po.getCategoryId());
                vo.setCategoryName(category != null ? category.getCategoryName() : "");
            }

            //anchor 查询标签列表
            List<KnowledgeTagRelationPo> relations = knowledgeTagRelationMapper.selectList(
                    new LambdaQueryWrapper<KnowledgeTagRelationPo>()
                            .eq(KnowledgeTagRelationPo::getKnowledgeId, po.getId()));
            if (!relations.isEmpty()) {
                List<Long> tagIds = relations.stream().map(KnowledgeTagRelationPo::getTagId).collect(Collectors.toList());
                List<KnowledgeTagPo> tags = knowledgeTagMapper.selectBatchIds(tagIds);
                vo.setTags(tags.stream().map(KnowledgeTagPo::getTagName).collect(Collectors.toList()));
            }

            return vo;
        });
    }

    @Override
    public KnowledgeVo findById(Long id) {
        AssertUtils.notNull(id, "ID不能为空");
        KnowledgePo po = baseMapper.selectById(id);
        AssertUtils.notNull(po, "知识条目不存在");

        KnowledgeVo vo = new KnowledgeVo();
        vo.setKnowledge(po);

        //anchor 查询标签列表
        List<KnowledgeTagRelationPo> relations = knowledgeTagRelationMapper.selectList(
                new LambdaQueryWrapper<KnowledgeTagRelationPo>()
                        .eq(KnowledgeTagRelationPo::getKnowledgeId, id));
        if (!relations.isEmpty()) {
            List<Long> tagIds = relations.stream().map(KnowledgeTagRelationPo::getTagId).collect(Collectors.toList());
            List<KnowledgeTagPo> tags = knowledgeTagMapper.selectBatchIds(tagIds);
            vo.setTags(tags.stream().map(KnowledgeTagPo::getTagName).collect(Collectors.toList()));
        }

        //anchor 查询附件列表
        List<SysAttachmentPo> attachments = sysAttachmentMapper.selectList(
                new LambdaQueryWrapper<SysAttachmentPo>()
                        .eq(SysAttachmentPo::getKnowledgeId, id)
                        .orderByAsc(SysAttachmentPo::getSortOrder));
        vo.setAttachments(attachments);

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
}
