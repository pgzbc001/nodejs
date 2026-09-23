-- ============================================================
-- 主数据管理平台 种子数据（MDM-0001）
-- 每次启动幂等执行：仅当表为空时插入
-- ============================================================

-- 分类树：行业核心经营类 / 合作伙伴类 / 组织人员类 / 公共基础类
INSERT INTO mdm_category (id, code, name, parent_id, sort_no, description, created_by, created_time)
SELECT 1, 'CAT01', '行业核心经营类', NULL, 1, '采购/生产/销售核心主数据', 'system', '2026-09-23T09:00:00'
WHERE NOT EXISTS (SELECT 1 FROM mdm_category WHERE id = 1);
INSERT INTO mdm_category (id, code, name, parent_id, sort_no, description, created_by, created_time)
SELECT 2, 'CAT02', '合作伙伴类', NULL, 2, '供应商/客户等外部伙伴', 'system', '2026-09-23T09:00:00'
WHERE NOT EXISTS (SELECT 1 FROM mdm_category WHERE id = 2);
INSERT INTO mdm_category (id, code, name, parent_id, sort_no, description, created_by, created_time)
SELECT 3, 'CAT03', '组织人员类', NULL, 3, '员工/组织等内部主数据', 'system', '2026-09-23T09:00:00'
WHERE NOT EXISTS (SELECT 1 FROM mdm_category WHERE id = 3);
INSERT INTO mdm_category (id, code, name, parent_id, sort_no, description, created_by, created_time)
SELECT 4, 'CAT04', '公共基础类', NULL, 4, '行政区划等基础数据', 'system', '2026-09-23T09:00:00'
WHERE NOT EXISTS (SELECT 1 FROM mdm_category WHERE id = 4);

INSERT INTO mdm_category (id, code, name, parent_id, sort_no, description, created_by, created_time)
SELECT 11, 'CAT0101', '原料', 1, 1, '生产用原料', 'system', '2026-09-23T09:00:00'
WHERE NOT EXISTS (SELECT 1 FROM mdm_category WHERE id = 11);
INSERT INTO mdm_category (id, code, name, parent_id, sort_no, description, created_by, created_time)
SELECT 12, 'CAT0102', '成品', 1, 2, '销售成品', 'system', '2026-09-23T09:00:00'
WHERE NOT EXISTS (SELECT 1 FROM mdm_category WHERE id = 12);
INSERT INTO mdm_category (id, code, name, parent_id, sort_no, description, created_by, created_time)
SELECT 13, 'CAT0103', '半成品', 1, 3, '中间产品', 'system', '2026-09-23T09:00:00'
WHERE NOT EXISTS (SELECT 1 FROM mdm_category WHERE id = 13);
INSERT INTO mdm_category (id, code, name, parent_id, sort_no, description, created_by, created_time)
SELECT 21, 'CAT0201', '供应商', 2, 1, '供应商主数据', 'system', '2026-09-23T09:00:00'
WHERE NOT EXISTS (SELECT 1 FROM mdm_category WHERE id = 21);
INSERT INTO mdm_category (id, code, name, parent_id, sort_no, description, created_by, created_time)
SELECT 22, 'CAT0202', '客户', 2, 2, '客户主数据', 'system', '2026-09-23T09:00:00'
WHERE NOT EXISTS (SELECT 1 FROM mdm_category WHERE id = 22);
INSERT INTO mdm_category (id, code, name, parent_id, sort_no, description, created_by, created_time)
SELECT 31, 'CAT0301', '员工', 3, 1, '员工主数据', 'system', '2026-09-23T09:00:00'
WHERE NOT EXISTS (SELECT 1 FROM mdm_category WHERE id = 31);
INSERT INTO mdm_category (id, code, name, parent_id, sort_no, description, created_by, created_time)
SELECT 41, 'CAT0401', '行政区划', 4, 1, '国家行政区划', 'system', '2026-09-23T09:00:00'
WHERE NOT EXISTS (SELECT 1 FROM mdm_category WHERE id = 41);

-- 演示模型：原材料（已上线）
INSERT INTO mdm_model (id, code, name, category_id, dept, status, version_no, field_defs, code_rules, ext_config, description, created_by, created_time, updated_by, updated_time)
SELECT 1, 'MAT_RAW', '原材料', 11, '原料部', 'ONLINE', 2,
'[
 {"name":"materialCategory","label":"物料分类编码","type":"TEXT","required":true,"unique":false,"listShow":false,"selectable":false,"domainSource":"MANUAL","domainValues":[],"refModelCode":null,"refFilter":null,"multiSelect":false,"searchable":false,"popup":false,"encrypted":false,"securityLevel":"PUBLIC","securityRule":null,"defaultValue":"CAT0101","group":"基本信息","customRule":null},
 {"name":"materialName","label":"物料名称","type":"TEXT","required":true,"unique":true,"listShow":true,"selectable":false,"domainSource":"MANUAL","domainValues":[],"refModelCode":null,"refFilter":null,"multiSelect":false,"searchable":true,"popup":false,"encrypted":false,"securityLevel":"PUBLIC","securityRule":null,"defaultValue":null,"group":"基本信息","customRule":null},
 {"name":"spec","label":"规格型号","type":"TEXT","required":false,"unique":false,"listShow":true,"selectable":false,"domainSource":"MANUAL","domainValues":[],"refModelCode":null,"refFilter":null,"multiSelect":false,"searchable":true,"popup":false,"encrypted":false,"securityLevel":"PUBLIC","securityRule":null,"defaultValue":null,"group":"基本信息","customRule":null},
 {"name":"unit","label":"计量单位","type":"TEXT","required":true,"unique":false,"listShow":true,"selectable":true,"domainSource":"MANUAL","domainValues":["吨","千克","米","张"],"refModelCode":null,"refFilter":null,"multiSelect":false,"searchable":true,"popup":false,"encrypted":false,"securityLevel":"PUBLIC","securityRule":null,"defaultValue":"吨","group":"基本信息","customRule":null},
 {"name":"subCategory","label":"细分类","type":"TEXT","required":false,"unique":false,"listShow":true,"selectable":true,"domainSource":"MANUAL","domainValues":["板材","型材","管材","线材"],"refModelCode":null,"refFilter":null,"multiSelect":false,"searchable":false,"popup":false,"encrypted":false,"securityLevel":"PUBLIC","securityRule":null,"defaultValue":null,"group":"基本信息","customRule":null},
 {"name":"price","label":"参考单价","type":"NUMBER","required":false,"unique":false,"listShow":true,"selectable":false,"domainSource":"MANUAL","domainValues":[],"refModelCode":null,"refFilter":null,"multiSelect":false,"searchable":true,"popup":false,"encrypted":false,"securityLevel":"PUBLIC","securityRule":null,"defaultValue":null,"group":"财务信息","customRule":null},
 {"name":"currency","label":"币种","type":"TEXT","required":false,"unique":false,"listShow":true,"selectable":true,"domainSource":"MANUAL","domainValues":["CNY","USD","EUR"],"refModelCode":null,"refFilter":null,"multiSelect":false,"searchable":false,"popup":false,"encrypted":false,"securityLevel":"PUBLIC","securityRule":null,"defaultValue":"CNY","group":"财务信息","customRule":null},
 {"name":"techParam","label":"技术参数","type":"LONG_TEXT","required":false,"unique":false,"listShow":false,"selectable":false,"domainSource":"MANUAL","domainValues":[],"refModelCode":null,"refFilter":null,"multiSelect":false,"searchable":false,"popup":false,"encrypted":false,"securityLevel":"PUBLIC","securityRule":null,"defaultValue":null,"group":"技术参数","customRule":null},
 {"name":"produceDate","label":"生产日期","type":"DATE","required":false,"unique":false,"listShow":true,"selectable":false,"domainSource":"MANUAL","domainValues":[],"refModelCode":null,"refFilter":null,"multiSelect":false,"searchable":false,"popup":false,"encrypted":false,"securityLevel":"PUBLIC","securityRule":null,"defaultValue":null,"group":"技术参数","customRule":null},
 {"name":"contactPhone","label":"联系电话","type":"TEXT","required":false,"unique":false,"listShow":true,"selectable":false,"domainSource":"MANUAL","domainValues":[],"refModelCode":null,"refFilter":null,"multiSelect":false,"searchable":false,"popup":false,"encrypted":false,"securityLevel":"SENSITIVE","securityRule":"手机号格式","defaultValue":null,"group":"技术参数","customRule":"PHONE"}
]',
'[
 {"type":"FIXED","value":"MAT","length":null,"padChar":null,"padSide":null,"seqStart":null,"seqStep":null,"refModelCode":null,"refField":null},
 {"type":"FIELD_REF","value":"materialCategory","length":null,"padChar":null,"padSide":null,"seqStart":null,"seqStep":null,"refModelCode":null,"refField":null},
 {"type":"SEQ","value":null,"length":6,"padChar":"0","padSide":"LEFT","seqStart":1,"seqStep":1,"refModelCode":null,"refField":null}
]',
'{"titleField":"materialName","treeEnabled":false,"parentField":null,"childField":null,"displayField":null}',
'原材料主数据演示模型', 'system', '2026-09-23T09:00:00', 'system', '2026-09-23T09:30:00'
WHERE NOT EXISTS (SELECT 1 FROM mdm_model WHERE id = 1);

-- 模型版本快照（v1 创建、v2 上线）
INSERT INTO mdm_model_version (model_id, version_no, status, snapshot, operation, operator, operated_time)
SELECT 1, 1, 'OFFLINE', (SELECT field_defs FROM mdm_model WHERE id = 1), 'CREATE', 'system', '2026-09-23T09:00:00'
WHERE NOT EXISTS (SELECT 1 FROM mdm_model_version WHERE model_id = 1 AND version_no = 1);

-- 编码顺序值：原材料模型当前 6
INSERT INTO mdm_code_sequence (model_id, current_value, updated_time)
SELECT 1, 6, '2026-09-23T10:00:00'
WHERE NOT EXISTS (SELECT 1 FROM mdm_code_sequence WHERE model_id = 1);

-- 质量规则（原材料模型）
INSERT INTO mdm_quality_rule (id, model_id, rule_type, field_name, expression, severity, message, enabled, created_by, created_time)
SELECT 1, 1, 'COMPLETENESS', 'materialName', '{}', 'CRITICAL', '物料名称不能为空', 1, 'system', '2026-09-23T09:00:00'
WHERE NOT EXISTS (SELECT 1 FROM mdm_quality_rule WHERE id = 1);
INSERT INTO mdm_quality_rule (id, model_id, rule_type, field_name, expression, severity, message, enabled, created_by, created_time)
SELECT 2, 1, 'COMPLIANCE', 'spec', '{"op":"LENGTH_MAX","value":50}', 'WARNING', '规格型号长度不能超过50字符', 1, 'system', '2026-09-23T09:00:00'
WHERE NOT EXISTS (SELECT 1 FROM mdm_quality_rule WHERE id = 2);
INSERT INTO mdm_quality_rule (id, model_id, rule_type, field_name, expression, severity, message, enabled, created_by, created_time)
SELECT 3, 1, 'CONSISTENCY', 'unit', '{"refField":"unit","inDomain":true}', 'INFO', '计量单位必须在预定义值域内（吨/千克/米/张）', 1, 'system', '2026-09-23T09:00:00'
WHERE NOT EXISTS (SELECT 1 FROM mdm_quality_rule WHERE id = 3);

-- 下游系统注册
INSERT INTO mdm_downstream_system (id, code, name, sys_type, endpoint, enabled, created_by, created_time)
SELECT 1, 'ERP', 'ERP系统（SAP）', 'ERP', 'http://erp.example.com/api', 1, 'system', '2026-09-23T09:00:00'
WHERE NOT EXISTS (SELECT 1 FROM mdm_downstream_system WHERE id = 1);
INSERT INTO mdm_downstream_system (id, code, name, sys_type, endpoint, enabled, created_by, created_time)
SELECT 2, 'MES', '制造执行系统', 'MES', 'http://mes.example.com/api', 1, 'system', '2026-09-23T09:00:00'
WHERE NOT EXISTS (SELECT 1 FROM mdm_downstream_system WHERE id = 2);
INSERT INTO mdm_downstream_system (id, code, name, sys_type, endpoint, enabled, created_by, created_time)
SELECT 3, 'WMS', '仓储管理系统', 'WMS', 'http://wms.example.com/api', 1, 'system', '2026-09-23T09:00:00'
WHERE NOT EXISTS (SELECT 1 FROM mdm_downstream_system WHERE id = 3);

-- 演示主数据（原材料模型 6 条）
INSERT INTO mdm_master_data (id, model_id, code, name, attributes, status, collab_status, version_no, created_by, created_time, updated_by, updated_time)
SELECT 1, 1, 'MATCAT0101000001', '不锈钢板304',
'{"materialCategory":"CAT0101","materialName":"不锈钢板304","spec":"2.5*1250*2500","unit":"吨","subCategory":"板材","price":18500.00,"currency":"CNY","techParam":"SUS304 奥氏体不锈钢","produceDate":"2026-08-15","contactPhone":null}',
'VALID', NULL, 1, 'zhangsan', '2026-09-23T10:00:00', 'zhangsan', '2026-09-23T10:00:00'
WHERE NOT EXISTS (SELECT 1 FROM mdm_master_data WHERE id = 1);
INSERT INTO mdm_master_data (id, model_id, code, name, attributes, status, collab_status, version_no, created_by, created_time, updated_by, updated_time)
SELECT 2, 1, 'MATCAT0101000002', '不锈钢板201',
'{"materialCategory":"CAT0101","materialName":"不锈钢板201","spec":"2.0*1220*2440","unit":"吨","subCategory":"板材","price":9800.00,"currency":"CNY","techParam":"SUS201","produceDate":"2026-08-20","contactPhone":null}',
'VALID', NULL, 1, 'zhangsan', '2026-09-23T10:05:00', 'zhangsan', '2026-09-23T10:05:00'
WHERE NOT EXISTS (SELECT 1 FROM mdm_master_data WHERE id = 2);
INSERT INTO mdm_master_data (id, model_id, code, name, attributes, status, collab_status, version_no, created_by, created_time, updated_by, updated_time)
SELECT 3, 1, 'MATCAT0101000003', '铝板5052',
'{"materialCategory":"CAT0101","materialName":"铝板5052","spec":"3.0*1500*3000","unit":"吨","subCategory":"板材","price":23500.00,"currency":"CNY","techParam":"5052 铝镁合金","produceDate":"2026-09-01","contactPhone":null}',
'VALID', NULL, 1, 'lisi', '2026-09-23T10:10:00', 'lisi', '2026-09-23T10:10:00'
WHERE NOT EXISTS (SELECT 1 FROM mdm_master_data WHERE id = 3);
INSERT INTO mdm_master_data (id, model_id, code, name, attributes, status, collab_status, version_no, created_by, created_time, updated_by, updated_time)
SELECT 4, 1, 'MATCAT0101000004', '冷轧钢板Q235',
'{"materialCategory":"CAT0101","materialName":"冷轧钢板Q235","spec":"1.5*1000*2000","unit":"吨","subCategory":"板材","price":4600.00,"currency":"CNY","techParam":"Q235B","produceDate":"2026-09-10","contactPhone":null}',
'VALID', NULL, 2, 'lisi', '2026-09-23T10:15:00', 'wangwu', '2026-09-23T11:00:00'
WHERE NOT EXISTS (SELECT 1 FROM mdm_master_data WHERE id = 4);
INSERT INTO mdm_master_data (id, model_id, code, name, attributes, status, collab_status, version_no, created_by, created_time, updated_by, updated_time)
SELECT 5, 1, 'MATCAT0101000005', '铜棒T2',
'{"materialCategory":"CAT0101","materialName":"铜棒T2","spec":"直径20","unit":"千克","subCategory":"型材","price":68200.00,"currency":"CNY","techParam":"T2 紫铜","produceDate":null,"contactPhone":null}',
'DRAFT', NULL, 1, 'zhangsan', '2026-09-23T10:20:00', 'zhangsan', '2026-09-23T10:20:00'
WHERE NOT EXISTS (SELECT 1 FROM mdm_master_data WHERE id = 5);
INSERT INTO mdm_master_data (id, model_id, code, name, attributes, status, collab_status, version_no, created_by, created_time, updated_by, updated_time)
SELECT 6, 1, 'MATCAT0101000006', '锌锭0#',
'{"materialCategory":"CAT0101","materialName":"锌锭0#","spec":"99.995","unit":"吨","subCategory":null,"price":22800.00,"currency":"CNY","techParam":"0# 锌锭","produceDate":"2026-07-30","contactPhone":null}',
'DISABLED', NULL, 1, 'zhangsan', '2026-09-23T10:25:00', 'zhangsan', '2026-09-23T10:25:00'
WHERE NOT EXISTS (SELECT 1 FROM mdm_master_data WHERE id = 6);

-- 数据版本快照
INSERT INTO mdm_data_version (data_id, model_id, version_no, code, snapshot, operation, operator, operated_time)
SELECT 1, 1, 1, 'MATCAT0101000001', (SELECT attributes FROM mdm_master_data WHERE id = 1), 'CREATE', 'zhangsan', '2026-09-23T10:00:00'
WHERE NOT EXISTS (SELECT 1 FROM mdm_data_version WHERE data_id = 1 AND version_no = 1);
INSERT INTO mdm_data_version (data_id, model_id, version_no, code, snapshot, operation, operator, operated_time)
SELECT 2, 1, 1, 'MATCAT0101000002', (SELECT attributes FROM mdm_master_data WHERE id = 2), 'CREATE', 'zhangsan', '2026-09-23T10:05:00'
WHERE NOT EXISTS (SELECT 1 FROM mdm_data_version WHERE data_id = 2 AND version_no = 1);
INSERT INTO mdm_data_version (data_id, model_id, version_no, code, snapshot, operation, operator, operated_time)
SELECT 3, 1, 1, 'MATCAT0101000003', (SELECT attributes FROM mdm_master_data WHERE id = 3), 'CREATE', 'lisi', '2026-09-23T10:10:00'
WHERE NOT EXISTS (SELECT 1 FROM mdm_data_version WHERE data_id = 3 AND version_no = 1);
INSERT INTO mdm_data_version (data_id, model_id, version_no, code, snapshot, operation, operator, operated_time)
SELECT 4, 1, 1, 'MATCAT0101000004', '{"materialCategory":"CAT0101","materialName":"冷轧钢板Q235","spec":"1.2*1000*2000","unit":"吨","subCategory":"板材","price":4300.00,"currency":"CNY","techParam":"Q235B","produceDate":"2026-09-10","contactPhone":null}', 'CREATE', 'lisi', '2026-09-23T10:15:00'
WHERE NOT EXISTS (SELECT 1 FROM mdm_data_version WHERE data_id = 4 AND version_no = 1);
INSERT INTO mdm_data_version (data_id, model_id, version_no, code, snapshot, operation, operator, operated_time)
SELECT 4, 1, 2, 'MATCAT0101000004', (SELECT attributes FROM mdm_master_data WHERE id = 4), 'UPDATE', 'wangwu', '2026-09-23T11:00:00'
WHERE NOT EXISTS (SELECT 1 FROM mdm_data_version WHERE data_id = 4 AND version_no = 2);
INSERT INTO mdm_data_version (data_id, model_id, version_no, code, snapshot, operation, operator, operated_time)
SELECT 5, 1, 1, 'MATCAT0101000005', (SELECT attributes FROM mdm_master_data WHERE id = 5), 'CREATE', 'zhangsan', '2026-09-23T10:20:00'
WHERE NOT EXISTS (SELECT 1 FROM mdm_data_version WHERE data_id = 5 AND version_no = 1);
INSERT INTO mdm_data_version (data_id, model_id, version_no, code, snapshot, operation, operator, operated_time)
SELECT 6, 1, 1, 'MATCAT0101000006', (SELECT attributes FROM mdm_master_data WHERE id = 6), 'CREATE', 'zhangsan', '2026-09-23T10:25:00'
WHERE NOT EXISTS (SELECT 1 FROM mdm_data_version WHERE data_id = 6 AND version_no = 1);

-- 操作日志示例
INSERT INTO mdm_operation_log (biz_type, operation, target_id, target_desc, detail, operator, operated_time)
SELECT 'MODEL', 'ONLINE', '1', '原材料模型上线', '{"modelCode":"MAT_RAW","version":2}', 'system', '2026-09-23T09:30:00'
WHERE NOT EXISTS (SELECT 1 FROM mdm_operation_log WHERE biz_type = 'MODEL' AND operation = 'ONLINE' AND target_id = '1');
INSERT INTO mdm_operation_log (biz_type, operation, target_id, target_desc, detail, operator, operated_time)
SELECT 'DATA', 'CREATE', '1', '新增主数据 MATCAT0101000001', '{"modelCode":"MAT_RAW"}', 'zhangsan', '2026-09-23T10:00:00'
WHERE NOT EXISTS (SELECT 1 FROM mdm_operation_log WHERE biz_type = 'DATA' AND operation = 'CREATE' AND target_id = '1');
