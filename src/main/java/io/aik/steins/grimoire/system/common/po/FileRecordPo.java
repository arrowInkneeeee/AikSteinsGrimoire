package io.aik.steins.grimoire.system.common.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.aik.steins.grimoire.core.po.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * 文件记录 -anchor
 *
 * @author a I k .
 */
@Data
@SuperBuilder
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@TableName("aik_file_record")
public class FileRecordPo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    public FileRecordPo() {
    }

    @TableId(type = IdType.INPUT)
    private Long id;

    private String originalName;

    private String storedName;

    private String filePath;

    private Long fileSize;

    private String fileType;

    private Integer downloadCount;
}
