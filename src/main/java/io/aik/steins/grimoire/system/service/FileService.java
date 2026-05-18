package io.aik.steins.grimoire.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.aik.steins.grimoire.system.common.dto.FileQuery;
import io.aik.steins.grimoire.system.common.vo.FileDownloadResult;
import io.aik.steins.grimoire.system.common.vo.FileVo;
import org.springframework.web.multipart.MultipartFile;

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
     */
    FileDownloadResult download(Long id);

    /**
     * 分页查询
     */
    IPage<FileVo> findPage(FileQuery query);

    /**
     * 根据 ID 查询
     */
    FileVo findById(Long id);

    /**
     * 删除文件
     */
    void remove(Long id);

    /**
     * 重命名文件（只修改显示名称，不影响磁盘存储）
     */
    void rename(Long id, String originalName);
}
