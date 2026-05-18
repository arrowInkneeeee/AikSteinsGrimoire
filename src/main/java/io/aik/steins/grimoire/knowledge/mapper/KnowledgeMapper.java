package io.aik.steins.grimoire.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.aik.steins.grimoire.knowledge.common.po.KnowledgePo;
import org.apache.ibatis.annotations.Mapper;

/**
 * -anchor 知识条目 Mapper
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since 2026/05/18
 * -
 */
@Mapper
public interface KnowledgeMapper extends BaseMapper<KnowledgePo> {
}
