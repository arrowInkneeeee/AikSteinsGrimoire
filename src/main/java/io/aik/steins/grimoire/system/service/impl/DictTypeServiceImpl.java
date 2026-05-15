package io.aik.steins.grimoire.system.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.aik.steins.grimoire.core.utils.AssertUtils;
import io.aik.steins.grimoire.system.common.dto.DictTypeDto;
import io.aik.steins.grimoire.system.common.dto.DictTypeQuery;
import io.aik.steins.grimoire.system.common.po.DictItemPo;
import io.aik.steins.grimoire.system.common.po.DictTypePo;
import io.aik.steins.grimoire.system.common.vo.DictTypeVo;
import io.aik.steins.grimoire.system.dao.DictItemMapper;
import io.aik.steins.grimoire.system.dao.DictTypeMapper;
import io.aik.steins.grimoire.system.service.DictTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional
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
    @Transactional
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
    @Transactional
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
