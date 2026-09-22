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
import java.nio.file.Paths;
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

    /**
     * 获取本地存储根路径
     *
     * <p>绝对路径直接使用；相对路径以<b>项目根目录</b>（含 {@code pom.xml} 的最近祖先目录）
     * 为基准解析，而非 JVM 工作目录（{@code user.dir}）——后者在 IDE 启动或部分
     * Maven 场景下可能指向 {@code target/}，导致文件写入构建产物目录。</p>
     *
     * @return 绝对规范路径
     */
    private String getBasePath() {
        String raw = resolveBasePath();
        java.nio.file.Path path = Paths.get(raw);
        if (path.isAbsolute()) {
            return path.normalize().toString();
        }
        // 相对路径：以项目根目录（含 pom.xml 的最近祖先目录）为基准解析
        java.nio.file.Path projectRoot = findProjectRoot();
        if (projectRoot != null) {
            return projectRoot.resolve(raw).normalize().toString();
        }
        // 兜底：使用 JVM 工作目录
        return path.toAbsolutePath().normalize().toString();
    }

    /**
     * 从 JVM 工作目录向上查找项目根目录（含 {@code pom.xml} 的最近祖先目录）
     *
     * <p>解决「IDE 启动时工作目录为 target/，相对路径解析到构建产物目录」的问题。
     * 找不到时返回 {@code null}，调用方回退到 {@code user.dir}。</p>
     *
     * @return 项目根目录；找不到返回 {@code null}
     */
    private java.nio.file.Path findProjectRoot() {
        java.nio.file.Path dir = java.nio.file.Paths.get("").toAbsolutePath();
        while (dir != null) {
            if (java.nio.file.Files.exists(dir.resolve("pom.xml"))) {
                return dir;
            }
            dir = dir.getParent();
        }
        return null;
    }

    /**
     * 解析配置中的存储根路径（可能为相对路径）
     * <p>读取 {@code grimoire.file.method.local.base-path}；未配置时回退到内置默认值。</p>
     *
     * @return 配置中的根路径
     */
    private String resolveBasePath() {
        if (fileStorageConfig.getMethod() != null
                && fileStorageConfig.getMethod().getLocal() != null
                && StrUtil.isNotBlank(fileStorageConfig.getMethod().getLocal().getBasePath())) {
            return fileStorageConfig.getMethod().getLocal().getBasePath();
        }
        return "./grimoire-files/storage";
    }
}
