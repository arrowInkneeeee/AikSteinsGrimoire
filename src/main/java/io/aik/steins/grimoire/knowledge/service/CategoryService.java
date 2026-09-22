package io.aik.steins.grimoire.knowledge.service;

import io.aik.steins.grimoire.knowledge.common.dto.CategoryDto;
import io.aik.steins.grimoire.knowledge.common.vo.CategoryTreeVo;
import io.aik.steins.grimoire.knowledge.common.vo.CategoryVo;

import java.util.List;

/**
 * -anchor 分类 Service
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since 2026/09/22
 * -
 */
public interface CategoryService {

    /**
     * 查询分类树（含 directCount / totalCount）
     *
     * @return 分类树根节点列表
     */
    List<CategoryTreeVo> findTree();

    /**
     * 按 ID 查询分类详情
     *
     * @param id 分类ID
     * @return 分类详情 VO
     */
    CategoryVo findById(Long id);

    /**
     * 新增分类
     *
     * @param dto 分类 DTO
     */
    void add(CategoryDto dto);

    /**
     * 修改分类
     *
     * @param dto 分类 DTO
     */
    void modify(CategoryDto dto);

    /**
     * 删除分类（含删除守卫：有子分类或被知识条目引用时拒绝）
     *
     * @param id 分类ID
     */
    void remove(Long id);
}
