package io.aik.steins.grimoire.core.storage;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import io.aik.steins.grimoire.core.config.FileStorageConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 本地文件存储策略
 *
 * @author a I k .
 * @implNote JDK 8
 * @since 2026/08/26
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "grimoire.file", name = "use",
        havingValue = "local", matchIfMissing = true
)
public class LocalFileStorage extends AbstractFileStorage {

    private final FileStorageConfig fileStorageConfig;

    @PostConstruct
    public void init() {
        String basePath = getBasePath();
        FileUtil.mkdir(basePath);
        log.info("本地文件存储初始化完成，根路径：{}", basePath);
    }

    @Override
    public String upload(InputStream inputStream, String originalFilename) throws Exception {
        String storedName = generateStoredName(originalFilename);
        String relativePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String basePath = getBasePath();

        // 统一使用正斜杠
        String fullDir = basePath + "/" + relativePath;
        String fullPath = fullDir + "/" + storedName;

        FileUtil.mkdir(fullDir);
        FileUtil.writeFromStream(inputStream, fullPath);

        return relativePath + "/" + storedName;
    }

    @Override
    public byte[] download(String storedPath) throws Exception {
        String fullPath = getBasePath() + "/" + storedPath;
        File file = new File(fullPath);
        if (!file.exists()) {
            throw new IOException("文件不存在：" + storedPath);
        }
        return FileUtil.readBytes(file);
    }

    @Override
    public boolean remove(String storedPath) throws Exception {
        String fullPath = getBasePath() + "/" + storedPath;
        return FileUtil.del(fullPath);
    }

    @Override
    public boolean exists(String storedPath) throws Exception {
        String fullPath = getBasePath() + "/" + storedPath;
        return FileUtil.exist(fullPath);
    }

    @Override
    public String getUrl(String storedPath) {
        return storedPath;
    }

    /**
     * 获取本地存储根路径
     */
    private String getBasePath() {
        if (fileStorageConfig.getMethod() != null
                && fileStorageConfig.getMethod().getLocal() != null
                && StrUtil.isNotBlank(fileStorageConfig.getMethod().getLocal().getBasePath())) {
            return fileStorageConfig.getMethod().getLocal().getBasePath();
        }
        // 兼容旧配置
        if (StrUtil.isNotBlank(fileStorageConfig.getBasePath())) {
            return fileStorageConfig.getBasePath();
        }
        return "./grimoire-files/storage";
    }
}
