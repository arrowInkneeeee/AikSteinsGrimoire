package io.aik.steins.grimoire.knowledge.common.constant;

/**
 * -anchor 知识库模块常量
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since 2026/05/18
 * -
 */
public final class KnowledgeConstant {

    private KnowledgeConstant() {
    }

    /**
     * 表前缀
     */
    public static final String TABLE_PREFIX = "aik_knowledge_";

    /**
     * 状态：启用
     */
    public static final Integer STATUS_ENABLE = 1;

    /**
     * 状态：禁用
     */
    public static final Integer STATUS_DISABLE = 0;

    /**
     * 分类根节点ID
     */
    public static final Long CATEGORY_ROOT_ID = 0L;
}
