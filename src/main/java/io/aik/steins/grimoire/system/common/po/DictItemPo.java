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
 * 字典项 -anchor
 *
 * @author a I k .
 */
@Data
@SuperBuilder
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@TableName("aik_dict_item")
public class DictItemPo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    public DictItemPo() {
    }

    @TableId(type = IdType.INPUT)
    private Long id;

    private String dictCode;

    private String itemCode;

    private String itemName;

    private Integer sortOrder;

    private Integer status;

    private String remark;
}
