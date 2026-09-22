package io.aik.steins.grimoire.system.attachment.service.impl;

import io.aik.steins.grimoire.core.exception.BusinessException;
import io.aik.steins.grimoire.system.attachment.constant.AttachmentBizType;
import io.aik.steins.grimoire.system.attachment.dao.SysAttachmentMapper;
import io.aik.steins.grimoire.system.attachment.dto.AttachmentDto;
import io.aik.steins.grimoire.system.attachment.po.SysAttachmentPo;
import io.aik.steins.grimoire.system.attachment.vo.AttachmentVo;
import io.aik.steins.grimoire.system.file.dao.FileMapper;
import io.aik.steins.grimoire.system.file.po.FileRecordPo;
import io.aik.steins.grimoire.system.file.service.FileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AttachmentServiceImpl 单元测试 -anchor
 *
 * <p>设施是<b>纯 Mockito</b>（无 Spring 上下文）。因此本类只能证明<b>服务层编排</b>：
 * 差量分类（卸载 / 复活 / 更新 / 新增）、加锁读的顺序、批量取元数据（禁 N+1）、
 * 以及"卸载时只在数到 0 个有效挂载才委派文件层删除"。
 * <b>SQL 层语义</b>（{@code @TableLogic} 追加的 {@code del_flag = 0}、{@code ORDER BY sort_order ASC, id ASC}、
 * 唯一键 {@code uk_biz_file} 的并发行为）由 MyBatis-Plus 生成，需真库运行时验证（交 t9）。</p>
 *
 * @author a I k .
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("通用附件挂载服务测试")
class AttachmentServiceImplTest {

    private static final String BIZ_TYPE = AttachmentBizType.KNOWLEDGE;
    private static final Long BIZ_ID = 9001L;
    private static final Long ATTACH_ID = 3001L;
    private static final Long FILE_ID = 2001L;
    private static final Long OTHER_FILE_ID = 2002L;

    @Mock
    private SysAttachmentMapper sysAttachmentMapper;

    @Mock
    private FileMapper fileMapper;

    @Mock
    private FileService fileService;

    @InjectMocks
    private AttachmentServiceImpl attachmentService;

    @Nested
    @DisplayName("按业务查询（findByBiz）")
    class ListByBizTest {

        @Test
        @DisplayName("保留 SQL 排序并批量取文件元数据：一次 IN 查询，无 N+1")
        void listByBiz_enrichesFileMetaWithSingleBatchQuery() {
            // -anchor given：mapper 返回的顺序即 SQL 的 ORDER BY sort_order ASC, id ASC 结果
            SysAttachmentPo first = buildMount(11L, FILE_ID, 0, "方案.pdf", "第一版", 0);
            SysAttachmentPo second = buildMount(12L, OTHER_FILE_ID, 0, "截图.png", null, 1);
            when(sysAttachmentMapper.selectList(any())).thenReturn(Arrays.asList(first, second));
            when(fileMapper.selectBatchIds(any())).thenReturn(Arrays.asList(
                    buildFile(FILE_ID, 1024L, "application/pdf"),
                    buildFile(OTHER_FILE_ID, 2048L, "image/png")));

            // -anchor when
            List<AttachmentVo> result = attachmentService.listByBiz(BIZ_TYPE, BIZ_ID);

            // -anchor then：顺序 = SQL 顺序，不重排
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getId()).isEqualTo(11L);
            assertThat(result.get(1).getId()).isEqualTo(12L);
            // 挂载层字段
            assertThat(result.get(0).getAttachName()).isEqualTo("方案.pdf");
            assertThat(result.get(0).getDescription()).isEqualTo("第一版");
            assertThat(result.get(0).getSortOrder()).isEqualTo(0);
            // 文件层字段（由 fileId 关联带出，字节数为字符串形式）
            assertThat(result.get(0).getFileSize()).isEqualTo("1024");
            assertThat(result.get(0).getFileType()).isEqualTo("application/pdf");
            assertThat(result.get(1).getFileSize()).isEqualTo("2048");
            // 禁 N+1：只允许一次批量查询，不得逐条 selectById
            verify(fileMapper, times(1)).selectBatchIds(any());
            verify(fileMapper, never()).selectById(any());
        }

        @Test
        @DisplayName("无挂载时返回空列表且不查文件表")
        void listByBiz_noMounts_returnsEmptyListWithoutFileQuery() {
            // -anchor given
            when(sysAttachmentMapper.selectList(any())).thenReturn(Collections.emptyList());

            // -anchor when
            List<AttachmentVo> result = attachmentService.listByBiz(BIZ_TYPE, BIZ_ID);

            // -anchor then
            assertThat(result).isEmpty();
            verify(fileMapper, never()).selectBatchIds(any());
        }

        @Test
        @DisplayName("未识别的 bizType 抛 BusinessException（白名单）")
        void listByBiz_unknownBizType_throws() {
            // -anchor given & when & then
            assertThatThrownBy(() -> attachmentService.listByBiz("unknown", BIZ_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("未识别的业务类型");
            verify(sysAttachmentMapper, never()).selectList(any());
        }

        @Test
        @DisplayName("bizId 为空抛 BusinessException")
        void listByBiz_nullBizId_throws() {
            // -anchor given & when & then
            assertThatThrownBy(() -> attachmentService.listByBiz(BIZ_TYPE, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("业务ID不能为空");
        }
    }

    @Nested
    @DisplayName("差量保存（save）")
    class SaveTest {

        @Test
        @DisplayName("新增：无既有行时插入一行，fileId/bizType/bizId/sortOrder 缺省 0 均正确")
        void save_newItem_insertsRow() {
            // -anchor given
            when(fileMapper.selectByIdForUpdate(FILE_ID)).thenReturn(buildFile(FILE_ID, 1L, "application/pdf"));
            when(sysAttachmentMapper.selectAllByBiz(BIZ_TYPE, BIZ_ID)).thenReturn(Collections.emptyList());

            // -anchor when
            attachmentService.save(BIZ_TYPE, BIZ_ID, Collections.singletonList(item(null, FILE_ID, "笔记.pdf", null, null)));

            // -anchor then
            ArgumentCaptor<SysAttachmentPo> captor = ArgumentCaptor.forClass(SysAttachmentPo.class);
            verify(sysAttachmentMapper).insert(captor.capture());
            SysAttachmentPo inserted = captor.getValue();
            assertThat(inserted.getId()).isNotNull();
            assertThat(inserted.getFileId()).isEqualTo(FILE_ID);
            assertThat(inserted.getBizType()).isEqualTo(BIZ_TYPE);
            assertThat(inserted.getBizId()).isEqualTo(BIZ_ID);
            assertThat(inserted.getAttachName()).isEqualTo("笔记.pdf");
            assertThat(inserted.getSortOrder()).isZero();
            assertThat(inserted.getDelFlag()).isEqualTo(SysAttachmentPo.DEL_FLAG_EFFECTIVE);
            verify(sysAttachmentMapper, never()).reviveById(anyLong());
            verify(sysAttachmentMapper, never()).deleteById(any(Long.class));
        }

        @Test
        @DisplayName("差量重复提交幂等：同一列表提交两次，第二次零写入（不新增、不卸载、不复活、不更新）")
        void save_samePayloadTwice_secondCallWritesNothing() {
            // -anchor given：第一次空表 → 插入；第二次读回刚插入的那一行（按 fileId 匹配，无需入参带 id）
            AttachmentDto payload = item(null, FILE_ID, "笔记.pdf", "说明", 0);
            when(fileMapper.selectByIdForUpdate(FILE_ID)).thenReturn(buildFile(FILE_ID, 1L, "application/pdf"));
            when(sysAttachmentMapper.selectAllByBiz(BIZ_TYPE, BIZ_ID)).thenReturn(
                    Collections.emptyList(),
                    Collections.singletonList(buildMount(11L, FILE_ID, 0, "笔记.pdf", "说明", 0)));

            // -anchor when
            attachmentService.save(BIZ_TYPE, BIZ_ID, Collections.singletonList(payload));
            assertThatCode(() -> attachmentService.save(BIZ_TYPE, BIZ_ID, Collections.singletonList(payload)))
                    .doesNotThrowAnyException();

            // -anchor then：只插入一次；第二次既无插入也无卸载/复活/更新
            verify(sysAttachmentMapper, times(1)).insert(any(SysAttachmentPo.class));
            verify(sysAttachmentMapper, never()).deleteById(any(Long.class));
            verify(sysAttachmentMapper, never()).reviveById(anyLong());
            verify(sysAttachmentMapper, never()).updateById(any(SysAttachmentPo.class));
        }

        @Test
        @DisplayName("卸载：新列表为空 → 仅置 del_flag = 1（保留挂载行），且不锁文件行")
        void save_emptyList_unloadsAllAndKeepsRow() {
            // -anchor given
            when(sysAttachmentMapper.selectAllByBiz(BIZ_TYPE, BIZ_ID))
                    .thenReturn(Collections.singletonList(buildMount(11L, FILE_ID, 0, "笔记.pdf", null, 0)));

            // -anchor when
            attachmentService.save(BIZ_TYPE, BIZ_ID, Collections.emptyList());

            // -anchor then：统一用 deleteById（@TableLogic 转 UPDATE），不物理删除、不手写 set(del_flag,1)
            verify(sysAttachmentMapper).deleteById(11L);
            verify(sysAttachmentMapper, never()).insert(any(SysAttachmentPo.class));
            // 空列表没有 fileId 可校验 → 不产生任何文件行加锁
            verify(fileMapper, never()).selectByIdForUpdate(anyLong());
        }

        @Test
        @DisplayName("复活：命中墓碑行（del_flag = 1）→ 置回 0 并刷新元数据")
        void save_tombstoneHit_revivesRow() {
            // -anchor given
            when(fileMapper.selectByIdForUpdate(OTHER_FILE_ID)).thenReturn(buildFile(OTHER_FILE_ID, 1L, "image/png"));
            when(sysAttachmentMapper.selectAllByBiz(BIZ_TYPE, BIZ_ID))
                    .thenReturn(Collections.singletonList(buildMount(12L, OTHER_FILE_ID, 1, "旧名.png", "旧说明", 9)));
            when(sysAttachmentMapper.reviveById(12L)).thenReturn(1);

            // -anchor when
            attachmentService.save(BIZ_TYPE, BIZ_ID,
                    Collections.singletonList(item(null, OTHER_FILE_ID, "新名.png", "新说明", 3)));

            // -anchor then
            verify(sysAttachmentMapper).reviveById(12L);
            ArgumentCaptor<SysAttachmentPo> captor = ArgumentCaptor.forClass(SysAttachmentPo.class);
            verify(sysAttachmentMapper).updateById(captor.capture());
            SysAttachmentPo refreshed = captor.getValue();
            assertThat(refreshed.getId()).isEqualTo(12L);
            assertThat(refreshed.getAttachName()).isEqualTo("新名.png");
            assertThat(refreshed.getDescription()).isEqualTo("新说明");
            assertThat(refreshed.getSortOrder()).isEqualTo(3);
            assertThat(refreshed.getDelFlag()).isEqualTo(SysAttachmentPo.DEL_FLAG_EFFECTIVE);
            // 复活不是新增：不得插行
            verify(sysAttachmentMapper, never()).insert(any(SysAttachmentPo.class));
        }

        @Test
        @DisplayName("交集：字段有变化才更新（按 id 命中既有有效行）")
        void save_existingRowWithChanges_updatesFields() {
            // -anchor given
            when(fileMapper.selectByIdForUpdate(FILE_ID)).thenReturn(buildFile(FILE_ID, 1L, "application/pdf"));
            when(sysAttachmentMapper.selectAllByBiz(BIZ_TYPE, BIZ_ID))
                    .thenReturn(Collections.singletonList(buildMount(11L, FILE_ID, 0, "旧名.pdf", "旧", 0)));

            // -anchor when
            attachmentService.save(BIZ_TYPE, BIZ_ID,
                    Collections.singletonList(item(11L, FILE_ID, "新名.pdf", "新", 5)));

            // -anchor then
            ArgumentCaptor<SysAttachmentPo> captor = ArgumentCaptor.forClass(SysAttachmentPo.class);
            verify(sysAttachmentMapper).updateById(captor.capture());
            assertThat(captor.getValue().getAttachName()).isEqualTo("新名.pdf");
            assertThat(captor.getValue().getSortOrder()).isEqualTo(5);
            verify(sysAttachmentMapper, never()).reviveById(anyLong());
            verify(sysAttachmentMapper, never()).insert(any(SysAttachmentPo.class));
        }

        @Test
        @DisplayName("交集：字段无变化则不产生 UPDATE（原样重复提交零写入）")
        void save_existingRowUnchanged_skipsUpdate() {
            // -anchor given
            when(fileMapper.selectByIdForUpdate(FILE_ID)).thenReturn(buildFile(FILE_ID, 1L, "application/pdf"));
            when(sysAttachmentMapper.selectAllByBiz(BIZ_TYPE, BIZ_ID))
                    .thenReturn(Collections.singletonList(buildMount(11L, FILE_ID, 0, "笔记.pdf", "说明", 0)));

            // -anchor when
            attachmentService.save(BIZ_TYPE, BIZ_ID,
                    Collections.singletonList(item(11L, FILE_ID, "笔记.pdf", "说明", 0)));

            // -anchor then
            verify(sysAttachmentMapper, never()).updateById(any(SysAttachmentPo.class));
            verify(sysAttachmentMapper, never()).insert(any(SysAttachmentPo.class));
            verify(sysAttachmentMapper, never()).deleteById(any(Long.class));
        }

        @Test
        @DisplayName("悬空 fileId：加锁读查不到 → BusinessException 且不写任何挂载行")
        void save_danglingFileId_throwsBeforeAnyWrite() {
            // -anchor given
            when(fileMapper.selectByIdForUpdate(FILE_ID)).thenReturn(null);

            // -anchor when & then
            assertThatThrownBy(() -> attachmentService.save(BIZ_TYPE, BIZ_ID,
                    Collections.singletonList(item(null, FILE_ID, "笔记.pdf", null, 0))))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("挂载的文件不存在");
            verify(sysAttachmentMapper, never()).insert(any(SysAttachmentPo.class));
            verify(sysAttachmentMapper, never()).selectAllByBiz(any(), any());
        }

        @Test
        @DisplayName("并发新增撞唯一键 → 降级为复活并重试一次")
        void save_duplicateKeyOnInsert_revivesAndRetries() {
            // -anchor given
            when(fileMapper.selectByIdForUpdate(FILE_ID)).thenReturn(buildFile(FILE_ID, 1L, "application/pdf"));
            when(sysAttachmentMapper.selectAllByBiz(BIZ_TYPE, BIZ_ID)).thenReturn(Collections.emptyList());
            when(sysAttachmentMapper.insert(any(SysAttachmentPo.class)))
                    .thenThrow(new DuplicateKeyException("uk_biz_file"));
            when(sysAttachmentMapper.selectByBizAndFileForUpdate(BIZ_TYPE, BIZ_ID, FILE_ID))
                    .thenReturn(buildMount(77L, FILE_ID, 1, "并发插入者", null, 0));
            when(sysAttachmentMapper.reviveById(77L)).thenReturn(1);

            // -anchor when
            attachmentService.save(BIZ_TYPE, BIZ_ID,
                    Collections.singletonList(item(null, FILE_ID, "笔记.pdf", null, 0)));

            // -anchor then：复活并发插入的那一行，并刷新成本次入参的元数据
            verify(sysAttachmentMapper).reviveById(77L);
            ArgumentCaptor<SysAttachmentPo> captor = ArgumentCaptor.forClass(SysAttachmentPo.class);
            verify(sysAttachmentMapper).updateById(captor.capture());
            assertThat(captor.getValue().getId()).isEqualTo(77L);
            assertThat(captor.getValue().getAttachName()).isEqualTo("笔记.pdf");
        }

        @Test
        @DisplayName("入参 attachId 命中但 fileId 不一致 → 拒绝（保护 uk_biz_file 语义）")
        void save_attachIdFileIdMismatch_throws() {
            // -anchor given：既有行 id=11 挂的是 FILE_ID
            when(fileMapper.selectByIdForUpdate(OTHER_FILE_ID)).thenReturn(buildFile(OTHER_FILE_ID, 1L, "image/png"));
            when(sysAttachmentMapper.selectAllByBiz(BIZ_TYPE, BIZ_ID))
                    .thenReturn(Collections.singletonList(buildMount(11L, FILE_ID, 0, "笔记.pdf", null, 0)));

            // -anchor when & then
            assertThatThrownBy(() -> attachmentService.save(BIZ_TYPE, BIZ_ID,
                    Collections.singletonList(item(11L, OTHER_FILE_ID, "换文件.png", null, 0))))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("挂载行与文件不匹配");
            verify(sysAttachmentMapper, never()).insert(any(SysAttachmentPo.class));
        }

        @Test
        @DisplayName("入参 attachId 在该业务下不存在 → 降级按 fileId 定位（无命中则新增）")
        void save_staleAttachId_fallsBackToFileIdMatching() {
            // -anchor given
            when(fileMapper.selectByIdForUpdate(FILE_ID)).thenReturn(buildFile(FILE_ID, 1L, "application/pdf"));
            when(sysAttachmentMapper.selectAllByBiz(BIZ_TYPE, BIZ_ID)).thenReturn(Collections.emptyList());

            // -anchor when
            attachmentService.save(BIZ_TYPE, BIZ_ID,
                    Collections.singletonList(item(999L, FILE_ID, "笔记.pdf", null, 0)));

            // -anchor then
            ArgumentCaptor<SysAttachmentPo> captor = ArgumentCaptor.forClass(SysAttachmentPo.class);
            verify(sysAttachmentMapper).insert(captor.capture());
            // 新增时一律生成新雪花 ID，不复用入参里那个陈旧的 id
            assertThat(captor.getValue().getId()).isNotEqualTo(999L);
        }

        @Test
        @DisplayName("未识别 bizType / 空 attachName / 入参 fileId 重复 → 均拒绝且零写入")
        void save_invalidPayloads_throwWithoutWrite() {
            // -anchor given & when & then
            assertThatThrownBy(() -> attachmentService.save("unknown", BIZ_ID, Collections.emptyList()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("未识别的业务类型");

            assertThatThrownBy(() -> attachmentService.save(BIZ_TYPE, BIZ_ID,
                    Collections.singletonList(item(null, FILE_ID, "  ", null, 0))))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("附件名称不能为空");

            assertThatThrownBy(() -> attachmentService.save(BIZ_TYPE, BIZ_ID, Arrays.asList(
                    item(null, FILE_ID, "a.pdf", null, 0),
                    item(null, FILE_ID, "b.pdf", null, 1))))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("同一文件不可重复挂载");

            assertThatThrownBy(() -> attachmentService.save(BIZ_TYPE, BIZ_ID, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("附件列表不能为空");

            verify(sysAttachmentMapper, never()).insert(any(SysAttachmentPo.class));
            verify(sysAttachmentMapper, never()).deleteById(any(Long.class));
            verify(fileMapper, never()).selectByIdForUpdate(anyLong());
        }
    }

    @Nested
    @DisplayName("卸载（remove）")
    class RemoveTest {

        @Test
        @DisplayName("多挂载删其一：正常返回不抛异常，文件记录与磁盘文件均保留")
        void remove_stillMountedByOtherBiz_returnsNormallyAndKeepsFile() {
            // -anchor given：卸载后仍剩 1 个有效挂载（另一业务/另一挂载点在共享）
            when(sysAttachmentMapper.selectById(ATTACH_ID))
                    .thenReturn(buildMount(ATTACH_ID, FILE_ID, 0, "共享.pdf", null, 0));
            when(fileMapper.selectByIdForUpdate(FILE_ID)).thenReturn(buildFile(FILE_ID, 1L, "application/pdf"));
            when(sysAttachmentMapper.deleteById(ATTACH_ID)).thenReturn(1);
            when(sysAttachmentMapper.selectCount(any())).thenReturn(1L);

            // -anchor when & then
            assertThatCode(() -> attachmentService.remove(ATTACH_ID)).doesNotThrowAnyException();
            verify(sysAttachmentMapper).deleteById(ATTACH_ID);
            // 关键：不得委派文件层删除（FileService.remove 对"仍有有效挂载"会抛异常，那是管理端语义）
            verify(fileService, never()).remove(anyLong());
        }

        @Test
        @DisplayName("唯一挂载卸载：委派 FileService 物理删记录 + 删盘（删盘在文件层提交后）")
        void remove_lastMount_delegatesPhysicalDelete() {
            // -anchor given
            when(sysAttachmentMapper.selectById(ATTACH_ID))
                    .thenReturn(buildMount(ATTACH_ID, FILE_ID, 0, "独占.pdf", null, 0));
            when(fileMapper.selectByIdForUpdate(FILE_ID)).thenReturn(buildFile(FILE_ID, 1L, "application/pdf"));
            when(sysAttachmentMapper.deleteById(ATTACH_ID)).thenReturn(1);
            when(sysAttachmentMapper.selectCount(any())).thenReturn(0L);

            // -anchor when
            attachmentService.remove(ATTACH_ID);

            // -anchor then：唯一所有者仍是 FileService（本类不重复删记录、不重复删盘）
            verify(fileService).remove(FILE_ID);
        }

        @Test
        @DisplayName("锁序与委派顺序：先锁文件行 → 再标记卸载 → 再计数 → 最后委派")
        void remove_lockOrderIsFileRowFirst() {
            // -anchor given
            when(sysAttachmentMapper.selectById(ATTACH_ID))
                    .thenReturn(buildMount(ATTACH_ID, FILE_ID, 0, "独占.pdf", null, 0));
            when(fileMapper.selectByIdForUpdate(FILE_ID)).thenReturn(buildFile(FILE_ID, 1L, "application/pdf"));
            when(sysAttachmentMapper.deleteById(ATTACH_ID)).thenReturn(1);
            when(sysAttachmentMapper.selectCount(any())).thenReturn(0L);

            // -anchor when
            attachmentService.remove(ATTACH_ID);

            // -anchor then：锁文件行是第一条【锁】语句，与挂载写入路径锁序对称（防悬空挂载与死锁）
            InOrder inOrder = inOrder(fileMapper, sysAttachmentMapper, fileService);
            inOrder.verify(fileMapper).selectByIdForUpdate(FILE_ID);
            inOrder.verify(sysAttachmentMapper).deleteById(ATTACH_ID);
            inOrder.verify(sysAttachmentMapper).selectCount(any());
            inOrder.verify(fileService).remove(FILE_ID);
        }

        @Test
        @DisplayName("挂载行不存在或已卸载 → BusinessException")
        void remove_notExists_throws() {
            // -anchor given：@TableLogic 让"不存在"与"已卸载"都返回 null
            when(sysAttachmentMapper.selectById(ATTACH_ID)).thenReturn(null);

            // -anchor when & then
            assertThatThrownBy(() -> attachmentService.remove(ATTACH_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("附件挂载不存在或已卸载");
            verify(sysAttachmentMapper, never()).deleteById(any(Long.class));
            verify(fileService, never()).remove(anyLong());
        }

        @Test
        @DisplayName("文件记录已不存在 → 仅卸载挂载行，不委派、不抛异常")
        void remove_fileRowAlreadyGone_onlyUnloadsMount() {
            // -anchor given
            when(sysAttachmentMapper.selectById(ATTACH_ID))
                    .thenReturn(buildMount(ATTACH_ID, FILE_ID, 0, "残留.pdf", null, 0));
            when(fileMapper.selectByIdForUpdate(FILE_ID)).thenReturn(null);
            when(sysAttachmentMapper.deleteById(ATTACH_ID)).thenReturn(1);

            // -anchor when & then
            assertThatCode(() -> attachmentService.remove(ATTACH_ID)).doesNotThrowAnyException();
            verify(fileService, never()).remove(anyLong());
            verify(sysAttachmentMapper, never()).selectCount(any());
        }

        @Test
        @DisplayName("并发：本行已被另一事务卸载（受影响行数 0）→ 幂等 no-op，不委派")
        void remove_concurrentlyUnloaded_isIdempotentNoOp() {
            // -anchor given
            when(sysAttachmentMapper.selectById(ATTACH_ID))
                    .thenReturn(buildMount(ATTACH_ID, FILE_ID, 0, "并发.pdf", null, 0));
            when(fileMapper.selectByIdForUpdate(FILE_ID)).thenReturn(buildFile(FILE_ID, 1L, "application/pdf"));
            when(sysAttachmentMapper.deleteById(ATTACH_ID)).thenReturn(0);

            // -anchor when & then
            assertThatCode(() -> attachmentService.remove(ATTACH_ID)).doesNotThrowAnyException();
            verify(fileService, never()).remove(anyLong());
            verify(sysAttachmentMapper, never()).selectCount(any());
        }
    }

    // --- fixtures ---

    private SysAttachmentPo buildMount(Long id, Long fileId, Integer delFlag,
                                       String attachName, String description, Integer sortOrder) {
        SysAttachmentPo po = new SysAttachmentPo();
        po.setId(id);
        po.setFileId(fileId);
        po.setBizType(BIZ_TYPE);
        po.setBizId(BIZ_ID);
        po.setAttachName(attachName);
        po.setDescription(description);
        po.setSortOrder(sortOrder);
        po.setDelFlag(delFlag);
        return po;
    }

    private FileRecordPo buildFile(Long id, Long fileSize, String fileType) {
        FileRecordPo po = new FileRecordPo();
        po.setId(id);
        po.setFileSize(fileSize);
        po.setFileType(fileType);
        po.setFilePath("2026/09/20/" + id);
        po.setOriginalName("首次上传者的名字");
        return po;
    }

    private AttachmentDto item(Long id, Long fileId, String attachName, String description, Integer sortOrder) {
        AttachmentDto dto = new AttachmentDto();
        dto.setId(id);
        dto.setFileId(fileId);
        dto.setAttachName(attachName);
        dto.setDescription(description);
        dto.setSortOrder(sortOrder);
        return dto;
    }
}
