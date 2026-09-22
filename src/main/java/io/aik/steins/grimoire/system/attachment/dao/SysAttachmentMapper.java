package io.aik.steins.grimoire.system.attachment.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.aik.steins.grimoire.system.attachment.po.SysAttachmentPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 通用附件挂载 Mapper -anchor
 *
 * <p>本接口是 {@code aik_sys_attachment} 的<b>唯一</b> DAO（挂载表的单一所有者）。
 * 三条手写 SQL 的存在理由只有一条：{@link SysAttachmentPo#getDelFlag()} 带
 * {@code @TableLogic}，MyBatis-Plus 会把 {@code del_flag = 0} 追加进它生成的所有
 * SELECT / UPDATE 的 WHERE 子句，因此<b>墓碑行（{@code del_flag = 1}）只能由手写 SQL 触及</b>——
 * 而差量的「复活」分支（SDD §2.5.7 第 3 条）恰恰必须命中墓碑行。</p>
 *
 * @author a I k .
 */
@Mapper
public interface SysAttachmentMapper extends BaseMapper<SysAttachmentPo> {

    /**
     * 查某业务下的【全部】挂载行（含已卸载的墓碑行）
     *
     * <p>差量算法只查一次即可同时得到 {@code oldSet}（内存里按 {@code delFlag = 0} 过滤）
     * 与墓碑集合；不得改用 MyBatis-Plus 生成的方法——它们看不到墓碑行。</p>
     *
     * @param bizType 业务类型（白名单取值）
     * @param bizId   业务主键
     * @return 该业务的全部挂载行（有效 + 已卸载），按 {@code sort_order ASC, id ASC}
     */
    @Select("SELECT * FROM aik_sys_attachment "
            + "WHERE biz_type = #{bizType} AND biz_id = #{bizId} "
            + "ORDER BY sort_order ASC, id ASC")
    List<SysAttachmentPo> selectAllByBiz(@Param("bizType") String bizType, @Param("bizId") Long bizId);

    /**
     * 按唯一键 {@code uk_biz_file (biz_type, biz_id, file_id)} 锁定查一行（含墓碑行）
     *
     * <p>用途：{@code insert} 撞 {@code DuplicateKeyException} 后的「复活并重试一次」。
     * 必须是<b>锁定读</b>：并发事务刚提交的行在当前事务的 REPEATABLE READ 快照里读不到，
     * 只有 {@code FOR UPDATE} 能读到最新已提交版本。</p>
     *
     * @param bizType 业务类型
     * @param bizId   业务主键
     * @param fileId  文件对象ID
     * @return 该三元组的挂载行；不存在返回 {@code null}
     */
    @Select("SELECT * FROM aik_sys_attachment "
            + "WHERE biz_type = #{bizType} AND biz_id = #{bizId} AND file_id = #{fileId} "
            + "FOR UPDATE")
    SysAttachmentPo selectByBizAndFileForUpdate(@Param("bizType") String bizType,
                                                @Param("bizId") Long bizId,
                                                @Param("fileId") Long fileId);

    /**
     * 复活墓碑行：把 {@code del_flag} 置回 0
     *
     * <p>必须手写 SQL：逻辑删除会把 {@code del_flag = 0} 追加进 UPDATE 的 WHERE，
     * MyBatis-Plus 的 update 永远选不中墓碑行。字段刷新由随后的
     * {@code updateById} 负责（那条语句会正常命中已复活的行，并由
     * {@code BaseMetaObjectHandler} 填充 {@code modify_time} / {@code modify_by}）。</p>
     *
     * @param id 挂载行ID
     * @return 受影响行数（1 = 复活成功）
     */
    @Update("UPDATE aik_sys_attachment SET del_flag = 0 WHERE id = #{id}")
    int reviveById(@Param("id") Long id);
}
