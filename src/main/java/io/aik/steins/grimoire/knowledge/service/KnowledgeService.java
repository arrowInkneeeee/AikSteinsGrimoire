package io.aik.steins.grimoire.knowledge.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.aik.steins.grimoire.knowledge.common.dto.KnowledgeDto;
import io.aik.steins.grimoire.knowledge.common.dto.KnowledgeQuery;
import io.aik.steins.grimoire.knowledge.common.po.KnowledgePo;
import io.aik.steins.grimoire.knowledge.common.vo.KnowledgeListVo;
import io.aik.steins.grimoire.knowledge.common.vo.KnowledgeVo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * -anchor 知识条目 Service
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since 2026/05/18
 * -
 */
public interface KnowledgeService extends IService<KnowledgePo> {

    /**
     * 新增知识条目
     *
     * @param dto 知识条目 DTO
     */
    void add(KnowledgeDto dto);

    /**
     * 修改知识条目
     *
     * @param dto 知识条目 DTO
     */
    void update(KnowledgeDto dto);

    /**
     * 删除知识条目
     *
     * @param id 主键ID
     */
    void delete(Long id);

    /**
     * 分页查询
     *
     * @param query 查询条件
     * @return 分页结果
     */
    IPage<KnowledgeListVo> findPage(KnowledgeQuery query);

    /**
     * 根据 ID 查询详情（含标签、附件）
     *
     * @param id 主键ID
     * @return 详情 VO
     */
    KnowledgeVo findById(Long id);

    /**
     * 切换状态
     *
     * @param id     主键ID
     * @param status 状态：1-启用 0-禁用
     */
    void toggleStatus(Long id, Integer status);
}
