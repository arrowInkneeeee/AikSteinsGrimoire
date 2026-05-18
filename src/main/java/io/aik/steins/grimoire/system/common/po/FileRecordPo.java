package io.aik.steins.grimoire.system.common.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.aik.steins.grimoire.core.po.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * -anchor 文件记录
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since 2026/05/15
 * -
 */
@Data
@SuperBuilder
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description = "文件记录")
@TableName("aik_file_record")
public class FileRecordPo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    public FileRecordPo() {
        super();
    }

    /**
     * 主键ID
     */
    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * 原始文件名
     */
    @Schema(description = "原始文件名")
    @TableField("original_name")
    private String originalName;

    /**
     * 存储文件名
     */
    @Schema(description = "存储文件名")
    @TableField("stored_name")
    private String storedName;

    /**
     * 文件路径
     */
    @Schema(description = "文件路径")
    @TableField("file_path")
    private String filePath;

    /**
     * 文件大小（字节）
     */
    @Schema(description = "文件大小（字节）")
    @TableField("file_size")
    private Long fileSize;

    /**
     * 文件类型
     */
    @Schema(description = "文件类型")
    @TableField("file_type")
    private String fileType;

    /**
     * 下载次数
     */
    @Schema(description = "下载次数")
    @TableField("download_count")
    private Integer downloadCount;
}
