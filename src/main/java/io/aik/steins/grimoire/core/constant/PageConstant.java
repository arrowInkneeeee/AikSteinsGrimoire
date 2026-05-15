package io.aik.steins.grimoire.core.constant;

/**
 * 分页常量 -anchor
 *
 * @author a I k .
 */
public final class PageConstant {

    private PageConstant() {
    }

    /**
     * 默认当前页码
     */
    public static final long DEFAULT_PAGE_NUM = 1;

    /**
     * 默认每页条数
     */
    public static final long DEFAULT_PAGE_SIZE = 10;

    /**
     * 最大每页条数，防止前端传过大数值导致数据库压力
     */
    public static final long MAX_PAGE_SIZE = 500;
}
