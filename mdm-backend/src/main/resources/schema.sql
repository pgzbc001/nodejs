-- ============================================================
-- 主数据管理平台 SQLite Schema（MDM-0001）
-- 详见 02-backend/01_function/MDM-0001/sql/sql-mdm-0001-ddl.md
-- ============================================================

CREATE TABLE IF NOT EXISTS mdm_category (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    code            TEXT    NOT NULL,
    name            TEXT    NOT NULL,
    parent_id       INTEGER,
    sort_no         INTEGER DEFAULT 0,
    description     TEXT,
    created_by      TEXT,
    created_time    TEXT,
    updated_by      TEXT,
    updated_time    TEXT,
    del_flag        INTEGER DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_category_code ON mdm_category(code) WHERE del_flag = 0;
CREATE INDEX IF NOT EXISTS idx_category_parent ON mdm_category(parent_id);

CREATE TABLE IF NOT EXISTS mdm_model (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    code            TEXT    NOT NULL,
    name            TEXT    NOT NULL,
    category_id     INTEGER NOT NULL,
    dept            TEXT,
    status          TEXT    DEFAULT 'OFFLINE',
    version_no      INTEGER DEFAULT 1,
    field_defs      TEXT    NOT NULL,
    code_rules      TEXT,
    ext_config      TEXT,
    description     TEXT,
    created_by      TEXT,
    created_time    TEXT,
    updated_by      TEXT,
    updated_time    TEXT,
    del_flag        INTEGER DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_model_code ON mdm_model(code) WHERE del_flag = 0;
CREATE INDEX IF NOT EXISTS idx_model_category ON mdm_model(category_id);

CREATE TABLE IF NOT EXISTS mdm_model_version (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    model_id        INTEGER NOT NULL,
    version_no      INTEGER NOT NULL,
    status          TEXT,
    snapshot        TEXT    NOT NULL,
    operation       TEXT,
    operator        TEXT,
    operated_time   TEXT,
    del_flag        INTEGER DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_mver_model ON mdm_model_version(model_id, version_no);

CREATE TABLE IF NOT EXISTS mdm_master_data (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    model_id        INTEGER NOT NULL,
    code            TEXT    NOT NULL,
    name            TEXT    NOT NULL,
    attributes      TEXT    NOT NULL,
    status          TEXT    DEFAULT 'VALID',
    collab_status   TEXT,
    version_no      INTEGER DEFAULT 1,
    created_by      TEXT,
    created_time    TEXT,
    updated_by      TEXT,
    updated_time    TEXT,
    del_flag        INTEGER DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_data_code ON mdm_master_data(model_id, code) WHERE del_flag = 0;
CREATE INDEX IF NOT EXISTS idx_data_model ON mdm_master_data(model_id, status);

CREATE TABLE IF NOT EXISTS mdm_data_version (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    data_id         INTEGER NOT NULL,
    model_id        INTEGER NOT NULL,
    version_no      INTEGER NOT NULL,
    code            TEXT,
    snapshot        TEXT    NOT NULL,
    operation       TEXT,
    operator        TEXT,
    operated_time   TEXT,
    remark          TEXT,
    del_flag        INTEGER DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_dver_data ON mdm_data_version(data_id, version_no);

CREATE TABLE IF NOT EXISTS mdm_code_sequence (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    model_id        INTEGER NOT NULL,
    current_value   INTEGER NOT NULL DEFAULT 0,
    updated_time    TEXT,
    UNIQUE(model_id)
);

CREATE TABLE IF NOT EXISTS mdm_quality_rule (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    model_id        INTEGER,
    rule_type       TEXT    NOT NULL,
    field_name      TEXT,
    expression      TEXT    NOT NULL,
    severity        TEXT    NOT NULL,
    message         TEXT    NOT NULL,
    enabled         INTEGER DEFAULT 1,
    created_by      TEXT,
    created_time    TEXT,
    updated_by      TEXT,
    updated_time    TEXT,
    del_flag        INTEGER DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_qrule_model ON mdm_quality_rule(model_id, enabled);

CREATE TABLE IF NOT EXISTS mdm_quality_result (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    data_id         INTEGER,
    model_id        INTEGER NOT NULL,
    rule_id         INTEGER,
    severity        TEXT    NOT NULL,
    message         TEXT,
    ignored         INTEGER DEFAULT 0,
    ignore_reason   TEXT,
    checked_time    TEXT,
    del_flag        INTEGER DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_qres_data ON mdm_quality_result(data_id);

CREATE TABLE IF NOT EXISTS mdm_downstream_system (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    code            TEXT    NOT NULL,
    name            TEXT    NOT NULL,
    sys_type        TEXT,
    endpoint        TEXT,
    enabled         INTEGER DEFAULT 1,
    created_by      TEXT,
    created_time    TEXT,
    updated_by      TEXT,
    updated_time    TEXT,
    del_flag        INTEGER DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_code ON mdm_downstream_system(code) WHERE del_flag = 0;

CREATE TABLE IF NOT EXISTS mdm_push_log (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    data_id         INTEGER NOT NULL,
    data_code       TEXT,
    model_id        INTEGER NOT NULL,
    system_id       INTEGER NOT NULL,
    system_code     TEXT,
    result          TEXT    NOT NULL,
    detail          TEXT,
    operator        TEXT,
    pushed_time     TEXT,
    del_flag        INTEGER DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_push_data ON mdm_push_log(data_id);
CREATE INDEX IF NOT EXISTS idx_push_time ON mdm_push_log(pushed_time);

CREATE TABLE IF NOT EXISTS mdm_collaboration (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    data_id         INTEGER NOT NULL,
    model_id        INTEGER NOT NULL,
    reason          TEXT,
    collaborator    TEXT,
    status          TEXT    DEFAULT 'PENDING',
    applicant       TEXT,
    apply_time      TEXT,
    confirm_time    TEXT,
    confirm_comment TEXT,
    del_flag        INTEGER DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_collab_data ON mdm_collaboration(data_id);

CREATE TABLE IF NOT EXISTS mdm_operation_log (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    biz_type        TEXT    NOT NULL,
    operation       TEXT    NOT NULL,
    target_id       TEXT,
    target_desc     TEXT,
    detail          TEXT,
    operator        TEXT,
    operated_time   TEXT,
    del_flag        INTEGER DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_oplog_time ON mdm_operation_log(operated_time, biz_type, operation);
