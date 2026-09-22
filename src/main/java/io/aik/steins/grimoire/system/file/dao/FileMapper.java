package io.aik.steins.grimoire.system.file.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.aik.steins.grimoire.system.file.po.FileRecordPo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 文件记录 Mapper -anchor
 *
 * <p>⚠️ 其中两条查询读的是附件挂载表 {@code aik_sys_attachment}（{@link #countEffectiveMounts} /
 * {@link #selectAttachNameForDownload}）：下载文件名（SDD §2.5.6 方案 1）与删除引用计数（SDD §2.5.7）
 * 都发生在文件模块的边界上，而统一的挂载服务尚未落盘，故暂放此处。
 * 待统一挂载服务落地后，这两条应改为经它收口，并删除本处 SQL（避免挂载表出现第二个所有者）。</p>
 *
 * @author a I k .
 */
public interface FileMapper extends BaseMapper<FileRecordPo> {

    /**
     * 按 md5 查重（秒传的内容寻址锚点）
     *
     * <p>真库 {@code idx_md5} 是【非唯一】索引（NON_UNIQUE=1），同一 md5 多行是并发窗口下的常态，
     * 因此必须把 {@code ORDER BY id ASC LIMIT 1} 钉在 SQL 里取最早那行作规范行；
     * <b>不得</b>改用 {@code selectOne}——多行时会抛 {@code TooManyResultsException}。</p>
     *
     * @param md5 文件内容 md5
     * @return 最早的一条文件记录；无命中返回 {@code null}
     */
    @Select("SELECT * FROM aik_sys_file WHERE md5 = #{md5} ORDER BY id ASC LIMIT 1")
    FileRecordPo selectLatestByMd5(@Param("md5") String md5);

    /**
     * 锁定查询文件行——删除路径的【第一条语句】（与挂载写入路径锁序一致）
     *
     * <p>必须在事务内调用：它串行化「卸载最后一个引用」与「新建挂载」两条路径，
     * 堵住"读到即将被删除的文件行 → 插入悬空挂载"的窗口。</p>
     *
     * @param id 文件ID
     * @return 文件记录；行不存在返回 {@code null}
     */
    @Select("SELECT * FROM aik_sys_file WHERE id = #{id} FOR UPDATE")
    FileRecordPo selectByIdForUpdate(@Param("id") Long id);

    /**
     * 统计某文件的有效挂载数
     *
     * @param fileId 文件ID
     * @return {@code del_flag = 0} 的挂载行数
     */
    @Select("SELECT COUNT(*) FROM aik_sys_attachment WHERE file_id = #{fileId} AND del_flag = 0")
    long countEffectiveMounts(@Param("fileId") Long fileId);

    /**
     * 取下载用的挂载层文件名——一次查询同时完成三项校验
     *
     * <p>等价于「该挂载行存在 <b>且</b> {@code del_flag = 0} <b>且</b> {@code file_id == fileId}」，
     * 任一不满足即返回 {@code null}（调用方按拒绝处理）。</p>
     *
     * <p>只 {@code SELECT} 需要的列，不依赖 {@code SysAttachmentPo}——其字段映射仍指向迁移前的
     * {@code knowledge_id} / {@code attach_url}，与真库结构不一致。</p>
     *
     * @param attachId 挂载行ID
     * @param fileId   文件ID
     * @return 挂载层的 {@code attach_name}；校验不通过返回 {@code null}
     */
    @Select("SELECT attach_name FROM aik_sys_attachment "
            + "WHERE id = #{attachId} AND del_flag = 0 AND file_id = #{fileId}")
    String selectAttachNameForDownload(@Param("attachId") Long attachId, @Param("fileId") Long fileId);
}
