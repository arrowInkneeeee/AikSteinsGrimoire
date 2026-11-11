-- 回滚：删除本批 code（不会影响既有其他知识）
DELETE FROM aik_knowledge WHERE code IN ('importer-smoke-test-20261111');
