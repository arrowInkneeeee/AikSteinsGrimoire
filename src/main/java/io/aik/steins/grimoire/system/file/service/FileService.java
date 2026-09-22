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
     * @param attachId 挂载行ID（可空）。**挂载层发起必须带**：服务端校验该挂载行存在、
     *                 {@code del_flag = 0} 且其 {@code file_id == id}，响应文件名取该行的 {@code attach_name}；
     *                 台账模式（{@code null}）不校验挂载，响应文件名取 {@code aik_sys_file.original_name}
     * @param response HTTP响应对象
     * @param preview  是否预览模式
     */
    void download(Long id, Long attachId, HttpServletResponse response, boolean preview);

    /**
     * 分页查询
     */
    IPage<FileVo> findPage(FileQuery query);

    /**
     * 根据 ID 查询
     */
    FileVo findById(Long id);

    /**
     * 删除文件（**物理删除**；仍被有效挂载引用则拒绝，文件不存在则幂等 no-op）
     */
    void remove(Long id);

    /**
     * 重命名文件（只修改显示名称，不影响磁盘存储）
     */
    void rename(Long id, String originalName);
}
