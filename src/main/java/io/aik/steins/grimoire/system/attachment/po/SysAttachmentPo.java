package io.aik.steins.grimoire.system.attachment.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.aik.steins.grimoire.core.po.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * -anchor 通用附件挂载（第 2 层：挂载点）
 *
 * <p>一行 = 某业务（{@code bizType}, {@code bizId}）以某名义（{@code attachName}）用某文件（{@code fileId}）。</p>
 *
 * <p>卸载时【保留该行】（{@code delFlag = 1}），以保留 {@code attachName} / {@code description} /
 * {@code sortOrder} 元数据，便于重新挂载时原样复活；有效挂载判定只统计 {@code delFlag = 0}。
 * 它<b>不</b>提供事件历史（复活复用同一行，{@code createTime} 始终是最初挂载时间）。</p>
 *
 * <p>继承约定：必须继承 {@link BaseEntity}，<b>不得</b>改继承 {@code BaseLogicEntity}——
 * 后者的逻辑删除列名是 {@code deleted}，本表不存在该列，改了启动即报 unknown column。
 * 本类是改造后全仓【唯一】带逻辑删除的实体（{@code FileRecordPo} 已摘除）。</p>
 *
 * @author a I k .
 * @version 2.0.0
 * @implNote JDK 8
 * @apiNote 表结构权威见 SDD §2.2 / §2.4；本实体不承载任何访问地址字段（url 链路已退役）
 * @since 2026/05/18
 * -
 */
@Data
@SuperBuilder
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description = "通用附件挂载")
@TableName("aik_sys_attachment")
public class SysAttachmentPo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 有效挂载标记值
     */
    public static final Integer DEL_FLAG_EFFECTIVE = 0;

    /**
     * 已卸载标记值
     */
    public static final Integer DEL_FLAG_UNLOADED = 1;

    //anchor 注：Lombok @SuperBuilder 需要显式无参构造，删除字段时不要误删
    public SysAttachmentPo() {
        super();
    }

    /**
     * 主键ID
     */
    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * 被挂载的文件对象ID（逻辑关联 aik_sys_file.id）
     */
    @Schema(description = "被挂载的文件对象ID")
    @TableField("file_id")
    private Long fileId;

    /**
     * 业务类型（取值白名单见 AttachmentBizType，当前仅 knowledge）
     */
    @Schema(description = "业务类型：knowledge")
    @TableField("biz_type")
    private String bizType;

    /**
     * 业务主键（biz_type = knowledge 时为 aik_knowledge.id）
     */
    @Schema(description = "业务主键")
    @TableField("biz_id")
    private Long bizId;

    /**
     * 用户可见文件名（权威归属【挂载层】）
     */
    @Schema(description = "用户可见文件名（权威）")
    @TableField("attach_name")
    private String attachName;

    /**
     * 描述
     */
    @Schema(description = "描述")
    @TableField("description")
    private String description;

    /**
     * 排序号
     */
    @Schema(description = "排序号")
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 卸载标记：0-有效挂载，1-已卸载
     *
     * <p>统一用 {@code mapper.deleteById(id)} / {@code service.removeById(id)} 置位，
     * <b>不要</b>手写 {@code set(delFlag, 1)}——手写会与 {@link TableLogic} 的语义重复。</p>
     */
    @Schema(description = "卸载标记：0-有效挂载，1-已卸载")
    @TableField("del_flag")
    @TableLogic
    private Integer delFlag;
}
