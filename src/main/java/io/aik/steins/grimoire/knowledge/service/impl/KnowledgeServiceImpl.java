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
    /** 附件挂载的唯一读写入口——本模块不得直连 SysAttachmentMapper（SDD §2.5.7） */
    private final AttachmentService attachmentService;

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

        //anchor 查询附件列表（经统一挂载服务：只返回 del_flag = 0、按 sort_order ASC, id ASC 排序、
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
