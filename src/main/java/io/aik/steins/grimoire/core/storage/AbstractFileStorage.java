package io.aik.steins.grimoire.core.storage;

import cn.hutool.core.lang.Singleton;
import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 文件存储抽象模板
 * <p>封装公共逻辑：Snowflake 文件名生成、Content-Type 映射、下载响应构建</p>
 * <p>子类只需实现真正的存储操作（doUpload/doDownload/doRemove/doExists）</p>
 *
 * @author a I k .
 * @implNote JDK 8
 * @since 2026/08/26
 */
@Slf4j
public abstract class AbstractFileStorage implements FileStorageStrategy {

    private static final Snowflake SNOWFLAKE = Singleton.get(Snowflake.class, 1L, 1L, true);

    /**
     * Content-Type 映射表
     */
    private static final Map<String, String> CONTENT_TYPE_MAP = new HashMap<>();

    static {
        // 图片
        CONTENT_TYPE_MAP.put("jpg", "image/jpeg");
        CONTENT_TYPE_MAP.put("jpeg", "image/jpeg");
        CONTENT_TYPE_MAP.put("png", "image/png");
        CONTENT_TYPE_MAP.put("gif", "image/gif");
        CONTENT_TYPE_MAP.put("bmp", "image/bmp");
        CONTENT_TYPE_MAP.put("ico", "image/x-icon");
        CONTENT_TYPE_MAP.put("webp", "image/webp");
        CONTENT_TYPE_MAP.put("svg", "image/svg+xml");

        // 音频
        CONTENT_TYPE_MAP.put("mp3", "audio/mpeg");
        CONTENT_TYPE_MAP.put("wav", "audio/wav");
        CONTENT_TYPE_MAP.put("ogg", "audio/ogg");
        CONTENT_TYPE_MAP.put("m4a", "audio/mp4");

        // 视频
        CONTENT_TYPE_MAP.put("mp4", "video/mp4");
        CONTENT_TYPE_MAP.put("webm", "video/webm");
        CONTENT_TYPE_MAP.put("ogg-video", "video/ogg");
        CONTENT_TYPE_MAP.put("avi", "video/x-msvideo");
        CONTENT_TYPE_MAP.put("mov", "video/quicktime");
        CONTENT_TYPE_MAP.put("mkv", "video/x-matroska");

        // 文档
        CONTENT_TYPE_MAP.put("pdf", "application/pdf");
        CONTENT_TYPE_MAP.put("doc", "application/msword");
        CONTENT_TYPE_MAP.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        CONTENT_TYPE_MAP.put("xls", "application/vnd.ms-excel");
        CONTENT_TYPE_MAP.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        CONTENT_TYPE_MAP.put("ppt", "application/vnd.ms-powerpoint");
        CONTENT_TYPE_MAP.put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        CONTENT_TYPE_MAP.put("txt", "text/plain");
        CONTENT_TYPE_MAP.put("html", "text/html");
        CONTENT_TYPE_MAP.put("htm", "text/html");
        CONTENT_TYPE_MAP.put("css", "text/css");
        CONTENT_TYPE_MAP.put("js", "application/javascript");
        CONTENT_TYPE_MAP.put("json", "application/json");
        CONTENT_TYPE_MAP.put("xml", "application/xml");
        CONTENT_TYPE_MAP.put("md", "text/markdown");
        CONTENT_TYPE_MAP.put("csv", "text/csv");

        // 压缩包
        CONTENT_TYPE_MAP.put("zip", "application/zip");
        CONTENT_TYPE_MAP.put("rar", "application/x-rar-compressed");
        CONTENT_TYPE_MAP.put("tar", "application/x-tar");
        CONTENT_TYPE_MAP.put("gz", "application/gzip");
        CONTENT_TYPE_MAP.put("7z", "application/x-7z-compressed");
    }

    /**
     * 生成存储文件名：Snowflake ID + 保留原始扩展名
     */
    protected String generateStoredName(String originalFilename) {
        if (StrUtil.isBlank(originalFilename)) {
            return String.valueOf(SNOWFLAKE.nextId());
        }
        String ext = extractExt(originalFilename);
        if (StrUtil.isBlank(ext)) {
            return String.valueOf(SNOWFLAKE.nextId());
        }
        return SNOWFLAKE.nextId() + "." + ext;
    }

    /**
     * 提取文件扩展名
     */
    protected String extractExt(String filename) {
        if (StrUtil.isBlank(filename) || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * 从路径中提取文件名
     */
    protected String extractFileName(String filePath) {
        if (StrUtil.isBlank(filePath)) {
            return "";
        }
        String normalized = filePath.replace("\\", "/");
        int lastSlash = normalized.lastIndexOf("/");
        return lastSlash >= 0 ? normalized.substring(lastSlash + 1) : normalized;
    }

    /**
     * 根据文件名解析 Content-Type
     */
    protected String resolveContentType(String filename) {
        String ext = extractExt(filename);
        return CONTENT_TYPE_MAP.getOrDefault(ext, "application/octet-stream");
    }

    /**
     * 构建下载响应
     *
     * @param response     响应对象
     * @param data         文件数据
     * @param filename     原始文件名（用于 Content-Disposition）
     * @param preview      是否预览模式（true=inline, false=attachment）
     */
    protected void buildDownloadResponse(HttpServletResponse response, byte[] data,
                                          String filename, boolean preview) {
        try {
            String contentType = resolveContentType(filename);
            String disposition = preview ? "inline" : "attachment";
            String encodedName = java.net.URLEncoder.encode(filename, "UTF-8").replace("+", "%20");

            response.setContentType(contentType);
            response.setHeader("Content-Disposition", disposition + ";filename*=UTF-8''" + encodedName);
            response.setContentLength(data.length);

            ServletOutputStream outputStream = response.getOutputStream();
            outputStream.write(data);
            outputStream.flush();
        } catch (IOException e) {
            log.error("构建下载响应失败", e);
            throw new RuntimeException("文件下载失败");
        }
    }

    @Override
    public String upload(MultipartFile file) throws Exception {
        return upload(file.getInputStream(), file.getOriginalFilename());
    }

    @Override
    public void download(HttpServletResponse response, String storedPath) throws Exception {
        byte[] data = download(storedPath);
        String filename = extractFileName(storedPath);
        buildDownloadResponse(response, data, filename, false);
    }

    /**
     * 下载到 Response（支持预览模式）
     */
    public void download(HttpServletResponse response, String storedPath, boolean preview) throws Exception {
        byte[] data = download(storedPath);
        String filename = extractFileName(storedPath);
        buildDownloadResponse(response, data, filename, preview);
    }

    /**
     * 下载到 Response（支持指定原始文件名和预览模式）
     */
    public void download(HttpServletResponse response, String storedPath,
                          String originalName, boolean preview) throws Exception {
        byte[] data = download(storedPath);
        String filename = StrUtil.isNotBlank(originalName) ? originalName : extractFileName(storedPath);
        buildDownloadResponse(response, data, filename, preview);
    }
}
