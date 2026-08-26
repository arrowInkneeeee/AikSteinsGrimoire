package io.aik.steins.grimoire.knowledge.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.aik.steins.grimoire.knowledge.common.po.KnowledgeCategoryPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 分类 Mapper -anchor
 *
 * @author a I k .
 */
@Mapper
public interface KnowledgeCategoryMapper extends BaseMapper<KnowledgeCategoryPo> {
}
