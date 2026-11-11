package io.aik.steins.grimoire.core.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * -anchor Aspose 许可证初始化工具
 * 应用启动时加载 Aspose 许可证，消除 Word/PDF 导出时的评估水印。
 *
 * 许可证不随仓库分发（避免公开泄露商业许可证），读取顺序：
 * 1. 环境变量 ASPOSE_LICENSE_XML（完整 XML 内容）
 * 2. 环境变量 ASPOSE_LICENSE_FILE（XML 文件路径）
 * 3. 本地默认文件 config/aspose-license.xml（已加入 .gitignore）
 * 未配置时仅告警并继续启动，导出结果可能带评估水印。
 *
 * @author aIk.
 * @version 1.1.0
 * @implNote JDK 8
 * @apiNote
 * @since 2026/07/15
 */
@Slf4j
@Component
public class AsposeLicenseUtil {

    private static final String ENV_LICENSE_XML = "ASPOSE_LICENSE_XML";
    private static final String ENV_LICENSE_FILE = "ASPOSE_LICENSE_FILE";
    private static final String DEFAULT_LICENSE_FILE = "config/aspose-license.xml";

    private static volatile boolean licenseLoaded = false;

    @PostConstruct
    public void init() {
        loadLicense();
    }

    /**
     * 确保许可证已加载，供外部调用前校验
     */
    public static void ensureLicenseLoaded() {
        if (!licenseLoaded) {
            loadLicense();
        }
    }

    private static synchronized void loadLicense() {
        if (licenseLoaded) {
            return;
        }
        try (InputStream is = resolveLicenseStream()) {
            if (is == null) {
                log.warn("未配置 Aspose 许可证：请设置环境变量 {}（XML 内容）或 {}（文件路径，默认 {}），否则导出可能带评估水印",
                        ENV_LICENSE_XML, ENV_LICENSE_FILE, DEFAULT_LICENSE_FILE);
                return;
            }
            com.aspose.words.License asposeLicense = new com.aspose.words.License();
            asposeLicense.setLicense(is);
            licenseLoaded = true;
            log.info("Aspose 许可证加载成功");
        } catch (Exception e) {
            log.error("Aspose 许可证加载失败", e);
        }
    }

    /**
     * 解析许可证输入流；未配置时返回 null（调用方告警后继续）。
     */
    private static InputStream resolveLicenseStream() throws Exception {
        String xml = System.getenv(ENV_LICENSE_XML);
        if (xml != null && !xml.trim().isEmpty()) {
            return new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
        }
        String file = System.getenv(ENV_LICENSE_FILE);
        if (file == null || file.trim().isEmpty()) {
            file = DEFAULT_LICENSE_FILE;
        }
        Path path = Paths.get(file);
        if (Files.isReadable(path)) {
            return Files.newInputStream(path);
        }
        return null;
    }
}
