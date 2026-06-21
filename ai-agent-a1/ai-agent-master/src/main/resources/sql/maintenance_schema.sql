create table if not exists maintenance_device_asset (
    id varchar(64) primary key,
    name varchar(128) not null,
    device_type varchar(64) not null,
    location varchar(255),
    status varchar(32) not null,
    risk_level varchar(32) not null,
    sensor_json text,
    last_inspection_time timestamp,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp
);

create table if not exists maintenance_fault_case (
    id varchar(64) primary key,
    device_type varchar(64) not null,
    fault_name varchar(128) not null,
    symptom_json text,
    image_feature_json text,
    cause text,
    solution text,
    severity int not null default 1,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp
);

create table if not exists maintenance_inspection_record (
    id varchar(64) primary key,
    device_id varchar(64),
    device_type varchar(64),
    description text,
    temperature decimal(8, 2),
    vibration decimal(8, 2),
    current_value decimal(8, 2),
    image_feature_json text,
    risk_level varchar(32),
    score int,
    evidence_json text,
    created_at timestamp default current_timestamp
);

create table if not exists maintenance_task (
    id varchar(64) primary key,
    device_id varchar(64),
    title varchar(255) not null,
    priority varchar(32) not null,
    status varchar(32) not null,
    step_json text,
    spare_part_json text,
    acceptance_json text,
    assignee varchar(64),
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp
);

create table if not exists maintenance_task_flow_event (
    id varchar(64) primary key,
    task_id varchar(64) not null,
    from_status varchar(32),
    to_status varchar(32) not null,
    operator_role varchar(64),
    note text,
    operated_at timestamp default current_timestamp
);

create table if not exists maintenance_report (
    report_id varchar(64) primary key,
    title varchar(255) not null,
    risk_level varchar(32),
    section_json text,
    markdown text,
    generated_at timestamp default current_timestamp,
    markdown_download_url varchar(512),
    pdf_download_url varchar(512)
);

create table if not exists maintenance_knowledge_contribution (
    id varchar(64) primary key,
    title varchar(255) not null,
    device_type varchar(64),
    fault_name varchar(128) not null,
    symptom_json text,
    image_feature_json text,
    cause text,
    solution text,
    content text,
    submitter varchar(128),
    status varchar(32) not null,
    review_note text,
    created_at timestamp default current_timestamp,
    reviewed_at timestamp,
    markdown_download_url varchar(512),
    pdf_download_url varchar(512)
);

create index if not exists idx_maintenance_device_risk on maintenance_device_asset(risk_level);
create index if not exists idx_maintenance_case_device_type on maintenance_fault_case(device_type);
create index if not exists idx_maintenance_task_status on maintenance_task(status);
create index if not exists idx_maintenance_task_flow_task on maintenance_task_flow_event(task_id);
create index if not exists idx_maintenance_report_generated_at on maintenance_report(generated_at);
create index if not exists idx_maintenance_knowledge_status on maintenance_knowledge_contribution(status);
