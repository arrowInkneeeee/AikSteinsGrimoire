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
 * <p>支持多存储策略切换：local / oss / sftp</p>
 *
 * @author a I k .
 */
@Data
@Component
@ConfigurationProperties(prefix = "grimoire.file")
public class FileStorageConfig {

    /**
     * 存储策略：local | oss | sftp
     */
    private String use = "local";

    /**
     * 临时文件目录
     */
    private String localTmp = "./grimoire-files/tmp";

    /**
     * 单个文件最大大小（字节），默认 10MB
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
     * 兼容旧配置：文件存储根路径（通用回退）
     */
    private String basePath = "./grimoire-files";

    /**
     * 策略专属配置
     */
    private MethodConfig method;

    // ==================== 便捷方法 ====================

    /**
     * 获取文件存储根路径（兼容旧配置）
     */
    public String getEffectiveBasePath() {
        return basePath;
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

    // ==================== 嵌套配置类 ====================

    @Data
    public static class MethodConfig {
        private LocalConfig local;
        private OssConfig oss;
        private SftpConfig sftp;
    }

    @Data
    public static class LocalConfig {
        private String basePath;
    }

    @Data
    public static class OssConfig {
        private String endpoint;
        private String accessKeyId;
        private String accessKeySecret;
        private String bucketName;
        private String basePath;
    }

    @Data
    public static class SftpConfig {
        private String host;
        private Integer port = 22;
        private String username;
        private String password;
        private String privateKey;
        private String basePath;
    }
}
