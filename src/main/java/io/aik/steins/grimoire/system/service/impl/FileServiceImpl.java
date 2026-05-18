package io.aik.steins.grimoire.system.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.aik.steins.grimoire.core.config.FileStorageConfig;
import io.aik.steins.grimoire.core.exception.BusinessException;
import io.aik.steins.grimoire.core.utils.AssertUtils;
import io.aik.steins.grimoire.system.common.dto.FileQuery;
import io.aik.steins.grimoire.system.common.po.FileRecordPo;
import io.aik.steins.grimoire.system.common.vo.FileDownloadResult;
import io.aik.steins.grimoire.system.common.vo.FileVo;
import io.aik.steins.grimoire.system.dao.FileMapper;
import io.aik.steins.grimoire.system.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

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
        String ext = FileUtil.extName(originalName);
        String storedName = IdUtil.simpleUUID() + (StrUtil.isNotBlank(ext) ? "." + ext : "");
        String relativePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String basePath = fileStorageConfig.getEffectiveBasePath();
        String fullDir = basePath + File.separator + relativePath;
        String fullPath = fullDir + File.separator + storedName;

        //anchor 创建目录
        FileUtil.mkdir(fullDir);

        //anchor 写入文件
        try {
            file.transferTo(new File(fullPath));
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException("文件上传失败");
        }

        //anchor 保存记录
        FileRecordPo po = new FileRecordPo();
        po.setId(IdUtil.getSnowflakeNextId());
        po.setOriginalName(originalName);
        po.setStoredName(storedName);
        po.setFilePath(relativePath);
        po.setFileSize(file.getSize());
        po.setFileType(file.getContentType());
        po.setDownloadCount(0);
        fileMapper.insert(po);

        return FileVo.of(po);
    }

    @Override
    public FileDownloadResult download(Long id) {
        FileRecordPo po = fileMapper.selectById(id);
        AssertUtils.notNull(po, "文件不存在");

        String fullPath = fileStorageConfig.getEffectiveBasePath() + File.separator + po.getFilePath() + File.separator + po.getStoredName();
        File file = new File(fullPath);
        AssertUtils.isTrue(FileUtil.exist(file), "文件不存在");

        //anchor 更新下载次数
        fileMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<FileRecordPo>()
                        .eq(FileRecordPo::getId, id)
                        .set(FileRecordPo::getDownloadCount, po.getDownloadCount() + 1));

        try {
            FileDownloadResult result = new FileDownloadResult();
            result.setResource(new InputStreamResource(new FileInputStream(file)));
            result.setOriginalName(po.getOriginalName());
            return result;
        } catch (IOException e) {
            log.error("文件读取失败", e);
            throw new BusinessException("文件读取失败");
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

        //anchor 删除磁盘文件（如果存在）
        String fullPath = fileStorageConfig.getEffectiveBasePath() + File.separator + po.getFilePath() + File.separator + po.getStoredName();
        if (FileUtil.exist(fullPath)) {
            FileUtil.del(fullPath);
        }

        //anchor 删除数据库记录
        fileMapper.deleteById(id);
    }
}
