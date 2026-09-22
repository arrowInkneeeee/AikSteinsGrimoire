package io.aik.steins.grimoire.core.storage;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.OSSObject;
import io.aik.steins.grimoire.core.config.FileStorageConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * OssFileStorage 存储策略单元测试 -anchor
 * <p>覆盖 upload / download / remove / exists 四条主路径，并覆盖继承自
 * {@link AbstractFileStorage} 的下载响应模板（Content-Type 解析、inline/attachment、异常包装）。</p>
 * <p>外部依赖（{@link OSSClientBuilder} / {@link OSS} / {@link OSSObject}）全部 mock：
 * OSS 客户端构造被 {@code mockConstruction} 拦截，{@link OSS} 为 Mockito mock，
 * 测试期间不发起任何真实网络连接。</p>
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote 依赖 src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker（inline mock maker）
 * @since 2026/09/20
 * -
 **/
@DisplayName("OSS 存储策略测试")
class OssFileStorageTest {

    private static final String ENDPOINT = "https://oss-cn-hangzhou.aliyuncs.com";
    private static final String ACCESS_KEY_ID = "test-access-key-id";
    private static final String ACCESS_KEY_SECRET = "test-access-key-secret";
    private static final String BUCKET_NAME = "grimoire-test-bucket";
    private static final String OBJECT_PATH = "2026/09/20/aik-grimoire.pdf";
    private static final byte[] PAYLOAD = "grimoire-oss-payload".getBytes(StandardCharsets.UTF_8);

    private OSS ossClient;
    private MockedConstruction<OSSClientBuilder> builderConstruction;

    @BeforeEach
    void setUp() {
        ossClient = mock(OSS.class);
        builderConstruction = Mockito.mockConstruction(OSSClientBuilder.class,
                (builder, context) -> when(builder.build(anyString(), anyString(), anyString())).thenReturn(ossClient));
    }

    @AfterEach
    void tearDown() {
        builderConstruction.close();
    }

    // ==================== 测试夹具 ====================

    private static FileStorageConfig ossConfig(String basePath) {
        FileStorageConfig.OssConfig oss = new FileStorageConfig.OssConfig();
        oss.setEndpoint(ENDPOINT);
        oss.setAccessKeyId(ACCESS_KEY_ID);
        oss.setAccessKeySecret(ACCESS_KEY_SECRET);
        oss.setBucketName(BUCKET_NAME);
        oss.setBasePath(basePath);
        FileStorageConfig.MethodConfig method = new FileStorageConfig.MethodConfig();
        method.setOss(oss);
        FileStorageConfig config = new FileStorageConfig();
        config.setUse("oss");
        config.setMethod(method);
        return config;
    }

    /**
     * 构造并初始化策略实例；init() 内的 {@code new OSSClientBuilder()} 被 mock 拦截。
     */
    private OssFileStorage storage(String basePath) {
        OssFileStorage storage = new OssFileStorage(ossConfig(basePath));
        storage.init();
        return storage;
    }

    private static InputStream payloadStream() {
        return new ByteArrayInputStream(PAYLOAD);
    }

    // ==================== init ====================

    @Test
    @DisplayName("init：以配置的 endpoint / AK / SK 构建客户端")
    void init_shouldBuildClientWithConfiguredEndpointAndCredentials() {
        storage("/upload");

        assertThat(builderConstruction.constructed()).hasSize(1);
        verify(builderConstruction.constructed().get(0)).build(ENDPOINT, ACCESS_KEY_ID, ACCESS_KEY_SECRET);
    }

    // ==================== upload ====================

    @Test
    @DisplayName("upload：objectKey = basePath + 雪花名，扩展名保留小写")
    void upload_shouldPrefixBasePathAndPreserveLowercaseExtension() throws Exception {
        OssFileStorage storage = storage("grimoire/attachments");
        InputStream inputStream = payloadStream();

        String objectKey = storage.upload(inputStream, "魔典封面.png");

        assertThat(objectKey).matches("grimoire/attachments/\\d+\\.png");
        assertThat(objectKey).doesNotContain("//");
        verify(ossClient).putObject(BUCKET_NAME, objectKey, inputStream);
    }

    @Test
    @DisplayName("upload：basePath 已带 / 时不产生双斜杠")
    void upload_basePathAlreadyEndsWithSlash_shouldNotDoubleSlash() throws Exception {
        String objectKey = storage("base/").upload(payloadStream(), "a.txt");

        assertThat(objectKey).matches("base/\\d+\\.txt");
        assertThat(objectKey).doesNotContain("//");
    }

    @Test
    @DisplayName("upload：basePath 为空白时不加前缀")
    void upload_blankBasePath_shouldHaveNoPrefix() throws Exception {
        String objectKey = storage("   ").upload(payloadStream(), "a.txt");

        assertThat(objectKey).matches("\\d+\\.txt");
    }

    @Test
    @DisplayName("upload：扩展名统一小写")
    void upload_shouldLowercaseExtension() throws Exception {
        String objectKey = storage("").upload(payloadStream(), "Report.PDF");

        assertThat(objectKey).matches("\\d+\\.pdf");
    }

    @Test
    @DisplayName("upload：原始文件无扩展名时仅返回雪花 ID")
    void upload_filenameWithoutExtension_shouldReturnPureSnowflake() throws Exception {
        String objectKey = storage("").upload(payloadStream(), "README");

        assertThat(objectKey).matches("\\d+");
    }

    @Test
    @DisplayName("upload：原始文件名为空白时仅返回雪花 ID")
    void upload_blankFilename_shouldReturnPureSnowflake() throws Exception {
        String objectKey = storage("").upload(payloadStream(), "   ");

        assertThat(objectKey).matches("\\d+");
    }

    // ==================== download ====================

    @Test
    @DisplayName("download：返回对象字节并关闭对象流")
    void download_shouldReturnObjectBytesAndCloseContentStream() throws Exception {
        TrackingInputStream content = new TrackingInputStream(PAYLOAD);
        OSSObject ossObject = mock(OSSObject.class);
        when(ossObject.getObjectContent()).thenReturn(content);
        when(ossClient.getObject(BUCKET_NAME, OBJECT_PATH)).thenReturn(ossObject);

        byte[] data = storage("").download(OBJECT_PATH);

        assertThat(data).isEqualTo(PAYLOAD);
        assertThat(content.closed).isTrue();
        verify(ossClient).getObject(BUCKET_NAME, OBJECT_PATH);
    }

    // ==================== remove ====================

    @Test
    @DisplayName("remove：删除对象并返回 true")
    void remove_shouldDeleteObjectAndReturnTrue() throws Exception {
        boolean removed = storage("").remove(OBJECT_PATH);

        assertThat(removed).isTrue();
        verify(ossClient).deleteObject(BUCKET_NAME, OBJECT_PATH);
    }

    // ==================== exists ====================

    @Test
    @DisplayName("exists：对象存在时返回 true")
    void exists_shouldReturnTrueWhenObjectExists() throws Exception {
        when(ossClient.doesObjectExist(BUCKET_NAME, OBJECT_PATH)).thenReturn(true);

        assertThat(storage("").exists(OBJECT_PATH)).isTrue();
        verify(ossClient).doesObjectExist(BUCKET_NAME, OBJECT_PATH);
    }

    @Test
    @DisplayName("exists：对象不存在时返回 false")
    void exists_shouldReturnFalseWhenObjectMissing() throws Exception {
        when(ossClient.doesObjectExist(BUCKET_NAME, OBJECT_PATH)).thenReturn(false);

        assertThat(storage("").exists(OBJECT_PATH)).isFalse();
    }

    // ==================== destroy ====================

    @Test
    @DisplayName("destroy：关闭 OSS 客户端")
    void destroy_shouldShutdownClient() {
        OssFileStorage storage = storage("");

        storage.destroy();

        verify(ossClient).shutdown();
    }

    @Test
    @DisplayName("destroy：未 init 时不抛异常")
    void destroy_withoutInit_shouldNotFail() {
        OssFileStorage storage = new OssFileStorage(ossConfig(""));

        assertThatCode(storage::destroy).doesNotThrowAnyException();
    }

    // ==================== 下载响应模板（继承自 AbstractFileStorage） ====================

    @Nested
    @DisplayName("下载响应模板")
    class DownloadResponseTemplateTest {

        private HttpServletResponse response;
        private ServletOutputStream outputStream;

        @BeforeEach
        void setUpResponse() throws Exception {
            response = mock(HttpServletResponse.class);
            outputStream = mock(ServletOutputStream.class);
            when(response.getOutputStream()).thenReturn(outputStream);
            OSSObject ossObject = mock(OSSObject.class);
            when(ossObject.getObjectContent()).thenReturn(new ByteArrayInputStream(PAYLOAD));
            when(ossClient.getObject(anyString(), anyString())).thenReturn(ossObject);
        }

        @Test
        @DisplayName("download：按扩展名解析 Content-Type 并写 attachment")
        void download_toResponse_shouldResolveContentTypeAndAttachmentDisposition() throws Exception {
            storage("").download(response, OBJECT_PATH);

            verify(response).setContentType("application/pdf");
            verify(response).setHeader("Content-Disposition", "attachment;filename*=UTF-8''aik-grimoire.pdf");
            verify(response).setContentLength(PAYLOAD.length);
            verify(outputStream).write(PAYLOAD);
        }

        @Test
        @DisplayName("download：preview=true 时写 inline")
        void download_preview_shouldUseInlineDisposition() throws Exception {
            storage("").download(response, "photo.png", true);

            verify(response).setContentType("image/png");
            verify(response).setHeader("Content-Disposition", "inline;filename*=UTF-8''photo.png");
        }

        @Test
        @DisplayName("download：给定原始中文文件名时以其作为响应文件名")
        void download_withOriginalName_shouldUseOriginalNameAsFileName() throws Exception {
            String originalName = "魔典封面.pdf";
            storage("").download(response, OBJECT_PATH, originalName, false);

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(response).setHeader(eq("Content-Disposition"), captor.capture());
            String expected = "attachment;filename*=UTF-8''"
                    + URLEncoder.encode(originalName, "UTF-8").replace("+", "%20");
            assertThat(captor.getValue()).isEqualTo(expected);
        }

        @Test
        @DisplayName("download：响应写出失败时包装为 RuntimeException")
        void download_whenResponseFails_shouldWrapAsRuntimeException() throws Exception {
            when(response.getOutputStream()).thenThrow(new IOException("stream broken"));

            assertThatThrownBy(() -> storage("").download(response, OBJECT_PATH))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("文件下载失败");
        }
    }

    /**
     * 可观测关闭状态的字节输入流，用于断言对象内容流被关闭
     */
    private static final class TrackingInputStream extends ByteArrayInputStream {

        private boolean closed;

        private TrackingInputStream(byte[] buf) {
            super(buf);
        }

        @Override
        public void close() throws IOException {
            this.closed = true;
            super.close();
        }
    }
}
