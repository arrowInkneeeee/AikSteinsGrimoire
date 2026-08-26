package io.aik.steins.grimoire.system.file.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.aik.steins.grimoire.core.config.FileStorageConfig;
import io.aik.steins.grimoire.core.exception.BusinessException;
import io.aik.steins.grimoire.core.storage.FileStorageStrategy;
import io.aik.steins.grimoire.core.utils.AssertUtils;
import io.aik.steins.grimoire.system.file.dto.FileQuery;
import io.aik.steins.grimoire.system.file.po.FileRecordPo;
import io.aik.steins.grimoire.system.file.vo.FileVo;
import io.aik.steins.grimoire.system.file.dao.FileMapper;
import io.aik.steins.grimoire.system.file.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 文件 Service 实现 -anchor
 *
 * @author a I k .
 */
@Slf4j
@Service("system.FileService")
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final FileMapper fileMapper;
    private final FileStorageConfig fileStorageConfig;
    private final FileStorageStrategy fileStorageStrategy;

    @Override
    public FileVo upload(MultipartFile file) {
        AssertUtils.notNull(file, "文件不能为空");
        AssertUtils.isTrue(!file.isEmpty(), "文件不能为空");
        AssertUtils.isTrue(file.getSize() <= fileStorageConfig.getMaxSize(),
                "文件大小不能超过" + (fileStorageConfig.getMaxSize() / 1024 / 1024) + "MB");

        //anchor 校验文件类型白名单
        if (Boolean.TRUE.equals(fileStorageConfig.getTypeCheckEnabled())
                && StrUtil.isNotBlank(fileStorageConfig.getAllowTypes())) {
            String contentType = StrUtil.nullToEmpty(file.getContentType()).toLowerCase();
            String ext = StrUtil.nullToEmpty(FileUtil.extName(file.getOriginalFilename())).toLowerCase();
            boolean allowed = fileStorageConfig.getAllowTypeSet().contains(contentType)
                    || fileStorageConfig.getAllowTypeSet().contains(ext);
            AssertUtils.isTrue(allowed, "不支持的文件类型");
        }

        String originalName = file.getOriginalFilename();
        AssertUtils.notEmpty(originalName, "文件名不能为空");

        try {
            //anchor 计算MD5
            String md5 = cn.hutool.crypto.digest.DigestUtil.md5Hex(file.getInputStream());

            //anchor 使用策略上传
            String storedPath = fileStorageStrategy.upload(file.getInputStream(), originalName);
            String url = fileStorageStrategy.getUrl(storedPath);

            //anchor 保存记录
            FileRecordPo po = new FileRecordPo();
            po.setId(IdUtil.getSnowflakeNextId());
            po.setOriginalName(originalName);
            po.setStoredName(FileUtil.getName(storedPath));
            po.setFilePath(storedPath);
            po.setFileSize(file.getSize());
            po.setFileType(file.getContentType());
            po.setStorageType(fileStorageConfig.getUse());
            po.setMd5(md5);
            po.setUrl(url);
            po.setDownloadCount(0);
            fileMapper.insert(po);

            return FileVo.of(po);
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException("文件上传失败");
        } catch (Exception e) {
            log.error("文件存储失败", e);
            throw new BusinessException("文件存储失败：" + e.getMessage());
        }
    }

    @Override
    public void download(Long id, HttpServletResponse response, boolean preview) {
        FileRecordPo po = fileMapper.selectById(id);
        AssertUtils.notNull(po, "文件不存在");

        try {
            //anchor 使用策略下载（传入原始文件名用于Content-Disposition）
            fileStorageStrategy.download(response, po.getFilePath(), po.getOriginalName(), preview);

            //anchor 更新下载次数
            fileMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<FileRecordPo>()
                            .eq(FileRecordPo::getId, id)
                            .set(FileRecordPo::getDownloadCount, po.getDownloadCount() + 1));
        } catch (Exception e) {
            log.error("文件下载失败", e);
            throw new BusinessException("文件下载失败");
        }
    }

    @Override
    public IPage<FileVo> findPage(FileQuery query) {
        LambdaQueryWrapper<FileRecordPo> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(query.getOriginalName()), FileRecordPo::getOriginalName, query.getOriginalName())
                .orderByDesc(FileRecordPo::getCreateTime);

        return fileMapper.selectPage(query.toPage(), wrapper).convert(FileVo::of);
    }

    @Override
    public FileVo findById(Long id) {
        FileRecordPo po = fileMapper.selectById(id);
        AssertUtils.notNull(po, "文件不存在");
        return FileVo.of(po);
    }

    @Override
    public void rename(Long id, String originalName) {
        AssertUtils.notNull(id, "文件ID不能为空");
        AssertUtils.notEmpty(originalName, "文件名不能为空");
        FileRecordPo po = fileMapper.selectById(id);
        AssertUtils.notNull(po, "文件不存在");

        //anchor 只修改显示名称，磁盘存储名和路径不变
        po.setOriginalName(originalName);
        fileMapper.updateById(po);
    }

    @Override
    public void remove(Long id) {
        FileRecordPo po = fileMapper.selectById(id);
        AssertUtils.notNull(po, "文件不存在");

        //anchor 逻辑删除（@TableLogic 自动处理）
        fileMapper.deleteById(id);
    }
}
