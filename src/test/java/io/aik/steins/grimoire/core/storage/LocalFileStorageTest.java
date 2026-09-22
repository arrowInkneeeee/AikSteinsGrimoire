package io.aik.steins.grimoire.core.storage;

import cn.hutool.core.io.FileUtil;
import io.aik.steins.grimoire.core.config.FileStorageConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * LocalFileStorage 存储策略单元测试 -anchor
 * <p>补齐 yyyy/MM/dd 日期分片路径拼接这一 core/storage 行为（仅本策略实现该分片），
 * 覆盖 upload / download / remove / exists 主路径与 base-path 优先级。</p>
 * <p>仅使用 target/ 下的临时目录，测试结束即删除；无任何外部依赖与网络访问。</p>
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since 2026/09/20
 * -
 **/
@DisplayName("本地存储策略测试")
class LocalFileStorageTest {

    private static final byte[] PAYLOAD = "grimoire-local-payload".getBytes(StandardCharsets.UTF_8);
    private static final DateTimeFormatter DATE_SHARD = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private Path storageRoot;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        storageRoot = Paths.get("target", "test-storage-" + suffix);
    }

    @AfterEach
    void tearDown() {
        FileUtil.del(storageRoot.toFile());
    }

    // ==================== 测试夹具 ====================

    private static LocalFileStorage storageAt(String basePath) {
        FileStorageConfig.LocalConfig local = new FileStorageConfig.LocalConfig();
        local.setBasePath(basePath);
        FileStorageConfig.MethodConfig method = new FileStorageConfig.MethodConfig();
        method.setLocal(local);
        FileStorageConfig config = new FileStorageConfig();
        config.setUse("local");
        config.setMethod(method);
        LocalFileStorage storage = new LocalFileStorage(config);
        storage.init();
        return storage;
    }

    // ==================== upload ====================

    @Test
    @DisplayName("upload：写入 yyyy/MM/dd 分片目录并保留扩展名")
    void upload_shouldWriteIntoDateShardedDirectoryAndKeepExtension() throws Exception {
        String relativePath = storageAt(storageRoot.toString())
                .upload(new ByteArrayInputStream(PAYLOAD), "报告.docx");

        assertThat(relativePath).matches(LocalDate.now().format(DATE_SHARD) + "/\\d+\\.docx");
        Path stored = storageRoot.resolve(relativePath);
        assertThat(stored).exists();
        assertThat(Files.readAllBytes(stored)).isEqualTo(PAYLOAD);
    }

    @Test
    @DisplayName("upload：无扩展名时仅用雪花 ID")
    void upload_withoutExtension_shouldStoreSnowflakeNameOnly() throws Exception {
        String relativePath = storageAt(storageRoot.toString())
                .upload(new ByteArrayInputStream(PAYLOAD), "README");

        assertThat(relativePath).matches(LocalDate.now().format(DATE_SHARD) + "/\\d+");
        assertThat(storageRoot.resolve(relativePath)).exists();
    }

    @Test
    @DisplayName("upload：同名文件两次上传产生两个不同快照")
    void upload_sameFilenameTwice_shouldProduceDistinctSnapshots() throws Exception {
        LocalFileStorage storage = storageAt(storageRoot.toString());

        String first = storage.upload(new ByteArrayInputStream(PAYLOAD), "a.png");
        String second = storage.upload(new ByteArrayInputStream(PAYLOAD), "a.png");

        assertThat(first).isNotEqualTo(second);
        assertThat(first).endsWith(".png");
        assertThat(second).endsWith(".png");
    }

    // ==================== download ====================

    @Test
    @DisplayName("download：读回上传字节")
    void download_shouldRoundTripUploadedBytes() throws Exception {
        LocalFileStorage storage = storageAt(storageRoot.toString());
        String relativePath = storage.upload(new ByteArrayInputStream(PAYLOAD), "a.txt");

        assertThat(storage.download(relativePath)).isEqualTo(PAYLOAD);
    }

    @Test
    @DisplayName("download：文件缺失时抛 IOException 并带上路径")
    void download_missingFile_shouldThrowIOExceptionWithPath() {
        LocalFileStorage storage = storageAt(storageRoot.toString());

        assertThatThrownBy(() -> storage.download("2026/01/01/missing.txt"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("文件不存在")
                .hasMessageContaining("2026/01/01/missing.txt");
    }

    // ==================== remove / exists ====================

    @Test
    @DisplayName("remove/exists：删除后 exists 归假")
    void remove_shouldDeleteFileAndExistsFollows() throws Exception {
        LocalFileStorage storage = storageAt(storageRoot.toString());
        String relativePath = storage.upload(new ByteArrayInputStream(PAYLOAD), "a.txt");

        assertThat(storage.exists(relativePath)).isTrue();
        assertThat(storage.remove(relativePath)).isTrue();
        assertThat(storage.exists(relativePath)).isFalse();
    }

    // ==================== base-path 解析 ====================

    @Test
    @DisplayName("base-path：method.local.base-path 生效且写入正确目录")
    void basePath_shouldUseMethodLocalConfig() throws Exception {
        LocalFileStorage storage = storageAt(storageRoot.toString());

        String relativePath = storage.upload(new ByteArrayInputStream(PAYLOAD), "a.txt");

        assertThat(storageRoot.resolve(relativePath)).exists();
    }
}
