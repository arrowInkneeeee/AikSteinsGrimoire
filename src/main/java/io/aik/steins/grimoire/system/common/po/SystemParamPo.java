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
 * 系统参数 -anchor
 *
 * @author a I k .
 */
@Data
@SuperBuilder
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@TableName("aik_system_param")
public class SystemParamPo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    public SystemParamPo() {
    }

    @TableId(type = IdType.INPUT)
    private Long id;

    private String paramKey;

    private String paramValue;

    private String description;

    private String paramGroup;

    private Integer editable;
}
