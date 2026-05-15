package io.aik.steins.grimoire.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.aik.steins.grimoire.system.common.dto.FileQuery;
import io.aik.steins.grimoire.system.common.vo.FileVo;
import org.springframework.core.io.Resource;
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
    Resource download(Long id);

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
}
