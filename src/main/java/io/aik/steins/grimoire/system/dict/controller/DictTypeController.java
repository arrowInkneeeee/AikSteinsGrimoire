package io.aik.steins.grimoire.system.dict.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.aik.steins.grimoire.core.dto.ApiResponse;
import io.aik.steins.grimoire.core.dto.IdDto;
import io.aik.steins.grimoire.system.dict.dto.DictTypeDto;
import io.aik.steins.grimoire.system.dict.dto.DictTypeQuery;
import io.aik.steins.grimoire.system.dict.vo.DictTypeItemsVo;
import io.aik.steins.grimoire.system.dict.vo.DictTypeVo;
import io.aik.steins.grimoire.system.dict.service.DictTypeService;
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

/**
 * 字典类型 Controller -anchor
 *
 * @author a I k .
 */
@Slf4j
@RestController
@RequestMapping("/grimoire/dictType")
@RequiredArgsConstructor
@Tag(name = "字典类型管理")
public class DictTypeController {

    private final DictTypeService dictTypeService;

    @PostMapping("/findPage")
    @Operation(summary = "分页查询字典类型")
    public ApiResponse<IPage<DictTypeVo>> findPage(@RequestBody DictTypeQuery query) {
        return ApiResponse.success(dictTypeService.findPage(query));
    }

    @GetMapping("/findById")
    @Operation(summary = "根据 ID 查询字典类型")
    public ApiResponse<DictTypeVo> findById(@RequestParam Long id) {
        return ApiResponse.success(dictTypeService.findById(id));
    }

    @GetMapping("/findTypeWithItems")
    @Operation(summary = "根据字典编码查询类型及字典项列表")
    public ApiResponse<DictTypeItemsVo> findTypeWithItems(@RequestParam String dictCode) {
        return ApiResponse.success(dictTypeService.findTypeWithItems(dictCode));
    }

    @PostMapping("/add")
    @Operation(summary = "新增字典类型")
    public ApiResponse<Void> add(@RequestBody @Validated DictTypeDto dto) {
        dictTypeService.add(dto);
        return ApiResponse.success();
    }

    @PostMapping("/modify")
    @Operation(summary = "修改字典类型")
    public ApiResponse<Void> modify(@RequestBody @Validated DictTypeDto dto) {
        dictTypeService.modify(dto);
        return ApiResponse.success();
    }

    @PostMapping("/remove")
    @Operation(summary = "删除字典类型")
    public ApiResponse<Void> remove(@RequestBody @Validated IdDto idDto) {
        dictTypeService.remove(idDto.getId());
        return ApiResponse.success();
    }
}
