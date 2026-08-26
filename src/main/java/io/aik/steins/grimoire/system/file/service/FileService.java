package io.aik.steins.grimoire.system.file.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.aik.steins.grimoire.system.file.dto.FileQuery;
import io.aik.steins.grimoire.system.file.vo.FileVo;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;

/**
 * 文件 Service -anchor
 *
 * @author a I k .
 */
public interface FileService {

    /**
     * 上传文件
     */
    FileVo upload(MultipartFile file);

    /**
     * 下载文件
     *
     * @param id       文件ID
     * @param response HTTP响应对象
     * @param preview  是否预览模式
     */
    void download(Long id, HttpServletResponse response, boolean preview);

    /**
     * 分页查询
     */
    IPage<FileVo> findPage(FileQuery query);

    /**
     * 根据 ID 查询
     */
    FileVo findById(Long id);

    /**
     * 删除文件（逻辑删除）
     */
    void remove(Long id);

    /**
     * 重命名文件（只修改显示名称，不影响磁盘存储）
     */
    void rename(Long id, String originalName);
}
