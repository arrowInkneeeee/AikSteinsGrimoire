package io.aik.steins.grimoire.knowledge.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.aik.steins.grimoire.core.exception.BusinessException;
import io.aik.steins.grimoire.core.utils.AssertUtils;
import io.aik.steins.grimoire.knowledge.common.constant.KnowledgeConstant;
import io.aik.steins.grimoire.knowledge.common.dto.CategoryDto;
import io.aik.steins.grimoire.knowledge.common.po.KnowledgeCategoryPo;
import io.aik.steins.grimoire.knowledge.common.po.KnowledgePo;
import io.aik.steins.grimoire.knowledge.common.vo.CategoryTreeVo;
import io.aik.steins.grimoire.knowledge.common.vo.CategoryVo;
import io.aik.steins.grimoire.knowledge.dao.KnowledgeCategoryMapper;
import io.aik.steins.grimoire.knowledge.dao.KnowledgeMapper;
import io.aik.steins.grimoire.knowledge.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * -anchor 分类 Service 实现
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
public class CategoryServiceImpl extends ServiceImpl<KnowledgeCategoryMapper, KnowledgeCategoryPo>
        implements CategoryService {

    private final KnowledgeMapper knowledgeMapper;

    //anchor ========== 查询 ==========

    @Override
    public List<CategoryTreeVo> findTree() {
        //anchor 一次加载全部分类（表极小）
        List<KnowledgeCategoryPo> allCategories = baseMapper.selectList(
                new LambdaQueryWrapper<KnowledgeCategoryPo>()
                        .orderByAsc(KnowledgeCategoryPo::getSortOrder));

        //anchor 按分类聚合启用条目数（GROUP BY category_id）
        Map<Long, Integer> directCountMap = loadDirectCountMap();

        //anchor 转为 TreeVo
        Map<Long, CategoryTreeVo> voMap = new HashMap<>();
        for (KnowledgeCategoryPo cat : allCategories) {
            CategoryTreeVo vo = new CategoryTreeVo();
            vo.setId(cat.getId());
            vo.setParentId(cat.getParentId());
            vo.setCategoryName(cat.getCategoryName());
            vo.setCategoryCode(cat.getCategoryCode());
            vo.setSortOrder(cat.getSortOrder());
            vo.setStatus(cat.getStatus());
            vo.setDirectCount(directCountMap.getOrDefault(cat.getId(), 0));
            vo.setTotalCount(0);
            vo.setChildren(new ArrayList<>());
            voMap.put(cat.getId(), vo);
        }

        //anchor 构建树形 + 自底向上上卷 totalCount
        List<CategoryTreeVo> roots = buildTreeAndRollup(allCategories, voMap);
        return roots;
    }

    @Override
    public CategoryVo findById(Long id) {
        AssertUtils.notNull(id, "ID不能为空");
        KnowledgeCategoryPo po = baseMapper.selectById(id);
        AssertUtils.notNull(po, "分类不存在");

        CategoryVo vo = new CategoryVo();
        BeanUtil.copyProperties(po, vo);
        return vo;
    }

    //anchor ========== 写入 ==========

    @Override
    public void add(CategoryDto dto) {
        AssertUtils.notEmpty(dto.getCategoryName(), "分类名称不能为空");
        AssertUtils.isTrue(dto.getCategoryName().length() <= 128, "分类名称不能超过128个字符");

        //anchor categoryCode 全局唯一校验（Service 层，DDL 无唯一索引）
        if (StrUtil.isNotBlank(dto.getCategoryCode())) {
            Long count = baseMapper.selectCount(
                    new LambdaQueryWrapper<KnowledgeCategoryPo>()
                            .eq(KnowledgeCategoryPo::getCategoryCode, dto.getCategoryCode()));
            AssertUtils.isTrue(count == 0, "分类编码已存在：" + dto.getCategoryCode());
        }

        //anchor parentId 校验
        Long parentId = dto.getParentId() != null ? dto.getParentId() : KnowledgeConstant.CATEGORY_ROOT_ID;
        if (!KnowledgeConstant.CATEGORY_ROOT_ID.equals(parentId)) {
            KnowledgeCategoryPo parent = baseMapper.selectById(parentId);
            AssertUtils.notNull(parent, "父分类不存在");
        }

        KnowledgeCategoryPo po = new KnowledgeCategoryPo();
        po.setId(IdUtil.getSnowflakeNextId());
        po.setCategoryName(dto.getCategoryName());
        po.setCategoryCode(dto.getCategoryCode());
        po.setParentId(parentId);
        po.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        po.setStatus(dto.getStatus() != null ? dto.getStatus() : KnowledgeConstant.STATUS_ENABLE);

        baseMapper.insert(po);
    }

    @Override
    public void modify(CategoryDto dto) {
        AssertUtils.notNull(dto.getId(), "ID不能为空");
        KnowledgeCategoryPo exist = baseMapper.selectById(dto.getId());
        AssertUtils.notNull(exist, "分类不存在");

        AssertUtils.notEmpty(dto.getCategoryName(), "分类名称不能为空");
        AssertUtils.isTrue(dto.getCategoryName().length() <= 128, "分类名称不能超过128个字符");

        //anchor categoryCode 唯一校验（排除自身）
        if (StrUtil.isNotBlank(dto.getCategoryCode())) {
            Long count = baseMapper.selectCount(
                    new LambdaQueryWrapper<KnowledgeCategoryPo>()
                            .eq(KnowledgeCategoryPo::getCategoryCode, dto.getCategoryCode())
                            .ne(KnowledgeCategoryPo::getId, dto.getId()));
            AssertUtils.isTrue(count == 0, "分类编码已存在：" + dto.getCategoryCode());
        }

        //anchor parentId 校验 + 成环检测
        Long parentId = dto.getParentId() != null ? dto.getParentId() : exist.getParentId();
        if (!KnowledgeConstant.CATEGORY_ROOT_ID.equals(parentId)) {
            KnowledgeCategoryPo parent = baseMapper.selectById(parentId);
            AssertUtils.notNull(parent, "父分类不存在");
        }
        //anchor 不得指向自身或自身子孙（成环检测）
        AssertUtils.isTrue(!dto.getId().equals(parentId), "父分类不得指向自身");
        Map<Long, KnowledgeCategoryPo> allCats = loadAllCategoryMap();
        List<Long> descendantIds = collectDescendantIds(dto.getId(), allCats);
        AssertUtils.isTrue(!descendantIds.contains(parentId),
                "父分类不得指向自身的子孙分类");

        KnowledgeCategoryPo po = new KnowledgeCategoryPo();
        po.setId(dto.getId());
        po.setCategoryName(dto.getCategoryName());
        po.setCategoryCode(dto.getCategoryCode());
        po.setParentId(parentId);
        po.setSortOrder(dto.getSortOrder());
        po.setStatus(dto.getStatus());

        baseMapper.updateById(po);
    }

    @Override
    public void remove(Long id) {
        AssertUtils.notNull(id, "ID不能为空");
        KnowledgeCategoryPo exist = baseMapper.selectById(id);
        AssertUtils.notNull(exist, "分类不存在");

        //anchor 删除守卫 ①：存在子分类 → 拒绝
        Long childCount = baseMapper.selectCount(
                new LambdaQueryWrapper<KnowledgeCategoryPo>()
                        .eq(KnowledgeCategoryPo::getParentId, id));
        AssertUtils.isTrue(childCount == 0, "请先删除子分类");

        //anchor 删除守卫 ②：存在知识条目引用 → 拒绝
        Long knowledgeCount = knowledgeMapper.selectCount(
                new LambdaQueryWrapper<KnowledgePo>()
                        .eq(KnowledgePo::getCategoryId, id));
        AssertUtils.isTrue(knowledgeCount == 0,
                "该分类下仍有 " + knowledgeCount + " 条知识条目，不允许删除");

        baseMapper.deleteById(id);
    }

    //anchor ========== 私有辅助方法 ==========

    /**
     * 按分类聚合启用条目数（GROUP BY category_id WHERE status = 1）
     */
    private Map<Long, Integer> loadDirectCountMap() {
        QueryWrapper<KnowledgePo> qw = new QueryWrapper<>();
        qw.select("category_id", "COUNT(*) AS cnt")
                .eq("status", 1)
                .isNotNull("category_id")
                .groupBy("category_id");
        List<Map<String, Object>> rows = knowledgeMapper.selectMaps(qw);

        Map<Long, Integer> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Long catId = ((Number) row.get("category_id")).longValue();
            Integer cnt = ((Number) row.get("cnt")).intValue();
            result.put(catId, cnt);
        }
        return result;
    }

    /**
     * 构建分类树并自底向上上卷 totalCount
     */
    private List<CategoryTreeVo> buildTreeAndRollup(
            List<KnowledgeCategoryPo> allCategories,
            Map<Long, CategoryTreeVo> voMap) {
        //anchor 按 parentId 分组
        Map<Long, List<KnowledgeCategoryPo>> childrenMap = allCategories.stream()
                .collect(Collectors.groupingBy(KnowledgeCategoryPo::getParentId));

        //anchor 找到根节点（parentId = 0）
        List<KnowledgeCategoryPo> roots = childrenMap.getOrDefault(
                KnowledgeConstant.CATEGORY_ROOT_ID, Collections.emptyList());

        //anchor 排序
        roots.sort(Comparator.comparingInt(c -> c.getSortOrder() != null ? c.getSortOrder() : 0));

        //anchor 递归构建树 + 上卷
        List<CategoryTreeVo> result = new ArrayList<>();
        for (KnowledgeCategoryPo root : roots) {
            CategoryTreeVo vo = voMap.get(root.getId());
            buildChildren(vo, childrenMap, voMap);
            vo.setTotalCount(rollupTotalCount(vo.getId(), childrenMap, voMap));
            result.add(vo);
        }
        return result;
    }

    /**
     * 递归设置子节点
     */
    private void buildChildren(CategoryTreeVo parent,
            Map<Long, List<KnowledgeCategoryPo>> childrenMap,
            Map<Long, CategoryTreeVo> voMap) {
        List<KnowledgeCategoryPo> children = childrenMap.getOrDefault(
                parent.getId(), Collections.emptyList());
        children.sort(Comparator.comparingInt(c -> c.getSortOrder() != null ? c.getSortOrder() : 0));

        List<CategoryTreeVo> childVos = new ArrayList<>();
        for (KnowledgeCategoryPo child : children) {
            CategoryTreeVo childVo = voMap.get(child.getId());
            buildChildren(childVo, childrenMap, voMap);
            childVos.add(childVo);
        }
        parent.setChildren(childVos);
    }

    /**
     * 自底向上上卷 totalCount = 自身 directCount + 所有子孙的 totalCount
     */
    private int rollupTotalCount(Long categoryId,
            Map<Long, List<KnowledgeCategoryPo>> childrenMap,
            Map<Long, CategoryTreeVo> voMap) {
        CategoryTreeVo vo = voMap.get(categoryId);
        int total = vo.getDirectCount() != null ? vo.getDirectCount() : 0;
        List<KnowledgeCategoryPo> children = childrenMap.getOrDefault(categoryId, Collections.emptyList());
        for (KnowledgeCategoryPo child : children) {
            total += rollupTotalCount(child.getId(), childrenMap, voMap);
        }
        vo.setTotalCount(total);
        return total;
    }

    /**
     * 加载全部分类到 Map
     */
    private Map<Long, KnowledgeCategoryPo> loadAllCategoryMap() {
        return baseMapper.selectList(null).stream()
                .collect(Collectors.toMap(KnowledgeCategoryPo::getId, c -> c));
    }

    /**
     * 递归收集子孙 ID（不含自身）
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
}
