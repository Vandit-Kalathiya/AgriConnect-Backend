-- Manual rollback for V20260329_01__ai_persistence_tables.sql
-- Run only after impact assessment in controlled windows.

DROP TABLE IF EXISTS ai_messages;
DROP TABLE IF EXISTS ai_conversations;
