package io.aik.steins.grimoire.system.service.impl;

import io.aik.steins.grimoire.core.exception.BusinessException;
import io.aik.steins.grimoire.system.common.dto.SystemParamDto;
import io.aik.steins.grimoire.system.common.po.SystemParamPo;
import io.aik.steins.grimoire.system.common.vo.SystemParamVo;
import io.aik.steins.grimoire.system.dao.SystemParamMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SystemParamServiceImpl 单元测试 -anchor
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since 2026/7/29
 * -
 **/
@ExtendWith(MockitoExtension.class)
@DisplayName("系统参数服务测试")
class SystemParamServiceImplTest {

    private static final Long TEST_PARAM_ID = 1001L;
    private static final String TEST_PARAM_KEY = "site.title";
    private static final String TEST_PARAM_VALUE = "命运石魔典";
    private static final String TEST_PARAM_GROUP = "site";

    @Mock
    private SystemParamMapper systemParamMapper;

    @InjectMocks
    private SystemParamServiceImpl systemParamService;

    @Nested
    @DisplayName("按键查询参数")
    class FindByKeyTest {

        @Test
        @DisplayName("参数键存在时返回视图")
        void findByKey_exists_returnsVo() {
            // -anchor given
            SystemParamPo po = buildPo(TEST_PARAM_ID, TEST_PARAM_KEY, TEST_PARAM_VALUE, 1);
            when(systemParamMapper.selectOne(any())).thenReturn(po);

            // -anchor when
            SystemParamVo result = systemParamService.findByKey(TEST_PARAM_KEY);

            // -anchor then
            assertThat(result).isNotNull();
            assertThat(result.getParamKey()).isEqualTo(TEST_PARAM_KEY);
            assertThat(result.getParamValue()).isEqualTo(TEST_PARAM_VALUE);
        }

        @Test
        @DisplayName("参数键不存在时返回 null")
        void findByKey_notExists_returnsNull() {
            // -anchor given
            when(systemParamMapper.selectOne(any())).thenReturn(null);

            // -anchor when
            SystemParamVo result = systemParamService.findByKey(TEST_PARAM_KEY);

            // -anchor then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("参数键为空白时抛出 BusinessException")
        void findByKey_blankKey_throwsBusinessException() {
            // -anchor given & when & then
            assertThatThrownBy(() -> systemParamService.findByKey("  "))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("参数键不能为空");
            verify(systemParamMapper, never()).selectOne(any());
        }
    }

    @Nested
    @DisplayName("获取参数值与缓存")
    class GetParamValueTest {

        @Test
        @DisplayName("缓存未命中时回源数据库并回填缓存")
        void getParamValue_cacheMiss_queriesDbAndBackfills() {
            // -anchor given
            SystemParamPo po = buildPo(TEST_PARAM_ID, TEST_PARAM_KEY, TEST_PARAM_VALUE, 1);
            when(systemParamMapper.selectOne(any())).thenReturn(po);

            // -anchor when
            String first = systemParamService.getParamValue(TEST_PARAM_KEY);
            String second = systemParamService.getParamValue(TEST_PARAM_KEY);

            // -anchor then
            assertThat(first).isEqualTo(TEST_PARAM_VALUE);
            assertThat(second).isEqualTo(TEST_PARAM_VALUE);
            verify(systemParamMapper, times(1)).selectOne(any());
        }

        @Test
        @DisplayName("数据库也不存在时返回 null")
        void getParamValue_notExists_returnsNull() {
            // -anchor given
            when(systemParamMapper.selectOne(any())).thenReturn(null);

            // -anchor when
            String result = systemParamService.getParamValue(TEST_PARAM_KEY);

            // -anchor then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("刷新缓存后直接从缓存取值")
        void refreshCache_loadsAllIntoCache() {
            // -anchor given
            SystemParamPo po = buildPo(TEST_PARAM_ID, TEST_PARAM_KEY, TEST_PARAM_VALUE, 1);
            when(systemParamMapper.selectList(null)).thenReturn(Arrays.asList(po));

            // -anchor when
            systemParamService.refreshCache();
            String value = systemParamService.getParamValue(TEST_PARAM_KEY);

            // -anchor then
            assertThat(value).isEqualTo(TEST_PARAM_VALUE);
            verify(systemParamMapper, never()).selectOne(any());
        }
    }

    @Nested
    @DisplayName("新增参数")
    class AddTest {

        @Test
        @DisplayName("参数键不存在时新增成功并写入缓存")
        void add_success() {
            // -anchor given
            SystemParamDto dto = buildDto(null, TEST_PARAM_KEY, TEST_PARAM_VALUE, 1);
            when(systemParamMapper.selectCount(any())).thenReturn(0L);

            // -anchor when
            systemParamService.add(dto);
            String cached = systemParamService.getParamValue(TEST_PARAM_KEY);

            // -anchor then
            verify(systemParamMapper).insert(any(SystemParamPo.class));
            assertThat(cached).isEqualTo(TEST_PARAM_VALUE);
            verify(systemParamMapper, never()).selectOne(any());
        }

        @Test
        @DisplayName("参数键已存在时抛出 BusinessException")
        void add_duplicateKey_throwsBusinessException() {
            // -anchor given
            SystemParamDto dto = buildDto(null, TEST_PARAM_KEY, TEST_PARAM_VALUE, 1);
            when(systemParamMapper.selectCount(any())).thenReturn(1L);

            // -anchor when & then
            assertThatThrownBy(() -> systemParamService.add(dto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("参数键已存在");
            verify(systemParamMapper, never()).insert(any(SystemParamPo.class));
        }
    }

    @Nested
    @DisplayName("修改参数")
    class ModifyTest {

        @Test
        @DisplayName("参数存在且可编辑时修改成功")
        void modify_success() {
            // -anchor given
            SystemParamPo existing = buildPo(TEST_PARAM_ID, TEST_PARAM_KEY, "旧值", 1);
            SystemParamDto dto = buildDto(TEST_PARAM_ID, TEST_PARAM_KEY, "新值", 1);
            when(systemParamMapper.selectById(TEST_PARAM_ID)).thenReturn(existing);

            // -anchor when
            systemParamService.modify(dto);
            String cached = systemParamService.getParamValue(TEST_PARAM_KEY);

            // -anchor then
            verify(systemParamMapper).updateById(any(SystemParamPo.class));
            assertThat(cached).isEqualTo("新值");
        }

        @Test
        @DisplayName("ID 为空时抛出 BusinessException")
        void modify_nullId_throwsBusinessException() {
            // -anchor given
            SystemParamDto dto = buildDto(null, TEST_PARAM_KEY, TEST_PARAM_VALUE, 1);

            // -anchor when & then
            assertThatThrownBy(() -> systemParamService.modify(dto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("ID不能为空");
        }

        @Test
        @DisplayName("参数不存在时抛出 BusinessException")
        void modify_notExists_throwsBusinessException() {
            // -anchor given
            SystemParamDto dto = buildDto(TEST_PARAM_ID, TEST_PARAM_KEY, TEST_PARAM_VALUE, 1);
            when(systemParamMapper.selectById(TEST_PARAM_ID)).thenReturn(null);

            // -anchor when & then
            assertThatThrownBy(() -> systemParamService.modify(dto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("系统参数不存在");
        }

        @Test
        @DisplayName("参数不可编辑时抛出 BusinessException")
        void modify_notEditable_throwsBusinessException() {
            // -anchor given
            SystemParamPo existing = buildPo(TEST_PARAM_ID, TEST_PARAM_KEY, "旧值", 0);
            SystemParamDto dto = buildDto(TEST_PARAM_ID, TEST_PARAM_KEY, "新值", 1);
            when(systemParamMapper.selectById(TEST_PARAM_ID)).thenReturn(existing);

            // -anchor when & then
            assertThatThrownBy(() -> systemParamService.modify(dto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("该参数不可编辑");
            verify(systemParamMapper, never()).updateById(any(SystemParamPo.class));
        }
    }

    @Nested
    @DisplayName("删除参数")
    class RemoveTest {

        @Test
        @DisplayName("参数存在时删除成功并清理缓存")
        void remove_success() {
            // -anchor given
            SystemParamPo po = buildPo(TEST_PARAM_ID, TEST_PARAM_KEY, TEST_PARAM_VALUE, 1);
            when(systemParamMapper.selectById(TEST_PARAM_ID)).thenReturn(po);
            when(systemParamMapper.selectOne(any())).thenReturn(null);

            // -anchor when
            systemParamService.remove(TEST_PARAM_ID);
            String cached = systemParamService.getParamValue(TEST_PARAM_KEY);

            // -anchor then
            verify(systemParamMapper).deleteById(TEST_PARAM_ID);
            assertThat(cached).isNull();
            verify(systemParamMapper).selectOne(any());
        }

        @Test
        @DisplayName("参数不存在时抛出 BusinessException")
        void remove_notExists_throwsBusinessException() {
            // -anchor given
            when(systemParamMapper.selectById(TEST_PARAM_ID)).thenReturn(null);

            // -anchor when & then
            assertThatThrownBy(() -> systemParamService.remove(TEST_PARAM_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("系统参数不存在");
            verify(systemParamMapper, never()).deleteById(any(Long.class));
        }
    }

    private SystemParamPo buildPo(Long id, String paramKey, String paramValue, Integer editable) {
        SystemParamPo po = new SystemParamPo();
        po.setId(id);
        po.setParamKey(paramKey);
        po.setParamValue(paramValue);
        po.setParamGroup(TEST_PARAM_GROUP);
        po.setEditable(editable);
        return po;
    }

    private SystemParamDto buildDto(Long id, String paramKey, String paramValue, Integer editable) {
        SystemParamDto dto = new SystemParamDto();
        dto.setId(id);
        dto.setParamKey(paramKey);
        dto.setParamValue(paramValue);
        dto.setParamGroup(TEST_PARAM_GROUP);
        dto.setEditable(editable);
        return dto;
    }
}
