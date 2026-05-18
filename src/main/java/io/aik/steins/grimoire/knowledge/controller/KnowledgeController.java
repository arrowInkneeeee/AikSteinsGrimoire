package io.aik.steins.grimoire.knowledge.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.aik.steins.grimoire.core.dto.ApiResponse;
import io.aik.steins.grimoire.core.dto.IdDto;
import io.aik.steins.grimoire.knowledge.common.dto.KnowledgeDto;
import io.aik.steins.grimoire.knowledge.common.dto.KnowledgeQuery;
import io.aik.steins.grimoire.knowledge.common.dto.ToggleStatusDto;
import io.aik.steins.grimoire.knowledge.common.vo.KnowledgeListVo;
import io.aik.steins.grimoire.knowledge.common.vo.KnowledgeVo;
import io.aik.steins.grimoire.knowledge.service.KnowledgeService;
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
 * -anchor 知识条目管理
 *
 * <p>提供知识条目的增删改查、分页列表、详情聚合查询</p>
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since 2026/05/18
 * -
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/knowledge")
@Tag(name = "知识条目管理")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    @PostMapping("/add")
    @Operation(summary = "新增知识条目")
    public ApiResponse<Void> add(@RequestBody @Validated KnowledgeDto dto) {
        knowledgeService.add(dto);
        return ApiResponse.success();
    }

    @PostMapping("/update")
    @Operation(summary = "修改知识条目")
    public ApiResponse<Void> update(@RequestBody @Validated KnowledgeDto dto) {
        knowledgeService.update(dto);
        return ApiResponse.success();
    }

    @PostMapping("/remove")
    @Operation(summary = "删除知识条目")
    public ApiResponse<Void> remove(@RequestBody @Validated IdDto idDto) {
        knowledgeService.delete(idDto.getId());
        return ApiResponse.success();
    }

    @PostMapping("/page")
    @Operation(summary = "分页查询知识条目")
    public ApiResponse<IPage<KnowledgeListVo>> page(@RequestBody KnowledgeQuery query) {
        return ApiResponse.success(knowledgeService.findPage(query));
    }

    @GetMapping("/findById")
    @Operation(summary = "查询知识条目详情")
    public ApiResponse<KnowledgeVo> findById(@RequestParam Long id) {
        return ApiResponse.success(knowledgeService.findById(id));
    }

    @PostMapping("/toggleStatus")
    @Operation(summary = "切换知识条目状态")
    public ApiResponse<Void> toggleStatus(@RequestBody @Validated ToggleStatusDto dto) {
        knowledgeService.toggleStatus(dto.getId(), dto.getStatus());
        return ApiResponse.success();
    }
}
