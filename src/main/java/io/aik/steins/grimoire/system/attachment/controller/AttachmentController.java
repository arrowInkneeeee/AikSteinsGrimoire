package io.aik.steins.grimoire.system.attachment.controller;

import io.aik.steins.grimoire.core.dto.ApiResponse;
import io.aik.steins.grimoire.core.dto.IdDto;
import io.aik.steins.grimoire.system.attachment.dto.AttachmentSaveDto;
import io.aik.steins.grimoire.system.attachment.service.AttachmentService;
import io.aik.steins.grimoire.system.attachment.vo.AttachmentVo;
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
 * 通用附件挂载 Controller -anchor
 *
 * <p>接口形状权威：api-contract.md §2.5.2（三端点）。路径采用<b>通用</b>形态
 * {@code findByBiz}——{@code /grimoire/attachment/findByKnowledgeId} 已<b>废弃</b>：
 * 路径里出现业务名，等于把刚删掉的 {@code knowledge_id} 硬编码从【列】搬到【URL】。</p>
 *
 * <p>附件域的三端点都<b>按业务发起</b>：列表带 {@code bizType} + {@code bizId}，卸载带挂载行 id；
 * 因此响应里不回显业务归属（{@link AttachmentVo} 的字段集不含 {@code bizType} / {@code bizId}）。</p>
 *
 * @author a I k .
 */
@Slf4j
@RestController
@RequestMapping("/grimoire/attachment")
@RequiredArgsConstructor
@Tag(name = "附件挂载管理")
public class AttachmentController {

    private final AttachmentService attachmentService;

    @GetMapping("/findByBiz")
    @Operation(summary = "按业务查询附件挂载列表")
    public ApiResponse<List<AttachmentVo>> findByBiz(@RequestParam String bizType,
                                                     @RequestParam Long bizId) {
        return ApiResponse.success(attachmentService.listByBiz(bizType, bizId));
    }

    @PostMapping("/save")
    @Operation(summary = "保存附件挂载（差量语义）")
    public ApiResponse<Void> save(@RequestBody @Validated AttachmentSaveDto dto) {
        attachmentService.save(dto.getBizType(), dto.getBizId(), dto.getAttachments());
        return ApiResponse.success();
    }

    @PostMapping("/remove")
    @Operation(summary = "卸载附件挂载")
    public ApiResponse<Void> remove(@RequestBody @Validated IdDto idDto) {
        attachmentService.remove(idDto.getId());
        return ApiResponse.success();
    }
}
