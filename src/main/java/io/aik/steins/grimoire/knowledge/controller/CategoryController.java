package io.aik.steins.grimoire.knowledge.controller;

import io.aik.steins.grimoire.core.dto.ApiResponse;
import io.aik.steins.grimoire.core.dto.IdDto;
import io.aik.steins.grimoire.knowledge.common.dto.CategoryDto;
import io.aik.steins.grimoire.knowledge.common.vo.CategoryTreeVo;
import io.aik.steins.grimoire.knowledge.common.vo.CategoryVo;
import io.aik.steins.grimoire.knowledge.service.CategoryService;
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

/**
 * -anchor 分类管理
 *
 * <p>提供分类树的查询、新增、修改、删除</p>
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since 2026/09/22
 * -
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/grimoire/category")
@Tag(name = "分类管理")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/findTree")
    @Operation(summary = "查询分类树")
    public ApiResponse<List<CategoryTreeVo>> findTree() {
        return ApiResponse.success(categoryService.findTree());
    }

    @GetMapping("/findById")
    @Operation(summary = "查询分类详情")
    public ApiResponse<CategoryVo> findById(@RequestParam Long id) {
        return ApiResponse.success(categoryService.findById(id));
    }

    @PostMapping("/add")
    @Operation(summary = "新增分类")
    public ApiResponse<Void> add(@RequestBody @Validated CategoryDto dto) {
        categoryService.add(dto);
        return ApiResponse.success();
    }

    @PostMapping("/modify")
    @Operation(summary = "修改分类")
    public ApiResponse<Void> modify(@RequestBody @Validated CategoryDto dto) {
        categoryService.modify(dto);
        return ApiResponse.success();
    }

    @PostMapping("/remove")
    @Operation(summary = "删除分类")
    public ApiResponse<Void> remove(@RequestBody @Validated IdDto idDto) {
        categoryService.remove(idDto.getId());
        return ApiResponse.success();
    }
}
