package io.aik.steins.grimoire.core.storage;

import cn.hutool.core.util.StrUtil;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.OSSObject;
import io.aik.steins.grimoire.core.config.FileStorageConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 阿里云 OSS 文件存储策略
 *
 * @author a I k .
 * @implNote JDK 8
 * @since 2026/08/26
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "grimoire.file", name = "use", havingValue = "oss"
)
public class OssFileStorage extends AbstractFileStorage {

    private final FileStorageConfig fileStorageConfig;

    private OSS ossClient;
    private String bucketName;
    private String basePath;

    @PostConstruct
    public void init() {
        FileStorageConfig.OssConfig ossConfig = fileStorageConfig.getMethod().getOss();
        this.bucketName = ossConfig.getBucketName();
        this.basePath = StrUtil.isNotBlank(ossConfig.getBasePath()) ? ossConfig.getBasePath() : "";
        if (StrUtil.isNotBlank(basePath) && !basePath.endsWith("/")) {
            basePath = basePath + "/";
        }
        this.ossClient = new OSSClientBuilder().build(
                ossConfig.getEndpoint(),
                ossConfig.getAccessKeyId(),
                ossConfig.getAccessKeySecret()
        );
        log.info("OSS 文件存储初始化完成，bucket：{}", bucketName);
    }

    @PreDestroy
    public void destroy() {
        if (ossClient != null) {
            ossClient.shutdown();
        }
    }

    @Override
    public String upload(InputStream inputStream, String originalFilename) throws Exception {
        String storedName = generateStoredName(originalFilename);
        String objectKey = basePath + storedName;
        ossClient.putObject(bucketName, objectKey, inputStream);
        return objectKey;
    }

    @Override
    public byte[] download(String storedPath) throws Exception {
        OSSObject ossObject = ossClient.getObject(bucketName, storedPath);
        try (InputStream inputStream = ossObject.getObjectContent();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
            return outputStream.toByteArray();
        }
    }

    @Override
    public boolean remove(String storedPath) throws Exception {
        ossClient.deleteObject(bucketName, storedPath);
        return true;
    }

    @Override
    public boolean exists(String storedPath) throws Exception {
        return ossClient.doesObjectExist(bucketName, storedPath);
    }

    @Override
    public String getUrl(String storedPath) {
        // 生成临时访问 URL（1小时有效）
        java.util.Date expiration = new java.util.Date(System.currentTimeMillis() + 3600 * 1000L);
        return ossClient.generatePresignedUrl(bucketName, storedPath, expiration).toString();
    }
}
