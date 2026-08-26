package io.aik.steins.grimoire.system.dict.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.aik.steins.grimoire.core.utils.AssertUtils;
import io.aik.steins.grimoire.system.common.constant.SystemConstant;
import io.aik.steins.grimoire.system.dict.dto.DictTypeDto;
import io.aik.steins.grimoire.system.dict.dto.DictTypeQuery;
import io.aik.steins.grimoire.system.dict.po.DictItemPo;
import io.aik.steins.grimoire.system.dict.po.DictTypePo;
import io.aik.steins.grimoire.system.dict.vo.DictItemVo;
import io.aik.steins.grimoire.system.dict.vo.DictTypeItemsVo;
import io.aik.steins.grimoire.system.dict.vo.DictTypeVo;
import io.aik.steins.grimoire.system.dict.dao.DictItemMapper;
import io.aik.steins.grimoire.system.dict.dao.DictTypeMapper;
import io.aik.steins.grimoire.system.dict.service.DictTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 字典类型 Service 实现 -anchor
 *
 * @author a I k .
 */
@Slf4j
@Service("system.DictTypeService")
@RequiredArgsConstructor
public class DictTypeServiceImpl implements DictTypeService {

    private final DictTypeMapper dictTypeMapper;
    private final DictItemMapper dictItemMapper;

    @Override
    public IPage<DictTypeVo> findPage(DictTypeQuery query) {
        LambdaQueryWrapper<DictTypePo> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(query.getDictCode()), DictTypePo::getDictCode, query.getDictCode())
                .like(StrUtil.isNotBlank(query.getDictName()), DictTypePo::getDictName, query.getDictName())
                .eq(query.getStatus() != null, DictTypePo::getStatus, query.getStatus())
                .orderByDesc(DictTypePo::getCreateTime);

        return dictTypeMapper.selectPage(query.toPage(), wrapper).convert(DictTypeVo::of);
    }

    @Override
    public DictTypeVo findById(Long id) {
        DictTypePo po = dictTypeMapper.selectById(id);
        return DictTypeVo.of(po);
    }

    @Override
    public DictTypeItemsVo findTypeWithItems(String dictCode) {
        AssertUtils.notEmpty(dictCode, "字典类型编码不能为空");
        DictTypePo typePo = dictTypeMapper.selectOne(
                new LambdaQueryWrapper<DictTypePo>()
                        .eq(DictTypePo::getDictCode, dictCode));
        AssertUtils.notNull(typePo, "字典类型不存在");

        List<DictItemPo> itemPos = dictItemMapper.selectList(
                new LambdaQueryWrapper<DictItemPo>()
                        .eq(DictItemPo::getDictCode, dictCode)
                        .eq(DictItemPo::getStatus, SystemConstant.STATUS_ENABLE)
                        .orderByAsc(DictItemPo::getSortOrder));

        DictTypeItemsVo vo = new DictTypeItemsVo();
        vo.setDictCode(typePo.getDictCode());
        vo.setDictName(typePo.getDictName());
        vo.setItems(itemPos.stream().map(DictItemVo::of).collect(Collectors.toList()));
        return vo;
    }

    @Override
    public void add(DictTypeDto dto) {
        //anchor 检查字典编码是否已存在
        Long count = dictTypeMapper.selectCount(
                new LambdaQueryWrapper<DictTypePo>().eq(DictTypePo::getDictCode, dto.getDictCode()));
        AssertUtils.isTrue(count == 0, "字典编码已存在");

        DictTypePo po = new DictTypePo();
        po.setId(IdUtil.getSnowflakeNextId());
        po.setDictCode(dto.getDictCode());
        po.setDictName(dto.getDictName());
        po.setDescription(dto.getDescription());
        po.setStatus(dto.getStatus());
        dictTypeMapper.insert(po);
    }

    @Override
    public void modify(DictTypeDto dto) {
        AssertUtils.notNull(dto.getId(), "ID不能为空");
        DictTypePo existing = dictTypeMapper.selectById(dto.getId());
        AssertUtils.notNull(existing, "字典类型不存在");

        //anchor 如果修改了编码，检查新编码是否与其他记录重复
        if (!existing.getDictCode().equals(dto.getDictCode())) {
            Long count = dictTypeMapper.selectCount(
                    new LambdaQueryWrapper<DictTypePo>()
                            .eq(DictTypePo::getDictCode, dto.getDictCode())
                            .ne(DictTypePo::getId, dto.getId()));
            AssertUtils.isTrue(count == 0, "字典编码已存在");
        }

        existing.setDictCode(dto.getDictCode());
        existing.setDictName(dto.getDictName());
        existing.setDescription(dto.getDescription());
        existing.setStatus(dto.getStatus());
        dictTypeMapper.updateById(existing);
    }

    @Override
    public void remove(Long id) {
        DictTypePo po = dictTypeMapper.selectById(id);
        AssertUtils.notNull(po, "字典类型不存在");

        //anchor 检查该字典类型下是否存在字典项
        Long itemCount = dictItemMapper.selectCount(
                new LambdaQueryWrapper<DictItemPo>().eq(DictItemPo::getDictCode, po.getDictCode()));
        AssertUtils.isTrue(itemCount == 0, "该字典类型下存在字典项，不允许删除");

        dictTypeMapper.deleteById(id);
    }
}
