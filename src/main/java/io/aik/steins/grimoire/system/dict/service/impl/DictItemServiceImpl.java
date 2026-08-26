package io.aik.steins.grimoire.system.dict.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.aik.steins.grimoire.core.utils.AssertUtils;
import io.aik.steins.grimoire.system.common.constant.SystemConstant;
import io.aik.steins.grimoire.system.dict.dto.DictItemDto;
import io.aik.steins.grimoire.system.dict.dto.DictItemQuery;
import io.aik.steins.grimoire.system.dict.po.DictItemPo;
import io.aik.steins.grimoire.system.dict.vo.DictItemVo;
import io.aik.steins.grimoire.system.dict.dao.DictItemMapper;
import io.aik.steins.grimoire.system.dict.service.DictItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 字典项 Service 实现 -anchor
 *
 * @author a I k .
 */
@Slf4j
@Service("system.DictItemService")
@RequiredArgsConstructor
public class DictItemServiceImpl implements DictItemService {

    private final DictItemMapper dictItemMapper;

    //anchor 字典项本地缓存，key: dictCode
    private final ConcurrentHashMap<String, List<DictItemVo>> dictItemCache = new ConcurrentHashMap<>();

    @Override
    public List<DictItemVo> findListByType(String dictCode) {
        AssertUtils.notEmpty(dictCode, "字典类型编码不能为空");
        List<DictItemVo> cached = dictItemCache.get(dictCode);
        if (cached != null) {
            return cached;
        }
        List<DictItemPo> list = dictItemMapper.selectList(
                new LambdaQueryWrapper<DictItemPo>()
                        .eq(DictItemPo::getDictCode, dictCode)
                        .eq(DictItemPo::getStatus, SystemConstant.STATUS_ENABLE)
                        .orderByAsc(DictItemPo::getSortOrder));
        List<DictItemVo> result = list.stream().map(DictItemVo::of).collect(Collectors.toList());
        dictItemCache.put(dictCode, result);
        return result;
    }

    @Override
    public IPage<DictItemVo> findPage(DictItemQuery query) {
        LambdaQueryWrapper<DictItemPo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StrUtil.isNotBlank(query.getDictCode()), DictItemPo::getDictCode, query.getDictCode())
                .like(StrUtil.isNotBlank(query.getItemCode()), DictItemPo::getItemCode, query.getItemCode())
                .like(StrUtil.isNotBlank(query.getItemName()), DictItemPo::getItemName, query.getItemName())
                .eq(query.getStatus() != null, DictItemPo::getStatus, query.getStatus())
                .orderByAsc(DictItemPo::getSortOrder)
                .orderByDesc(DictItemPo::getCreateTime);

        return dictItemMapper.selectPage(query.toPage(), wrapper).convert(DictItemVo::of);
    }

    @Override
    public Map<String, List<DictItemVo>> findMapByTypes(List<String> dictCodes) {
        AssertUtils.notEmpty(dictCodes, "字典类型编码列表不能为空");
        Map<String, List<DictItemVo>> result = new HashMap<>();
        for (String dictCode : dictCodes) {
            result.put(dictCode, findListByType(dictCode));
        }
        return result;
    }

    @Override
    public void add(DictItemDto dto) {
        //anchor 检查同类型下字典项编码是否重复
        Long count = dictItemMapper.selectCount(
                new LambdaQueryWrapper<DictItemPo>()
                        .eq(DictItemPo::getDictCode, dto.getDictCode())
                        .eq(DictItemPo::getItemCode, dto.getItemCode()));
        AssertUtils.isTrue(count == 0, "该字典类型下字典项编码已存在");

        DictItemPo po = new DictItemPo();
        po.setId(IdUtil.getSnowflakeNextId());
        po.setDictCode(dto.getDictCode());
        po.setItemCode(dto.getItemCode());
        po.setItemName(dto.getItemName());
        po.setSortOrder(dto.getSortOrder());
        po.setStatus(dto.getStatus());
        po.setRemark(dto.getRemark());
        dictItemMapper.insert(po);

        //anchor 刷新缓存
        clearDictItemCache(dto.getDictCode());
    }

    @Override
    public void modify(DictItemDto dto) {
        AssertUtils.notNull(dto.getId(), "ID不能为空");
        DictItemPo existing = dictItemMapper.selectById(dto.getId());
        AssertUtils.notNull(existing, "字典项不存在");

        //anchor 如果修改了类型编码或项编码，检查是否与其他记录重复
        if (!existing.getDictCode().equals(dto.getDictCode()) || !existing.getItemCode().equals(dto.getItemCode())) {
            Long count = dictItemMapper.selectCount(
                    new LambdaQueryWrapper<DictItemPo>()
                            .eq(DictItemPo::getDictCode, dto.getDictCode())
                            .eq(DictItemPo::getItemCode, dto.getItemCode())
                            .ne(DictItemPo::getId, dto.getId()));
            AssertUtils.isTrue(count == 0, "该字典类型下字典项编码已存在");
        }

        existing.setDictCode(dto.getDictCode());
        existing.setItemCode(dto.getItemCode());
        existing.setItemName(dto.getItemName());
        existing.setSortOrder(dto.getSortOrder());
        existing.setStatus(dto.getStatus());
        existing.setRemark(dto.getRemark());
        dictItemMapper.updateById(existing);

        //anchor 刷新缓存
        clearDictItemCache(existing.getDictCode());
        if (!existing.getDictCode().equals(dto.getDictCode())) {
            clearDictItemCache(dto.getDictCode());
        }
    }

    @Override
    public void remove(Long id) {
        DictItemPo po = dictItemMapper.selectById(id);
        AssertUtils.notNull(po, "字典项不存在");
        dictItemMapper.deleteById(id);

        //anchor 刷新缓存
        clearDictItemCache(po.getDictCode());
    }

    /**
     * 清除字典项缓存
     */
    private void clearDictItemCache(String dictCode) {
        if (StrUtil.isNotBlank(dictCode)) {
            dictItemCache.remove(dictCode);
        }
    }
}
