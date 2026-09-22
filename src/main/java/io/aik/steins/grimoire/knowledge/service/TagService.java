package io.aik.steins.grimoire.knowledge.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.aik.steins.grimoire.knowledge.common.dto.TagDto;
import io.aik.steins.grimoire.knowledge.common.dto.TagQuery;
import io.aik.steins.grimoire.knowledge.common.vo.TagVo;

import java.util.List;

/**
 * -anchor 标签 Service
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since 2026/09/22
 * -
 */
public interface TagService {

    /**
     * 查询全部标签（含读时聚合 useCount）
     *
     * @return 标签列表
     */
    List<TagVo> findAll();

    /**
     * 分页查询标签
     *
     * @param query 查询条件
     * @return 分页结果
     */
    IPage<TagVo> findPage(TagQuery query);

    /**
     * 按 ID 查询标签
     *
     * @param id 标签ID
     * @return 标签 VO
     */
    TagVo findById(Long id);

    /**
     * 新增标签
     *
     * @param dto 标签 DTO
     */
    void add(TagDto dto);

    /**
     * 修改标签
     *
     * @param dto 标签 DTO
     */
    void modify(TagDto dto);

    /**
     * 删除标签（含删除守卫：被知识条目引用时拒绝）
     *
     * @param id 标签ID
     */
    void remove(Long id);
}
