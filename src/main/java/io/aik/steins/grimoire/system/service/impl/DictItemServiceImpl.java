package io.aik.steins.grimoire.system.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.aik.steins.grimoire.core.utils.AssertUtils;
import io.aik.steins.grimoire.system.common.constant.SystemConstant;
import io.aik.steins.grimoire.system.common.dto.DictItemDto;
import io.aik.steins.grimoire.system.common.dto.DictItemQuery;
import io.aik.steins.grimoire.system.common.po.DictItemPo;
import io.aik.steins.grimoire.system.common.vo.DictItemVo;
import io.aik.steins.grimoire.system.dao.DictItemMapper;
import io.aik.steins.grimoire.system.service.DictItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

    @Override
    public List<DictItemVo> findListByType(String dictCode) {
        AssertUtils.notEmpty(dictCode, "字典类型编码不能为空");
        List<DictItemPo> list = dictItemMapper.selectList(
                new LambdaQueryWrapper<DictItemPo>()
                        .eq(DictItemPo::getDictCode, dictCode)
                        .eq(DictItemPo::getStatus, SystemConstant.STATUS_ENABLE)
                        .orderByAsc(DictItemPo::getSortOrder));
        return list.stream().map(DictItemVo::of).collect(Collectors.toList());
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
    @Transactional
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
    }

    @Override
    @Transactional
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
    }

    @Override
    @Transactional
    public void remove(Long id) {
        DictItemPo po = dictItemMapper.selectById(id);
        AssertUtils.notNull(po, "字典项不存在");
        dictItemMapper.deleteById(id);
    }
}
