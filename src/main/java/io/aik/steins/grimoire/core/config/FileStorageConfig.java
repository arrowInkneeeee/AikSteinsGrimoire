package io.aik.steins.grimoire.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文件存储配置 -anchor
 *
 * @author a I k .
 */
@Data
@Component
@ConfigurationProperties(prefix = "grimoire.file")
public class FileStorageConfig {

    /**
     * 文件存储根路径
     */
    private String basePath = "./grimoire-files";

    /**
     * 单个文件最大大小（字节）
     */
    private Long maxSize = 10 * 1024 * 1024L;
}
