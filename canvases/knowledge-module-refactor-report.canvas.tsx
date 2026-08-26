import { Stack, H1, H2, Grid, Stat, Divider, Table, Text, Callout } from 'qoder/canvas';

export default function KnowledgeModuleRefactorReport() {
  return (
    <Stack gap={20}>
      <H1>knowledge 模块结构规范化改造 — 完成报告</H1>

      <Grid columns={4} gap={16}>
        <Stat value="6" label="新建文件" tone="success" />
        <Stat value="6" label="删除文件" tone="danger" />
        <Stat value="2" label="修改引用" />
        <Stat value="3" label="清理空目录" tone="warning" />
      </Grid>

      <Divider />

      <H2>改造目标达成情况</H2>
      <Table
        headers={['目标', '状态', '说明']}
        rows={[
          ['dao 归位', '完成', 'knowledge/common/dao/ 4 个 Mapper 全部上移到 knowledge/dao/'],
          ['系统级附件归位', '完成', 'KnowledgeAttachmentPo/Mapper 迁移到 system 模块并改名 SysAttachment*'],
          ['PO 名表名一一映射', '完成', 'CategoryPo → KnowledgeCategoryPo'],
          ['清理空目录', '完成', '删除 knowledge/mapper/、common/mapper/、common/dao/'],
        ]}
        rowTone={['success', 'success', 'success', 'success']}
      />

      <Divider />

      <H2>关键文件变更</H2>
      <Table
        headers={['类型', '路径', '说明']}
        rows={[
          ['新建', 'knowledge/dao/KnowledgeCategoryMapper.java', '上移 + 改名'],
          ['新建', 'knowledge/dao/KnowledgeTagMapper.java', '上移'],
          ['新建', 'knowledge/dao/KnowledgeTagRelationMapper.java', '上移'],
          ['新建', 'system/dao/SysAttachmentMapper.java', '跨模块迁移 + 改名'],
          ['新建', 'system/common/po/SysAttachmentPo.java', '跨模块迁移 + 改名'],
          ['新建', 'knowledge/common/po/KnowledgeCategoryPo.java', '原地改名'],
          ['修改', 'knowledge/service/impl/KnowledgeServiceImpl.java', '更新 import 和注入'],
          ['修改', 'knowledge/common/vo/KnowledgeVo.java', 'KnowledgeAttachmentPo → SysAttachmentPo'],
          ['删除', 'knowledge/common/dao/* (4 个文件)', '迁空后删除'],
          ['删除', 'knowledge/common/po/CategoryPo.java', '重命名后删除旧文件'],
          ['删除', 'knowledge/common/po/KnowledgeAttachmentPo.java', '迁移后删除'],
        ]}
      />

      <Divider />

      <H2>验证结果</H2>
      <Table
        headers={['检查项', '结果', '证据']}
        rows={[
          ['编译通过', '通过', 'mvn clean compile -q 无错误'],
          ['启动成功', '通过', 'AikSteinsGrimoire is successfully started!'],
          ['Swagger 正常', '通过', 'Swagger UI 200，/v3/api-docs 接口注册正常'],
          ['旧类名残留', '无残留', 'grep 全代码无 CategoryPo/CategoryMapper/KnowledgeAttachmentPo/KnowledgeAttachmentMapper'],
          ['system 模块耦合', '无耦合', 'system 模块无 knowledge.common.po 引用'],
          ['空目录残留', '已清理', 'Test-Path 确认 3 个目录均为 False'],
        ]}
        rowTone={['success', 'success', 'success', 'success', 'success', 'success']}
      />

      <Divider />

      <H2>改造后目录结构</H2>
      <Callout tone="info" title="knowledge/">
        common/ — constant/ dto/ enums/ po/ vo/（保持不动）
        <br />
        dao/ — KnowledgeMapper、KnowledgeCategoryMapper、KnowledgeTagMapper、KnowledgeTagRelationMapper
        <br />
        controller/ service/
      </Callout>
      <Callout tone="info" title="system/">
        common/po/ — SysAttachmentPo（新增，从 knowledge 迁移）
        <br />
        dao/ — SysAttachmentMapper（新增，从 knowledge 迁移）
      </Callout>

      <Text tone="secondary" size="small">
        报告生成时间：2026-08-26 · 全部改造目标已达成
      </Text>
    </Stack>
  );
}
