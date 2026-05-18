package io.aik.steins.grimoire.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.aik.steins.grimoire.system.common.dto.SystemParamDto;
import io.aik.steins.grimoire.system.common.dto.SystemParamQuery;
import io.aik.steins.grimoire.system.common.vo.SystemParamVo;

import java.util.List;

/**
 * 系统参数 Service -anchor
 *
 * @author a I k .
 */
public interface SystemParamService {

    /**
     * 分页查询
     */
    IPage<SystemParamVo> findPage(SystemParamQuery query);

    /**
     * 根据参数键查询
     */
    SystemParamVo findByKey(String paramKey);

    /**
     * 根据参数键获取参数值（从缓存读取）
     */
    String getParamValue(String paramKey);

    /**
     * 根据参数分组查询参数列表
     */
    List<SystemParamVo> findByGroup(String paramGroup);

    /**
     * 新增
     */
    void add(SystemParamDto dto);

    /**
     * 修改（含热更新）
     */
    void modify(SystemParamDto dto);

    /**
     * 删除
     */
    void remove(Long id);

    /**
     * 刷新参数缓存
     */
    void refreshCache();
}
