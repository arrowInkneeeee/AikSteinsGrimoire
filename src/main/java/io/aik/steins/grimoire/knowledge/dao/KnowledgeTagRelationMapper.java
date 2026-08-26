package io.aik.steins.grimoire.knowledge.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.aik.steins.grimoire.knowledge.common.po.KnowledgeTagRelationPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 标签关联 Mapper -anchor
 *
 * @author a I k .
 */
@Mapper
public interface KnowledgeTagRelationMapper extends BaseMapper<KnowledgeTagRelationPo> {
}
