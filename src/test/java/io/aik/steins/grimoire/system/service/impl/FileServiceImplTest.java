package io.aik.steins.grimoire.system.file.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import io.aik.steins.grimoire.core.config.FileStorageConfig;
import io.aik.steins.grimoire.core.exception.BusinessException;
import io.aik.steins.grimoire.core.storage.FileStorageStrategy;
import io.aik.steins.grimoire.system.file.po.FileRecordPo;
import io.aik.steins.grimoire.system.file.vo.FileVo;
import io.aik.steins.grimoire.system.file.dao.FileMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FileServiceImpl 单元测试 -anchor
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since 2026/7/29
 * -
 **/
@ExtendWith(MockitoExtension.class)
@DisplayName("文件服务测试")
class FileServiceImplTest {

    private static final Long TEST_FILE_ID = 2001L;
    private static final Long TEST_ATTACH_ID = 3001L;
    private static final String TEST_FILE_NAME = "魔典封面.png";
    private static final Long TEST_MAX_SIZE = 10L;

    @Mock
    private FileMapper fileMapper;

    @Mock
    private FileStorageConfig fileStorageConfig;

    @Mock
    private MultipartFile multipartFile;

    @Mock
    private HttpServletResponse httpServletResponse;

    @Mock
    private FileStorageStrategy fileStorageStrategy;

    @InjectMocks
    private FileServiceImpl fileService;

    @Nested
    @DisplayName("上传校验")
    class UploadValidationTest {

        @Test
        @DisplayName("文件为 null 时抛出 BusinessException")
        void upload_nullFile_throwsBusinessException() {
            // -anchor given & when & then
            assertThatThrownBy(() -> fileService.upload(null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("文件不能为空");
        }

        @Test
        @DisplayName("空文件时抛出 BusinessException")
        void upload_emptyFile_throwsBusinessException() {
            // -anchor given
            when(multipartFile.isEmpty()).thenReturn(true);

            // -anchor when & then
            assertThatThrownBy(() -> fileService.upload(multipartFile))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("文件不能为空");
        }

        @Test
        @DisplayName("文件超过大小限制时抛出 BusinessException")
        void upload_oversize_throwsBusinessException() {
            // -anchor given
            when(multipartFile.isEmpty()).thenReturn(false);
            when(multipartFile.getSize()).thenReturn(TEST_MAX_SIZE + 1L);
            when(fileStorageConfig.getMaxSize()).thenReturn(TEST_MAX_SIZE);

            // -anchor when & then
            assertThatThrownBy(() -> fileService.upload(multipartFile))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("文件大小不能超过");
        }

        @Test
        @DisplayName("文件类型不在白名单时抛出 BusinessException")
        void upload_typeNotAllowed_throwsBusinessException() {
            // -anchor given
            when(multipartFile.isEmpty()).thenReturn(false);
            when(multipartFile.getSize()).thenReturn(1L);
            when(multipartFile.getContentType()).thenReturn("application/octet-stream");
            when(multipartFile.getOriginalFilename()).thenReturn("evil.exe");
            when(fileStorageConfig.getMaxSize()).thenReturn(TEST_MAX_SIZE);
            when(fileStorageConfig.getTypeCheckEnabled()).thenReturn(true);
            when(fileStorageConfig.getAllowTypes()).thenReturn("jpg,png");
            when(fileStorageConfig.getAllowTypeSet()).thenReturn(new HashSet<>(Arrays.asList("jpg", "png")));

            // -anchor when & then
            assertThatThrownBy(() -> fileService.upload(multipartFile))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("不支持的文件类型");
        }
    }

    @Nested
    @DisplayName("md5 秒传")
    class UploadDedupTest {

        @Test
        @DisplayName("md5 命中：跳过写盘且不插入新记录，复用既有 fileId")
        void upload_md5Hit_skipsDiskWriteAndInsert() throws Exception {
            // -anchor given
            byte[] content = "abc".getBytes(StandardCharsets.UTF_8);
            String md5 = DigestUtil.md5Hex(content);
            FileRecordPo existing = buildPo(TEST_FILE_ID, "首次上传者.png");
            existing.setMd5(md5);

            when(multipartFile.isEmpty()).thenReturn(false);
            when(multipartFile.getSize()).thenReturn((long) content.length);
            when(multipartFile.getOriginalFilename()).thenReturn(TEST_FILE_NAME);
            when(fileStorageConfig.getMaxSize()).thenReturn(TEST_MAX_SIZE);
            when(multipartFile.getBytes()).thenReturn(content);
            when(fileMapper.selectLatestByMd5(md5)).thenReturn(existing);

            // -anchor when
            FileVo vo = fileService.upload(multipartFile);

            // -anchor then
            assertThat(vo.getId()).isEqualTo(TEST_FILE_ID);
            assertThat(vo.getOriginalName()).isEqualTo("首次上传者.png");
            // 两个 upload 重载都不得被调用：命中查重时【磁盘零写入】
            verify(fileStorageStrategy, never()).upload(any(InputStream.class), anyString());
            verify(fileStorageStrategy, never()).upload(any(MultipartFile.class));
            verify(fileMapper, never()).insert(any(FileRecordPo.class));
        }

        @Test
        @DisplayName("md5 未命中：写盘并插入新记录")
        void upload_md5Miss_writesDiskAndInserts() throws Exception {
            // -anchor given
            byte[] content = "xyz".getBytes(StandardCharsets.UTF_8);
            String md5 = DigestUtil.md5Hex(content);
            String storedPath = "2026/09/20/new.png";

            when(multipartFile.isEmpty()).thenReturn(false);
            when(multipartFile.getSize()).thenReturn((long) content.length);
            when(multipartFile.getOriginalFilename()).thenReturn(TEST_FILE_NAME);
            when(fileStorageConfig.getMaxSize()).thenReturn(TEST_MAX_SIZE);
            when(multipartFile.getBytes()).thenReturn(content);
            when(fileMapper.selectLatestByMd5(md5)).thenReturn(null);
            when(fileStorageStrategy.upload(any(InputStream.class), eq(TEST_FILE_NAME))).thenReturn(storedPath);
            when(fileStorageConfig.getUse()).thenReturn("local");
            when(multipartFile.getContentType()).thenReturn("image/png");

            // -anchor when
            FileVo vo = fileService.upload(multipartFile);

            // -anchor then
            assertThat(vo.getOriginalName()).isEqualTo(TEST_FILE_NAME);
            assertThat(vo.getStorageType()).isEqualTo("local");
            verify(fileStorageStrategy).upload(any(InputStream.class), eq(TEST_FILE_NAME));
            verify(fileStorageStrategy, never()).upload(any(MultipartFile.class));
            verify(fileMapper).insert(any(FileRecordPo.class));
        }
    }

    @Nested
    @DisplayName("按 ID 查询")
    class FindByIdTest {

        @Test
        @DisplayName("文件存在时返回视图")
        void findById_exists_returnsVo() {
            // -anchor given
            FileRecordPo po = buildPo(TEST_FILE_ID, TEST_FILE_NAME);
            when(fileMapper.selectById(TEST_FILE_ID)).thenReturn(po);

            // -anchor when
            FileVo result = fileService.findById(TEST_FILE_ID);

            // -anchor then
            assertThat(result).isNotNull();
            assertThat(result.getOriginalName()).isEqualTo(TEST_FILE_NAME);
        }

        @Test
        @DisplayName("文件不存在时抛出 BusinessException")
        void findById_notExists_throwsBusinessException() {
            // -anchor given
            when(fileMapper.selectById(TEST_FILE_ID)).thenReturn(null);

            // -anchor when & then
            assertThatThrownBy(() -> fileService.findById(TEST_FILE_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("文件不存在");
        }
    }

    @Nested
    @DisplayName("重命名")
    class RenameTest {

        @Test
        @DisplayName("文件存在时重命名成功")
        void rename_success() {
            // -anchor given
            FileRecordPo po = buildPo(TEST_FILE_ID, "旧名称.png");
            when(fileMapper.selectById(TEST_FILE_ID)).thenReturn(po);

            // -anchor when
            fileService.rename(TEST_FILE_ID, TEST_FILE_NAME);

            // -anchor then
            assertThat(po.getOriginalName()).isEqualTo(TEST_FILE_NAME);
            verify(fileMapper).updateById(po);
        }

        @Test
        @DisplayName("文件名为空白时抛出 BusinessException")
        void rename_blankName_throwsBusinessException() {
            // -anchor given & when & then
            assertThatThrownBy(() -> fileService.rename(TEST_FILE_ID, "  "))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("文件名不能为空");
            verify(fileMapper, never()).updateById(any(FileRecordPo.class));
        }

        @Test
        @DisplayName("文件不存在时抛出 BusinessException")
        void rename_notExists_throwsBusinessException() {
            // -anchor given
            when(fileMapper.selectById(TEST_FILE_ID)).thenReturn(null);

            // -anchor when & then
            assertThatThrownBy(() -> fileService.rename(TEST_FILE_ID, TEST_FILE_NAME))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("文件不存在");
        }
    }

    @Nested
    @DisplayName("删除与下载前置校验")
    class GuardTest {

        @Test
        @DisplayName("删除不存在文件时按幂等 no-op 处理：不抛异常、不发 DELETE、不删盘")
        void remove_notExists_isIdempotentNoOp() throws Exception {
            // -anchor given
            when(fileMapper.selectByIdForUpdate(TEST_FILE_ID)).thenReturn(null);

            // -anchor when & then
            assertThatCode(() -> fileService.remove(TEST_FILE_ID)).doesNotThrowAnyException();
            verify(fileMapper, never()).deleteById(any(Long.class));
            verify(fileStorageStrategy, never()).remove(anyString());
        }

        @Test
        @DisplayName("下载不存在文件时抛出 BusinessException")
        void download_notExists_throwsBusinessException() {
            // -anchor given
            when(fileMapper.selectById(TEST_FILE_ID)).thenReturn(null);

            // -anchor when & then
            assertThatThrownBy(() -> fileService.download(TEST_FILE_ID, null, httpServletResponse, false))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("文件不存在");
        }
    }

    @Nested
    @DisplayName("管理端删除（引用计数）")
    class RemoveTest {

        @Test
        @DisplayName("无有效挂载：物理删除记录并删盘")
        void remove_noMount_physicallyDeletesAndRemovesDisk() throws Exception {
            // -anchor given
            FileRecordPo po = buildPo(TEST_FILE_ID, TEST_FILE_NAME);
            when(fileMapper.selectByIdForUpdate(TEST_FILE_ID)).thenReturn(po);
            when(fileMapper.countEffectiveMounts(TEST_FILE_ID)).thenReturn(0L);

            // -anchor when
            fileService.remove(TEST_FILE_ID);

            // -anchor then
            verify(fileMapper).deleteById(TEST_FILE_ID);
            verify(fileStorageStrategy).remove(po.getFilePath());
        }

        @Test
        @DisplayName("仍有其它有效挂载：抛 BusinessException 且不删记录、不删盘")
        void remove_stillMounted_throwsAndKeepsDisk() throws Exception {
            // -anchor given
            FileRecordPo po = buildPo(TEST_FILE_ID, TEST_FILE_NAME);
            when(fileMapper.selectByIdForUpdate(TEST_FILE_ID)).thenReturn(po);
            when(fileMapper.countEffectiveMounts(TEST_FILE_ID)).thenReturn(1L);

            // -anchor when & then
            assertThatThrownBy(() -> fileService.remove(TEST_FILE_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("仍被其它挂载点引用");
            verify(fileMapper, never()).deleteById(any(Long.class));
            verify(fileStorageStrategy, never()).remove(anyString());
        }
    }

    @Nested
    @DisplayName("下载响应文件名来源")
    class DownloadNameTest {

        @Test
        @DisplayName("挂载模式：带 attachId，响应文件名取挂载层 attachName")
        void download_withAttachId_usesAttachNameFromMount() throws Exception {
            // -anchor given
            FileRecordPo po = buildPo(TEST_FILE_ID, "首次上传者.png");
            when(fileMapper.selectById(TEST_FILE_ID)).thenReturn(po);
            when(fileMapper.selectAttachNameForDownload(TEST_ATTACH_ID, TEST_FILE_ID)).thenReturn("会议记录.pdf");

            // -anchor when
            fileService.download(TEST_FILE_ID, TEST_ATTACH_ID, httpServletResponse, false);

            // -anchor then
            verify(fileStorageStrategy).download(httpServletResponse, po.getFilePath(), "会议记录.pdf", false);
        }

        @Test
        @DisplayName("台账模式：不带 attachId，响应文件名取 originalName")
        void download_withoutAttachId_usesLedgerName() throws Exception {
            // -anchor given
            FileRecordPo po = buildPo(TEST_FILE_ID, TEST_FILE_NAME);
            when(fileMapper.selectById(TEST_FILE_ID)).thenReturn(po);

            // -anchor when
            fileService.download(TEST_FILE_ID, null, httpServletResponse, false);

            // -anchor then
            verify(fileStorageStrategy).download(httpServletResponse, po.getFilePath(), TEST_FILE_NAME, false);
            verify(fileMapper, never()).selectAttachNameForDownload(any(), any());
        }

        @Test
        @DisplayName("挂载行与文件不匹配（查询返回 null）时拒绝下载")
        void download_attachIdNotMatchingFile_throws() throws Exception {
            // -anchor given
            FileRecordPo po = buildPo(TEST_FILE_ID, TEST_FILE_NAME);
            when(fileMapper.selectById(TEST_FILE_ID)).thenReturn(po);
            when(fileMapper.selectAttachNameForDownload(TEST_ATTACH_ID, TEST_FILE_ID)).thenReturn(null);

            // -anchor when & then
            assertThatThrownBy(() -> fileService.download(TEST_FILE_ID, TEST_ATTACH_ID, httpServletResponse, false))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("附件挂载不存在或与文件不匹配");
            verify(fileStorageStrategy, never()).download(any(), anyString(), anyString(), anyBoolean());
        }
    }

    private FileRecordPo buildPo(Long id, String originalName) {
        FileRecordPo po = new FileRecordPo();
        po.setId(id);
        po.setOriginalName(originalName);
        po.setStoredName("stored.png");
        po.setFilePath("2026/07/29");
        po.setFileSize(1L);
        po.setDownloadCount(0);
        return po;
    }
}
