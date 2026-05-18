package io.aik.steins.grimoire.core.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * -anchor 带逻辑删除的基础实体
 *
 * <p>需要软删除的表继承此类，不需要的继承 {@link BaseEntity}</p>
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
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description = "带逻辑删除的基础实体")
public class BaseLogicEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 删除标志：0-未删除，1-已删除
     */
    @Schema(description = "删除标志：0-未删除，1-已删除")
    @TableField(value = "deleted")
    @TableLogic(value = "0", delval = "1")
    private Integer deleted;
}
