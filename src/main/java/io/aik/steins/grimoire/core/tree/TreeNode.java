package io.aik.steins.grimoire.core.tree;

import java.util.List;

/**
 * 树形节点接口 -anchor
 *
 * <p>需要构建树形结构的实体实现此接口</p>
 *
 * @param <T> ID 类型
 * @author a I k .
 */
public interface TreeNode<T> {

    /**
     * 节点唯一标识
     */
    T getId();

    /**
     * 父节点标识，根节点返回 null 或特定值
     */
    T getParentId();

    /**
     * 排序号，同级节点按此排序
     */
    Integer getSortOrder();

    /**
     * 子节点列表
     */
    List<? extends TreeNode<T>> getChildren();

    /**
     * 设置子节点列表
     */
    void setChildren(List<? extends TreeNode<T>> children);
}
