package io.aik.steins.grimoire.system.file.service.impl;

import io.aik.steins.grimoire.core.config.FileStorageConfig;
import io.aik.steins.grimoire.core.exception.BusinessException;
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

import java.util.Arrays;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
    private static final String TEST_FILE_NAME = "魔典封面.png";
    private static final Long TEST_MAX_SIZE = 10L;

    @Mock
    private FileMapper fileMapper;

    @Mock
    private FileStorageConfig fileStorageConfig;

    @Mock
    private MultipartFile multipartFile;

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
        @DisplayName("删除不存在文件时抛出 BusinessException")
        void remove_notExists_throwsBusinessException() {
            // -anchor given
            when(fileMapper.selectById(TEST_FILE_ID)).thenReturn(null);

            // -anchor when & then
            assertThatThrownBy(() -> fileService.remove(TEST_FILE_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("文件不存在");
            verify(fileMapper, never()).deleteById(any(Long.class));
        }

        @Test
        @DisplayName("下载不存在文件时抛出 BusinessException")
        void download_notExists_throwsBusinessException() {
            // -anchor given
            when(fileMapper.selectById(TEST_FILE_ID)).thenReturn(null);

            // -anchor when & then
            assertThatThrownBy(() -> fileService.download(TEST_FILE_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("文件不存在");
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
