package io.aik.steins.grimoire.core.tree;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 树形结构构建工具 -anchor
 *
 * @author a I k .
 */
public class TreeUtils {

    private TreeUtils() {
    }

    /**
     * 构建树形结构
     *
     * <p>默认根节点的 parentId 为 null 或 0（数值类型）</p>
     *
     * @param nodes 扁平节点列表
     * @return 根节点列表
     */
    public static <T, N extends TreeNode<T>> List<N> buildTree(List<N> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return new ArrayList<>();
        }
        // 自动检测根节点 parentId：先尝试 null，再尝试 0（Long）
        T sampleParentId = nodes.get(0).getParentId();
        if (sampleParentId instanceof Number) {
            return buildTree(nodes, (T) Long.valueOf(0L));
        }
        return buildTree(nodes, null);
    }

    /**
     * 构建树形结构，指定根节点的 parentId
     *
     * @param nodes        扁平节点列表
     * @param rootParentId 根节点的 parentId 值
     * @return 根节点列表
     */
    public static <T, N extends TreeNode<T>> List<N> buildTree(List<N> nodes, T rootParentId) {
        if (nodes == null || nodes.isEmpty()) {
            return new ArrayList<>();
        }

        // 按 parentId 分组
        Map<T, List<N>> parentMap = nodes.stream()
                .collect(Collectors.groupingBy(TreeNode::getParentId));

        // 找到根节点（parentId 等于 rootParentId 的节点）
        List<N> roots = parentMap.getOrDefault(rootParentId, new ArrayList<>());

        // 按 sortOrder 排序
        roots.sort(Comparator.comparing(n -> n.getSortOrder() == null ? 0 : n.getSortOrder()));

        // 递归设置子节点
        for (N root : roots) {
            setChildren(root, parentMap);
        }

        return roots;
    }

    /**
     * 递归设置子节点
     */
    @SuppressWarnings("unchecked")
    private static <T, N extends TreeNode<T>> void setChildren(N node, Map<T, List<N>> parentMap) {
        List<N> children = (List<N>) parentMap.get(node.getId());
        if (children == null || children.isEmpty()) {
            node.setChildren(new ArrayList<>());
            return;
        }
        // 按 sortOrder 排序
        children.sort(Comparator.comparing(n -> n.getSortOrder() == null ? 0 : n.getSortOrder()));
        node.setChildren(children);
        for (N child : children) {
            setChildren(child, parentMap);
        }
    }
}
