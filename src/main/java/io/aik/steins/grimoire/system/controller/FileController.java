package io.aik.steins.grimoire.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.aik.steins.grimoire.core.dto.ApiResponse;
import io.aik.steins.grimoire.system.common.dto.FileQuery;
import io.aik.steins.grimoire.system.common.vo.FileDownloadResult;
import io.aik.steins.grimoire.system.common.vo.FileVo;
import io.aik.steins.grimoire.system.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
    public ResponseEntity<Resource> download(@RequestParam Long id) {
        FileDownloadResult result = fileService.download(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=\"" + result.getOriginalName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(result.getResource());
    }

    @PostMapping("/findPage")
    @Operation(summary = "分页查询文件")
    public ApiResponse<IPage<FileVo>> findPage(@RequestBody FileQuery query) {
        return ApiResponse.success(fileService.findPage(query));
    }

    @PostMapping("/rename")
    @Operation(summary = "重命名文件")
    public ApiResponse<Void> rename(@RequestParam Long id, @RequestParam String originalName) {
        fileService.rename(id, originalName);
        return ApiResponse.success();
    }

    @PostMapping("/remove")
    @Operation(summary = "删除文件")
    public ApiResponse<Void> remove(@RequestParam Long id) {
        fileService.remove(id);
        return ApiResponse.success();
    }
}
