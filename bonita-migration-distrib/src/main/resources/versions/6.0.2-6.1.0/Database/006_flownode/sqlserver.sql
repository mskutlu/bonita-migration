--
-- arch_flownode_instance
-- 

ALTER TABLE arch_flownode_instance ADD executedByDelegate NUMERIC(19,0)
@@


--
-- flownode_instance
-- 

ALTER TABLE flownode_instance ADD executedByDelegate NUMERIC(19,0)
@@


--
-- Datas
-- 

UPDATE flownode_instance SET executedbydelegate = executedby @@
UPDATE arch_flownode_instance SET executedbydelegate = executedby @@