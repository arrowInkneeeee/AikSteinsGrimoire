package io.aik.steins.grimoire.knowledge.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.aik.steins.grimoire.core.dto.ApiResponse;
import io.aik.steins.grimoire.core.dto.IdDto;
import io.aik.steins.grimoire.knowledge.common.dto.TagDto;
import io.aik.steins.grimoire.knowledge.common.dto.TagQuery;
import io.aik.steins.grimoire.knowledge.common.vo.TagVo;
import io.aik.steins.grimoire.knowledge.service.TagService;
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
 * -anchor 标签管理
 *
 * <p>提供标签的全量查询、分页、新增、修改、删除</p>
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
@RequestMapping("/grimoire/tag")
@Tag(name = "标签管理")
public class TagController {

    private final TagService tagService;

    @GetMapping("/findAll")
    @Operation(summary = "查询全部标签")
    public ApiResponse<List<TagVo>> findAll() {
        return ApiResponse.success(tagService.findAll());
    }

    @PostMapping("/findPage")
    @Operation(summary = "分页查询标签")
    public ApiResponse<IPage<TagVo>> findPage(@RequestBody TagQuery query) {
        return ApiResponse.success(tagService.findPage(query));
    }

    @GetMapping("/findById")
    @Operation(summary = "查询标签详情")
    public ApiResponse<TagVo> findById(@RequestParam Long id) {
        return ApiResponse.success(tagService.findById(id));
    }

    @PostMapping("/add")
    @Operation(summary = "新增标签")
    public ApiResponse<Void> add(@RequestBody @Validated TagDto dto) {
        tagService.add(dto);
        return ApiResponse.success();
    }

    @PostMapping("/modify")
    @Operation(summary = "修改标签")
    public ApiResponse<Void> modify(@RequestBody @Validated TagDto dto) {
        tagService.modify(dto);
        return ApiResponse.success();
    }

    @PostMapping("/remove")
    @Operation(summary = "删除标签")
    public ApiResponse<Void> remove(@RequestBody @Validated IdDto idDto) {
        tagService.remove(idDto.getId());
        return ApiResponse.success();
    }
}
