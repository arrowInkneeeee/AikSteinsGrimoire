package io.aik.steins.grimoire.system.common.vo;

import cn.hutool.core.bean.BeanUtil;
import io.aik.steins.grimoire.system.common.po.FileRecordPo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件视图 -anchor
 *
 * @author a I k .
 */
@Data
@Schema(description = "文件视图")
public class FileVo {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "原始文件名")
    private String originalName;

    @Schema(description = "存储文件名")
    private String storedName;

    @Schema(description = "文件路径")
    private String filePath;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @Schema(description = "MIME类型")
    private String fileType;

    @Schema(description = "下载次数")
    private Integer downloadCount;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    public static FileVo of(FileRecordPo po) {
        if (po == null) {
            return null;
        }
        FileVo vo = new FileVo();
        BeanUtil.copyProperties(po, vo);
        return vo;
    }
}
