package io.aik.steins.grimoire.knowledge.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.aik.steins.grimoire.knowledge.common.po.KnowledgeTagPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 标签 Mapper -anchor
 *
 * @author a I k .
 */
@Mapper
public interface KnowledgeTagMapper extends BaseMapper<KnowledgeTagPo> {
}
