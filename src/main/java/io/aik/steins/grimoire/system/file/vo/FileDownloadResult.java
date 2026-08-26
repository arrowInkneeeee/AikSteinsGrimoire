package io.aik.steins.grimoire.system.file.vo;

import lombok.Data;
import org.springframework.core.io.Resource;

/**
 * 文件下载结果 -anchor
 *
 * @author a I k .
 */
@Data
public class FileDownloadResult {

    private Resource resource;
    private String originalName;
}
