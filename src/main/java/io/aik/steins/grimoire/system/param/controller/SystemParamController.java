package io.aik.steins.grimoire.system.param.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.aik.steins.grimoire.core.dto.ApiResponse;
import io.aik.steins.grimoire.core.dto.IdDto;
import io.aik.steins.grimoire.system.param.dto.SystemParamDto;
import io.aik.steins.grimoire.system.param.dto.SystemParamQuery;
import io.aik.steins.grimoire.system.param.vo.SystemParamVo;
import io.aik.steins.grimoire.system.param.service.SystemParamService;
import io.swagger.v3.oas.annotations.Operation;

import java.util.List;
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
 * 系统参数 Controller -anchor
 *
 * @author a I k .
 */
@Slf4j
@RestController
@RequestMapping("/grimoire/systemParam")
@RequiredArgsConstructor
@Tag(name = "系统参数管理")
public class SystemParamController {

    private final SystemParamService systemParamService;

    @PostMapping("/findPage")
    @Operation(summary = "分页查询系统参数")
    public ApiResponse<IPage<SystemParamVo>> findPage(@RequestBody SystemParamQuery query) {
        return ApiResponse.success(systemParamService.findPage(query));
    }

    @GetMapping("/findByKey")
    @Operation(summary = "根据参数键查询")
    public ApiResponse<SystemParamVo> findByKey(@RequestParam String paramKey) {
        return ApiResponse.success(systemParamService.findByKey(paramKey));
    }

    @GetMapping("/findByGroup")
    @Operation(summary = "根据参数分组查询参数列表")
    public ApiResponse<List<SystemParamVo>> findByGroup(@RequestParam String group) {
        return ApiResponse.success(systemParamService.findByGroup(group));
    }

    @PostMapping("/add")
    @Operation(summary = "新增系统参数")
    public ApiResponse<Void> add(@RequestBody @Validated SystemParamDto dto) {
        systemParamService.add(dto);
        return ApiResponse.success();
    }

    @PostMapping("/modify")
    @Operation(summary = "修改系统参数")
    public ApiResponse<Void> modify(@RequestBody @Validated SystemParamDto dto) {
        systemParamService.modify(dto);
        return ApiResponse.success();
    }

    @PostMapping("/remove")
    @Operation(summary = "删除系统参数")
    public ApiResponse<Void> remove(@RequestBody @Validated IdDto idDto) {
        systemParamService.remove(idDto.getId());
        return ApiResponse.success();
    }

    @PostMapping("/refreshCache")
    @Operation(summary = "刷新参数缓存")
    public ApiResponse<Void> refreshCache() {
        systemParamService.refreshCache();
        return ApiResponse.success();
    }
}
