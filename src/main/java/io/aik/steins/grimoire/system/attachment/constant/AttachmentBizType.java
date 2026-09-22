package io.aik.steins.grimoire.system.attachment.constant;

import io.aik.steins.grimoire.core.utils.AssertUtils;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * -anchor 附件挂载业务类型常量（{@code biz_type} 白名单的【唯一来源】）
 *
 * <p>设计权威：SDD §2.5.5。取值登记表当前只有一行：{@code knowledge} → {@code aik_knowledge.id}。</p>
 *
 * <p><b>为什么不用 Java {@code enum}</b>：{@code biz_type} 是<b>开放取值</b>（DDL 就是
 * {@code VARCHAR(32)}）——新增业务域挂载不应要求修改附件模块代码并重新编译。写入时校验白名单，
 * 未知值抛 {@code BusinessException}；取值增多时可下沉为字典表（届时以字典为准）。</p>
 *
 * <p>本类<b>不</b>并入 {@code SystemConstant}：后者是字典 / 参数域的常量容器，
 * 混装会让职责再次交叉（SDD §2.5.5 显式记录该裁决）。</p>
 *
 * @author a I k .
 */
public final class AttachmentBizType {

    /**
     * 知识条目（biz_id 指向 aik_knowledge.id）
     */
    public static final String KNOWLEDGE = "knowledge";

    /**
     * 白名单：新增业务域时在此登记，并同步 SDD §2.5.5 的取值登记表
     */
    private static final Set<String> WHITELIST = Collections.unmodifiableSet(
            new LinkedHashSet<>(Collections.singletonList(KNOWLEDGE)));

    private AttachmentBizType() {
    }

    /**
     * 白名单（只读视图）
     */
    public static Set<String> whitelist() {
        return WHITELIST;
    }

    /**
     * 是否在白名单内
     */
    public static boolean isSupported(String bizType) {
        return bizType != null && WHITELIST.contains(bizType);
    }

    /**
     * 校验 {@code biz_type} 在白名单内
     *
     * @param bizType 业务类型
     * @throws io.aik.steins.grimoire.core.exception.BusinessException 未识别的取值
     */
    public static void validate(String bizType) {
        AssertUtils.isTrue(isSupported(bizType),
                "未识别的业务类型：" + bizType + "（仅支持：" + String.join("、", WHITELIST) + "）");
    }
}
