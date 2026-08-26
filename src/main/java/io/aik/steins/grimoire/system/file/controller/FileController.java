package io.aik.steins.grimoire.system.file.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.aik.steins.grimoire.core.dto.ApiResponse;
import io.aik.steins.grimoire.core.dto.IdDto;
import io.aik.steins.grimoire.system.file.dto.FileQuery;
import io.aik.steins.grimoire.system.file.dto.FileRenameDto;
import io.aik.steins.grimoire.system.file.vo.FileVo;
import io.aik.steins.grimoire.system.file.service.FileService;
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
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;

/**
 * 文件 Controller -anchor
 *
 * @author a I k .
 */
@Slf4j
@RestController
@RequestMapping("/grimoire/file")
@RequiredArgsConstructor
@Tag(name = "文件管理")
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload")
    @Operation(summary = "上传文件")
    public ApiResponse<FileVo> upload(@RequestParam("file") MultipartFile file) {
        FileVo vo = fileService.upload(file);
        return ApiResponse.success(vo);
    }

    @GetMapping("/download")
    @Operation(summary = "下载文件")
    public void download(@RequestParam Long id,
                         @RequestParam(required = false, defaultValue = "false") boolean preview,
                         HttpServletResponse response) {
        fileService.download(id, response, preview);
    }

    @PostMapping("/findPage")
    @Operation(summary = "分页查询文件")
    public ApiResponse<IPage<FileVo>> findPage(@RequestBody FileQuery query) {
        return ApiResponse.success(fileService.findPage(query));
    }

    @PostMapping("/rename")
    @Operation(summary = "重命名文件")
    public ApiResponse<Void> rename(@RequestBody @Validated FileRenameDto dto) {
        fileService.rename(dto.getId(), dto.getOriginalName());
        return ApiResponse.success();
    }

    @PostMapping("/remove")
    @Operation(summary = "删除文件")
    public ApiResponse<Void> remove(@RequestBody @Validated IdDto idDto) {
        fileService.remove(idDto.getId());
        return ApiResponse.success();
    }
}
