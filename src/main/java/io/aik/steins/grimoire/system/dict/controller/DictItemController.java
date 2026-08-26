package io.aik.steins.grimoire.system.dict.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.aik.steins.grimoire.core.dto.ApiResponse;
import io.aik.steins.grimoire.core.dto.IdDto;
import io.aik.steins.grimoire.system.dict.dto.DictCodesDto;
import io.aik.steins.grimoire.system.dict.dto.DictItemDto;
import io.aik.steins.grimoire.system.dict.dto.DictItemQuery;
import io.aik.steins.grimoire.system.dict.vo.DictItemVo;
import io.aik.steins.grimoire.system.dict.service.DictItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 字典项 Controller -anchor
 *
 * @author a I k .
 */
@Slf4j
@RestController
@RequestMapping("/grimoire/dictItem")
@RequiredArgsConstructor
@Tag(name = "字典项管理")
public class DictItemController {

    private final DictItemService dictItemService;

    @GetMapping("/findListByType")
    @Operation(summary = "根据字典类型编码查询启用项列表")
    public ApiResponse<List<DictItemVo>> findListByType(@RequestParam String dictCode) {
        return ApiResponse.success(dictItemService.findListByType(dictCode));
    }

    @PostMapping("/findMapByTypes")
    @Operation(summary = "根据多个字典类型编码批量查询字典项")
    public ApiResponse<Map<String, List<DictItemVo>>> findMapByTypes(@RequestBody @Validated DictCodesDto dto) {
        return ApiResponse.success(dictItemService.findMapByTypes(dto.getDictCodes()));
    }

    @PostMapping("/findPage")
    @Operation(summary = "分页查询字典项")
    public ApiResponse<IPage<DictItemVo>> findPage(@RequestBody DictItemQuery query) {
        return ApiResponse.success(dictItemService.findPage(query));
    }

    @PostMapping("/add")
    @Operation(summary = "新增字典项")
    public ApiResponse<Void> add(@RequestBody @Validated DictItemDto dto) {
        dictItemService.add(dto);
        return ApiResponse.success();
    }

    @PostMapping("/modify")
    @Operation(summary = "修改字典项")
    public ApiResponse<Void> modify(@RequestBody @Validated DictItemDto dto) {
        dictItemService.modify(dto);
        return ApiResponse.success();
    }

    @PostMapping("/remove")
    @Operation(summary = "删除字典项")
    public ApiResponse<Void> remove(@RequestBody @Validated IdDto idDto) {
        dictItemService.remove(idDto.getId());
        return ApiResponse.success();
    }
}
