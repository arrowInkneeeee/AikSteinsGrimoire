package io.aik.steins.grimoire.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.aik.steins.grimoire.system.common.dto.DictTypeDto;
import io.aik.steins.grimoire.system.common.dto.DictTypeQuery;
import io.aik.steins.grimoire.system.common.vo.DictTypeItemsVo;
import io.aik.steins.grimoire.system.common.vo.DictTypeVo;

/**
 * 字典类型 Service -anchor
 *
 * @author a I k .
 */
public interface DictTypeService {

    /**
     * 分页查询
     */
    IPage<DictTypeVo> findPage(DictTypeQuery query);

    /**
     * 根据 ID 查询
     */
    DictTypeVo findById(Long id);

    /**
     * 根据字典编码查询类型及启用项列表
     */
    DictTypeItemsVo findTypeWithItems(String dictCode);

    /**
     * 新增
     */
    void add(DictTypeDto dto);

    /**
     * 修改
     */
    void modify(DictTypeDto dto);

    /**
     * 删除
     */
    void remove(Long id);
}
