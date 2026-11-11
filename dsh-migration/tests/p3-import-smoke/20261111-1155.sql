SET NAMES utf8mb4;
-- 由 knowledge-import.mjs 生成；按 uk_code 幂等；执行前请备份。
INSERT INTO aik_knowledge
  (id, title, code, type, summary, content, source_project, source_path, resource_path, ext_json, category_id, status)
VALUES
  (1794372146331000000, 'importer-smoke-test-20261111', 'importer-smoke-test-20261111', 4, '// P3 importer commit-path smoke test; safe to delete',
   '// P3 importer commit-path smoke test; safe to delete
', '', 'smoke-test',
   '', '{"sourceRef":"smoke-test"}', NULL, 1)
ON DUPLICATE KEY UPDATE
  title = VALUES(title), type = VALUES(type), summary = VALUES(summary),
  content = VALUES(content), source_project = VALUES(source_project), source_path = VALUES(source_path),
  resource_path = VALUES(resource_path), ext_json = VALUES(ext_json), category_id = VALUES(category_id), status = 1;
