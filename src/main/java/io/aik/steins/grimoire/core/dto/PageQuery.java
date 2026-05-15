package io.aik.steins.grimoire.core.dto;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.aik.steins.grimoire.core.constant.PageConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 分页查询参数 -anchor
 *
 * @author a I k .
 */
@Data
@Schema(description = "分页查询参数")
public class PageQuery {

    @Schema(description = "当前页码", example = "1")
    private Long current = PageConstant.DEFAULT_PAGE_NUM;

    @Schema(description = "每页条数", example = "10")
    private Long size = PageConstant.DEFAULT_PAGE_SIZE;

    /**
     * 转换为 MyBatis-Plus Page 对象
     *
     * @param <T> 数据类型
     * @return Page 对象
     */
    public <T> Page<T> toPage() {
        long pageNum = current == null || current < 1 ? PageConstant.DEFAULT_PAGE_NUM : current;
        long pageSize = size == null || size < 1 ? PageConstant.DEFAULT_PAGE_SIZE : size;
        //anchor 限制最大页大小，防止恶意传参
        pageSize = Math.min(pageSize, PageConstant.MAX_PAGE_SIZE);
        return new Page<>(pageNum, pageSize);
    }
}
