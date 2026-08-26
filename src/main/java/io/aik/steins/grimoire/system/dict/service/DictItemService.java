package io.aik.steins.grimoire.system.dict.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.aik.steins.grimoire.system.dict.dto.DictItemDto;
import io.aik.steins.grimoire.system.dict.dto.DictItemQuery;
import io.aik.steins.grimoire.system.dict.vo.DictItemVo;

import java.util.List;
import java.util.Map;

/**
 * 字典项 Service -anchor
 *
 * @author a I k .
 */
public interface DictItemService {

    /**
     * 根据字典类型编码查询启用项列表
     */
    List<DictItemVo> findListByType(String dictCode);

    /**
     * 分页查询
     */
    IPage<DictItemVo> findPage(DictItemQuery query);

    /**
     * 新增
     */
    void add(DictItemDto dto);

    /**
     * 修改
     */
    void modify(DictItemDto dto);

    /**
     * 删除
     */
    void remove(Long id);

    /**
     * 根据多个字典类型编码批量查询字典项
     */
    Map<String, List<DictItemVo>> findMapByTypes(List<String> dictCodes);
}
