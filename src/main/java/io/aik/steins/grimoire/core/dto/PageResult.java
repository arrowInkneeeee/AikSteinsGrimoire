package io.aik.steins.grimoire.core.dto;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 通用分页结果 -anchor
 *
 * @param <T> 数据类型
 * @author a I k .
 */
@Data
@Schema(description = "分页结果")
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "数据列表")
    private List<T> records;

    @Schema(description = "总记录数")
    private long total;

    @Schema(description = "总页数")
    private long pages;

    @Schema(description = "当前页")
    private long current;

    @Schema(description = "每页大小")
    private long size;

    @Schema(description = "是否有下一页")
    private boolean hasNext;

    @Schema(description = "是否有上一页")
    private boolean hasPrevious;

    /**
     * 从 MyBatis-Plus 的 IPage 构建 PageResult
     */
    public static <T> PageResult<T> of(IPage<T> page) {
        if (page == null) {
            return null;
        }
        PageResult<T> result = new PageResult<>();
        result.setRecords(page.getRecords());
        result.setTotal(page.getTotal());
        result.setPages(page.getPages());
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        result.setHasNext(page.getCurrent() < page.getPages());
        result.setHasPrevious(page.getCurrent() > 1);
        return result;
    }
}
