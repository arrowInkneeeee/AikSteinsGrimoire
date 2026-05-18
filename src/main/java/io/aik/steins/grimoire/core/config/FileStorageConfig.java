package io.aik.steins.grimoire.core.config;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

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
     * 文件存储根路径（通用回退）
     */
    private String basePath = "./grimoire-files";

    /**
     * Windows 文件存储根路径
     */
    private String basePathWindows;

    /**
     * Linux 文件存储根路径
     */
    private String basePathLinux;

    /**
     * 单个文件最大大小（字节）
     */
    private Long maxSize = 10 * 1024 * 1024L;

    /**
     * 是否进行上传文件类型校验
     */
    private Boolean typeCheckEnabled = true;

    /**
     * 允许上传的文件类型（扩展名或 MIME 类型），逗号分隔
     */
    private String allowTypes;

    /**
     * 获取当前操作系统对应的存储根路径
     */
    public String getEffectiveBasePath() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("windows")) {
            return StrUtil.isNotBlank(basePathWindows) ? basePathWindows : basePath;
        }
        return StrUtil.isNotBlank(basePathLinux) ? basePathLinux : basePath;
    }

    /**
     * 解析允许的文件类型集合
     */
    public Set<String> getAllowTypeSet() {
        if (StrUtil.isBlank(allowTypes)) {
            return Collections.emptySet();
        }
        return Arrays.stream(allowTypes.split(","))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }
}
