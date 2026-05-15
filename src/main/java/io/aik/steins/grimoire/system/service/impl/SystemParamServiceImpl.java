package io.aik.steins.grimoire.system.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.aik.steins.grimoire.core.utils.AssertUtils;
import io.aik.steins.grimoire.system.common.dto.SystemParamDto;
import io.aik.steins.grimoire.system.common.dto.SystemParamQuery;
import io.aik.steins.grimoire.system.common.po.SystemParamPo;
import io.aik.steins.grimoire.system.common.vo.SystemParamVo;
import io.aik.steins.grimoire.system.dao.SystemParamMapper;
import io.aik.steins.grimoire.system.service.SystemParamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统参数 Service 实现 -anchor
 *
 * @author a I k .
 */
@Slf4j
@Service("system.SystemParamService")
@RequiredArgsConstructor
public class SystemParamServiceImpl implements SystemParamService {

    private final SystemParamMapper systemParamMapper;

    //anchor 参数缓存，使用 ConcurrentHashMap 保证线程安全
    private final ConcurrentHashMap<String, String> paramCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void loadAllParams() {
        refreshCache();
        log.info("系统参数缓存加载完成，共 {} 条", paramCache.size());
    }

    @Override
    public IPage<SystemParamVo> findPage(SystemParamQuery query) {
        LambdaQueryWrapper<SystemParamPo> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(query.getParamKey()), SystemParamPo::getParamKey, query.getParamKey())
                .eq(StrUtil.isNotBlank(query.getParamGroup()), SystemParamPo::getParamGroup, query.getParamGroup())
                .orderByDesc(SystemParamPo::getCreateTime);

        return systemParamMapper.selectPage(query.toPage(), wrapper).convert(SystemParamVo::of);
    }

    @Override
    public SystemParamVo findByKey(String paramKey) {
        AssertUtils.notEmpty(paramKey, "参数键不能为空");
        SystemParamPo po = systemParamMapper.selectOne(
                new LambdaQueryWrapper<SystemParamPo>().eq(SystemParamPo::getParamKey, paramKey));
        return SystemParamVo.of(po);
    }

    @Override
    public String getParamValue(String paramKey) {
        AssertUtils.notEmpty(paramKey, "参数键不能为空");
        String value = paramCache.get(paramKey);
        if (value == null) {
            //anchor 缓存未命中，从数据库查询并回填缓存
            SystemParamPo po = systemParamMapper.selectOne(
                    new LambdaQueryWrapper<SystemParamPo>().eq(SystemParamPo::getParamKey, paramKey));
            if (po != null) {
                value = po.getParamValue();
                paramCache.put(paramKey, value);
            }
        }
        return value;
    }

    @Override
    @Transactional
    public void add(SystemParamDto dto) {
        //anchor 检查参数键是否已存在
        Long count = systemParamMapper.selectCount(
                new LambdaQueryWrapper<SystemParamPo>().eq(SystemParamPo::getParamKey, dto.getParamKey()));
        AssertUtils.isTrue(count == 0, "参数键已存在");

        SystemParamPo po = new SystemParamPo();
        po.setId(IdUtil.getSnowflakeNextId());
        po.setParamKey(dto.getParamKey());
        po.setParamValue(dto.getParamValue());
        po.setDescription(dto.getDescription());
        po.setParamGroup(dto.getParamGroup());
        po.setEditable(dto.getEditable());
        systemParamMapper.insert(po);

        //anchor 刷新缓存
        paramCache.put(po.getParamKey(), po.getParamValue());
    }

    @Override
    @Transactional
    public void modify(SystemParamDto dto) {
        AssertUtils.notNull(dto.getId(), "ID不能为空");
        SystemParamPo existing = systemParamMapper.selectById(dto.getId());
        AssertUtils.notNull(existing, "系统参数不存在");

        //anchor 检查是否可编辑
        AssertUtils.isTrue(existing.getEditable() != null && existing.getEditable() == 1, "该参数不可编辑");

        //anchor 如果修改了参数键，检查是否与其他记录重复
        if (!existing.getParamKey().equals(dto.getParamKey())) {
            Long count = systemParamMapper.selectCount(
                    new LambdaQueryWrapper<SystemParamPo>()
                            .eq(SystemParamPo::getParamKey, dto.getParamKey())
                            .ne(SystemParamPo::getId, dto.getId()));
            AssertUtils.isTrue(count == 0, "参数键已存在");
        }

        String oldKey = existing.getParamKey();
        existing.setParamKey(dto.getParamKey());
        existing.setParamValue(dto.getParamValue());
        existing.setDescription(dto.getDescription());
        existing.setParamGroup(dto.getParamGroup());
        existing.setEditable(dto.getEditable());
        systemParamMapper.updateById(existing);

        //anchor 刷新缓存，如果参数键变更需清理旧键
        if (!oldKey.equals(dto.getParamKey())) {
            paramCache.remove(oldKey);
        }
        paramCache.put(existing.getParamKey(), existing.getParamValue());
    }

    @Override
    @Transactional
    public void remove(Long id) {
        SystemParamPo po = systemParamMapper.selectById(id);
        AssertUtils.notNull(po, "系统参数不存在");
        systemParamMapper.deleteById(id);

        //anchor 刷新缓存
        paramCache.remove(po.getParamKey());
    }

    @Override
    public void refreshCache() {
        paramCache.clear();
        List<SystemParamPo> list = systemParamMapper.selectList(null);
        for (SystemParamPo po : list) {
            paramCache.put(po.getParamKey(), po.getParamValue());
        }
        log.info("系统参数缓存刷新完成，共 {} 条", paramCache.size());
    }
}
