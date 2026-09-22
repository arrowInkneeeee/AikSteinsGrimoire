package io.aik.steins.grimoire.knowledge.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.aik.steins.grimoire.core.exception.BusinessException;
import io.aik.steins.grimoire.core.utils.AssertUtils;
import io.aik.steins.grimoire.knowledge.common.dto.TagDto;
import io.aik.steins.grimoire.knowledge.common.dto.TagQuery;
import io.aik.steins.grimoire.knowledge.common.po.KnowledgePo;
import io.aik.steins.grimoire.knowledge.common.po.KnowledgeTagPo;
import io.aik.steins.grimoire.knowledge.common.po.KnowledgeTagRelationPo;
import io.aik.steins.grimoire.knowledge.common.vo.TagVo;
import io.aik.steins.grimoire.knowledge.dao.KnowledgeMapper;
import io.aik.steins.grimoire.knowledge.dao.KnowledgeTagMapper;
import io.aik.steins.grimoire.knowledge.dao.KnowledgeTagRelationMapper;
import io.aik.steins.grimoire.knowledge.service.TagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * -anchor 标签 Service 实现
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since 2026/09/22
 * -
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class TagServiceImpl extends ServiceImpl<KnowledgeTagMapper, KnowledgeTagPo>
        implements TagService {

    private final KnowledgeTagRelationMapper knowledgeTagRelationMapper;
    private final KnowledgeMapper knowledgeMapper;

    /** 标签颜色正则：#RGB 或 #RRGGBB */
    private static final Pattern COLOR_PATTERN = Pattern.compile("^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6})$");

    //anchor ========== 查询 ==========

    @Override
    public List<TagVo> findAll() {
        List<KnowledgeTagPo> allTags = baseMapper.selectList(null);
        Map<Long, Integer> useCountMap = loadUseCountMap();
        return allTags.stream().map(tag -> convertToVo(tag, useCountMap)).collect(Collectors.toList());
    }

    @Override
    public IPage<TagVo> findPage(TagQuery query) {
        LambdaQueryWrapper<KnowledgeTagPo> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(query.getTagName()),
                KnowledgeTagPo::getTagName, query.getTagName());

        IPage<KnowledgeTagPo> page = baseMapper.selectPage(query.toPage(), wrapper);
        Map<Long, Integer> useCountMap = loadUseCountMap();
        return page.convert(tag -> convertToVo(tag, useCountMap));
    }

    @Override
    public TagVo findById(Long id) {
        AssertUtils.notNull(id, "ID不能为空");
        KnowledgeTagPo po = baseMapper.selectById(id);
        AssertUtils.notNull(po, "标签不存在");
        Map<Long, Integer> useCountMap = loadUseCountMap();
        return convertToVo(po, useCountMap);
    }

    //anchor ========== 写入 ==========

    @Override
    public void add(TagDto dto) {
        AssertUtils.notEmpty(dto.getTagName(), "标签名称不能为空");
        AssertUtils.isTrue(dto.getTagName().length() <= 64, "标签名称不能超过64个字符");
        validateColor(dto.getTagColor());

        //anchor 重名校验（DB 有 uk_tag_name，此处转为可读异常）
        Long count = baseMapper.selectCount(
                new LambdaQueryWrapper<KnowledgeTagPo>()
                        .eq(KnowledgeTagPo::getTagName, dto.getTagName()));
        AssertUtils.isTrue(count == 0, "标签名称已存在：" + dto.getTagName());

        KnowledgeTagPo po = new KnowledgeTagPo();
        po.setId(IdUtil.getSnowflakeNextId());
        po.setTagName(dto.getTagName());
        po.setTagColor(dto.getTagColor());

        baseMapper.insert(po);
    }

    @Override
    public void modify(TagDto dto) {
        AssertUtils.notNull(dto.getId(), "ID不能为空");
        KnowledgeTagPo exist = baseMapper.selectById(dto.getId());
        AssertUtils.notNull(exist, "标签不存在");

        AssertUtils.notEmpty(dto.getTagName(), "标签名称不能为空");
        AssertUtils.isTrue(dto.getTagName().length() <= 64, "标签名称不能超过64个字符");
        validateColor(dto.getTagColor());

        //anchor 重名校验（排除自身）
        Long count = baseMapper.selectCount(
                new LambdaQueryWrapper<KnowledgeTagPo>()
                        .eq(KnowledgeTagPo::getTagName, dto.getTagName())
                        .ne(KnowledgeTagPo::getId, dto.getId()));
        AssertUtils.isTrue(count == 0, "标签名称已存在：" + dto.getTagName());

        KnowledgeTagPo po = new KnowledgeTagPo();
        po.setId(dto.getId());
        po.setTagName(dto.getTagName());
        po.setTagColor(dto.getTagColor());

        baseMapper.updateById(po);
    }

    @Override
    public void remove(Long id) {
        AssertUtils.notNull(id, "ID不能为空");
        KnowledgeTagPo exist = baseMapper.selectById(id);
        AssertUtils.notNull(exist, "标签不存在");

        //anchor 删除守卫：存在标签关联 → 拒绝
        Long refCount = knowledgeTagRelationMapper.selectCount(
                new LambdaQueryWrapper<KnowledgeTagRelationPo>()
                        .eq(KnowledgeTagRelationPo::getTagId, id));
        AssertUtils.isTrue(refCount == 0,
                "该标签仍被 " + refCount + " 条知识条目引用，不允许删除");

        baseMapper.deleteById(id);
    }

    //anchor ========== 私有辅助方法 ==========

    /**
     * 读时聚合 useCount：按 tag_id 分组，只统计 status=1 的知识条目
     */
    private Map<Long, Integer> loadUseCountMap() {
        //anchor 取全部启用知识条目的 ID 集合
        List<Long> activeKnowledgeIds = knowledgeMapper.selectList(
                new LambdaQueryWrapper<KnowledgePo>()
                        .eq(KnowledgePo::getStatus, 1)
                        .select(KnowledgePo::getId))
                .stream().map(KnowledgePo::getId).collect(Collectors.toList());
        if (activeKnowledgeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        //anchor 按 tag_id 分组计数
        QueryWrapper<KnowledgeTagRelationPo> qw = new QueryWrapper<>();
        qw.select("tag_id", "COUNT(*) AS cnt")
                .in("knowledge_id", activeKnowledgeIds)
                .groupBy("tag_id");
        List<Map<String, Object>> rows = knowledgeTagRelationMapper.selectMaps(qw);

        Map<Long, Integer> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Long tagId = ((Number) row.get("tag_id")).longValue();
            Integer cnt = ((Number) row.get("cnt")).intValue();
            result.put(tagId, cnt);
        }
        return result;
    }

    /**
     * 标签 PO → TagVo（含聚合 useCount）
     */
    private TagVo convertToVo(KnowledgeTagPo po, Map<Long, Integer> useCountMap) {
        TagVo vo = new TagVo();
        vo.setId(po.getId());
        vo.setTagName(po.getTagName());
        vo.setTagColor(po.getTagColor());
        vo.setUseCount(useCountMap.getOrDefault(po.getId(), 0));
        return vo;
    }

    /**
     * 校验标签颜色格式（#RGB 或 #RRGGBB）
     */
    private void validateColor(String color) {
        if (StrUtil.isBlank(color)) {
            return;
        }
        AssertUtils.isTrue(COLOR_PATTERN.matcher(color).matches(),
                "标签颜色格式不正确，应为 #RGB 或 #RRGGBB");
    }
}
