package io.aik.steins.grimoire.core.storage;

import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;

/**
 * 文件存储策略接口
 * <p>定义统一的文件上传、下载、删除操作，具体实现由各存储策略负责</p>
 *
 * @author a I k .
 * @implNote JDK 8
 * @since 2026/08/26
 */
public interface FileStorageStrategy {

    /**
     * 上传文件（最底层接口）
     *
     * @param inputStream    文件输入流
     * @param originalFilename 原始文件名
     * @return 存储路径标识
     * @throws Exception 上传异常
     */
    String upload(InputStream inputStream, String originalFilename) throws Exception;

    /**
     * 上传文件（便捷方法，MultipartFile 转 InputStream）
     *
     * @param file MultipartFile
     * @return 存储路径标识
     * @throws Exception 上传异常
     */
    String upload(MultipartFile file) throws Exception;

    /**
     * 下载文件为字节数组
     *
     * @param storedPath 存储路径标识
     * @return 文件字节数组
     * @throws Exception 下载异常
     */
    byte[] download(String storedPath) throws Exception;

    /**
     * 下载文件并直接写入 HttpServletResponse
     *
     * @param response   响应对象
     * @param storedPath 存储路径标识
     * @throws Exception 下载异常
     */
    void download(HttpServletResponse response, String storedPath) throws Exception;

    /**
     * 下载文件并直接写入 HttpServletResponse（支持指定原始文件名和预览模式）
     *
     * @param response     响应对象
     * @param storedPath   存储路径标识
     * @param originalName 原始文件名
     * @param preview      是否预览模式
     * @throws Exception 下载异常
     */
    void download(HttpServletResponse response, String storedPath,
                  String originalName, boolean preview) throws Exception;

    /**
     * 删除文件
     *
     * @param storedPath 存储路径标识
     * @return 是否删除成功
     * @throws Exception 删除异常
     */
    boolean remove(String storedPath) throws Exception;

    /**
     * 检查文件是否存在
     *
     * @param storedPath 存储路径标识
     * @return 是否存在
     * @throws Exception 检查异常
     */
    boolean exists(String storedPath) throws Exception;

    /**
     * 获取文件访问 URL
     *
     * @param storedPath 存储路径标识
     * @return 访问 URL（本地返回相对路径，OSS 返回公网地址）
     */
    String getUrl(String storedPath);
}
