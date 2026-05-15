package io.aik.steins.grimoire.core.po;

import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * 带逻辑删除的基础实体 -anchor
 *
 * <p>需要软删除的表继承此类，不需要的继承 {@link BaseEntity}</p>
 *
 * @author a I k .
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class BaseLogicEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 删除标志：0-未删除，1-已删除
     */
    @TableLogic(value = "0", delval = "1")
    private Integer deleted;
}
