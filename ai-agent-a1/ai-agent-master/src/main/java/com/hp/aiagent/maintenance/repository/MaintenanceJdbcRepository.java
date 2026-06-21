package com.hp.aiagent.maintenance.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.aiagent.maintenance.model.DeviceAsset;
import com.hp.aiagent.maintenance.model.FaultCase;
import com.hp.aiagent.maintenance.model.KnowledgeContribution;
import com.hp.aiagent.maintenance.model.MaintenanceTask;
import com.hp.aiagent.maintenance.model.ReportResult;
import com.hp.aiagent.maintenance.model.TaskFlowEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Repository
@DependsOn("maintenanceDatabaseInitializer")
@ConditionalOnProperty(name = "app.persistence.jdbc.enabled", havingValue = "true")
public class MaintenanceJdbcRepository {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public MaintenanceJdbcRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public List<DeviceAsset> listDevices() {
        return jdbcTemplate.query("""
                select id, name, device_type, location, status, risk_level, sensor_json, last_inspection_time
                from maintenance_device_asset
                order by created_at desc, id
                """, deviceMapper());
    }

    public void saveDevice(DeviceAsset device) {
        jdbcTemplate.update("""
                insert into maintenance_device_asset
                (id, name, device_type, location, status, risk_level, sensor_json, last_inspection_time, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, current_timestamp)
                on conflict (id) do update set
                    name = excluded.name,
                    device_type = excluded.device_type,
                    location = excluded.location,
                    status = excluded.status,
                    risk_level = excluded.risk_level,
                    sensor_json = excluded.sensor_json,
                    last_inspection_time = excluded.last_inspection_time,
                    updated_at = current_timestamp
                """,
                device.id(),
                device.name(),
                device.type(),
                device.location(),
                device.status(),
                device.riskLevel(),
                toJson(device.sensors()),
                parseDisplayTime(device.lastInspectionTime())
        );
    }

    public List<FaultCase> listFaultCases() {
        return jdbcTemplate.query("""
                select id, device_type, fault_name, symptom_json, image_feature_json, cause, solution, severity
                from maintenance_fault_case
                order by created_at desc, id
                """, faultCaseMapper());
    }

    public void saveFaultCase(FaultCase faultCase) {
        jdbcTemplate.update("""
                insert into maintenance_fault_case
                (id, device_type, fault_name, symptom_json, image_feature_json, cause, solution, severity, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, current_timestamp)
                on conflict (id) do update set
                    device_type = excluded.device_type,
                    fault_name = excluded.fault_name,
                    symptom_json = excluded.symptom_json,
                    image_feature_json = excluded.image_feature_json,
                    cause = excluded.cause,
                    solution = excluded.solution,
                    severity = excluded.severity,
                    updated_at = current_timestamp
                """,
                faultCase.id(),
                faultCase.deviceType(),
                faultCase.faultName(),
                toJson(faultCase.symptoms()),
                toJson(faultCase.imageFeatures()),
                faultCase.cause(),
                faultCase.solution(),
                faultCase.severity()
        );
    }

    public List<MaintenanceTask> listTasks() {
        return jdbcTemplate.query("""
                select id, device_id, title, priority, status, step_json, spare_part_json, acceptance_json, created_at
                from maintenance_task
                order by created_at desc, id
                """, taskMapper());
    }

    public void saveTask(MaintenanceTask task) {
        jdbcTemplate.update("""
                insert into maintenance_task
                (id, device_id, title, priority, status, step_json, spare_part_json, acceptance_json, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, current_timestamp)
                on conflict (id) do update set
                    device_id = excluded.device_id,
                    title = excluded.title,
                    priority = excluded.priority,
                    status = excluded.status,
                    step_json = excluded.step_json,
                    spare_part_json = excluded.spare_part_json,
                    acceptance_json = excluded.acceptance_json,
                    updated_at = current_timestamp
                """,
                task.id(),
                task.deviceId(),
                task.title(),
                task.priority(),
                task.status(),
                toJson(task.steps()),
                toJson(task.spareParts()),
                toJson(task.acceptanceCriteria()),
                toTimestamp(task.createdAt())
        );
    }

    public List<TaskFlowEvent> listTaskFlowEvents() {
        return jdbcTemplate.query("""
                select task_id, from_status, to_status, operator_role, note, operated_at
                from maintenance_task_flow_event
                order by operated_at desc
                limit 50
                """, taskFlowMapper());
    }

    public void saveTaskFlowEvent(TaskFlowEvent event) {
        jdbcTemplate.update("""
                insert into maintenance_task_flow_event
                (id, task_id, from_status, to_status, operator_role, note, operated_at)
                values (?, ?, ?, ?, ?, ?, ?)
                """,
                "FLOW-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT),
                event.taskId(),
                event.fromStatus(),
                event.toStatus(),
                event.operatorRole(),
                event.note(),
                toTimestamp(event.operatedAt())
        );
    }

    public List<ReportResult> listReports() {
        return jdbcTemplate.query("""
                select report_id, title, risk_level, section_json, markdown, generated_at, markdown_download_url, pdf_download_url
                from maintenance_report
                order by generated_at desc
                limit 20
                """, reportMapper());
    }

    public void saveReport(ReportResult report) {
        jdbcTemplate.update("""
                insert into maintenance_report
                (report_id, title, risk_level, section_json, markdown, generated_at, markdown_download_url, pdf_download_url)
                values (?, ?, ?, ?, ?, ?, ?, ?)
                on conflict (report_id) do update set
                    title = excluded.title,
                    risk_level = excluded.risk_level,
                    section_json = excluded.section_json,
                    markdown = excluded.markdown,
                    generated_at = excluded.generated_at,
                    markdown_download_url = excluded.markdown_download_url,
                    pdf_download_url = excluded.pdf_download_url
                """,
                report.reportId(),
                report.title(),
                report.riskLevel(),
                toJson(report.sections()),
                report.markdown(),
                toTimestamp(report.generatedAt()),
                report.markdownDownloadUrl(),
                report.pdfDownloadUrl()
        );
    }

    public List<KnowledgeContribution> listKnowledgeContributions() {
        return jdbcTemplate.query("""
                select id, title, device_type, fault_name, symptom_json, image_feature_json, cause, solution,
                       content, submitter, status, review_note, created_at, reviewed_at,
                       markdown_download_url, pdf_download_url
                from maintenance_knowledge_contribution
                order by created_at desc, id
                """, knowledgeMapper());
    }

    public void saveKnowledgeContribution(KnowledgeContribution item) {
        jdbcTemplate.update("""
                insert into maintenance_knowledge_contribution
                (id, title, device_type, fault_name, symptom_json, image_feature_json, cause, solution,
                 content, submitter, status, review_note, created_at, reviewed_at, markdown_download_url, pdf_download_url)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                on conflict (id) do update set
                    title = excluded.title,
                    device_type = excluded.device_type,
                    fault_name = excluded.fault_name,
                    symptom_json = excluded.symptom_json,
                    image_feature_json = excluded.image_feature_json,
                    cause = excluded.cause,
                    solution = excluded.solution,
                    content = excluded.content,
                    submitter = excluded.submitter,
                    status = excluded.status,
                    review_note = excluded.review_note,
                    reviewed_at = excluded.reviewed_at,
                    markdown_download_url = excluded.markdown_download_url,
                    pdf_download_url = excluded.pdf_download_url
                """,
                item.id(),
                item.title(),
                item.deviceType(),
                item.faultName(),
                toJson(item.symptoms()),
                toJson(item.imageFeatures()),
                item.cause(),
                item.solution(),
                item.content(),
                item.submitter(),
                item.status(),
                item.reviewNote(),
                toTimestamp(item.createdAt()),
                toTimestamp(item.reviewedAt()),
                item.markdownDownloadUrl(),
                item.pdfDownloadUrl()
        );
    }

    private RowMapper<DeviceAsset> deviceMapper() {
        return (rs, rowNum) -> new DeviceAsset(
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("device_type"),
                rs.getString("location"),
                rs.getString("status"),
                rs.getString("risk_level"),
                fromJsonList(rs.getString("sensor_json")),
                formatTime(toLocalDateTime(rs.getTimestamp("last_inspection_time")))
        );
    }

    private RowMapper<FaultCase> faultCaseMapper() {
        return (rs, rowNum) -> new FaultCase(
                rs.getString("id"),
                rs.getString("device_type"),
                rs.getString("fault_name"),
                fromJsonList(rs.getString("symptom_json")),
                fromJsonList(rs.getString("image_feature_json")),
                rs.getString("cause"),
                rs.getString("solution"),
                rs.getInt("severity")
        );
    }

    private RowMapper<MaintenanceTask> taskMapper() {
        return (rs, rowNum) -> new MaintenanceTask(
                rs.getString("id"),
                rs.getString("device_id"),
                rs.getString("title"),
                rs.getString("priority"),
                rs.getString("status"),
                fromJsonList(rs.getString("step_json")),
                fromJsonList(rs.getString("spare_part_json")),
                fromJsonList(rs.getString("acceptance_json")),
                toLocalDateTime(rs.getTimestamp("created_at"))
        );
    }

    private RowMapper<TaskFlowEvent> taskFlowMapper() {
        return (rs, rowNum) -> new TaskFlowEvent(
                rs.getString("task_id"),
                rs.getString("from_status"),
                rs.getString("to_status"),
                rs.getString("operator_role"),
                rs.getString("note"),
                toLocalDateTime(rs.getTimestamp("operated_at"))
        );
    }

    private RowMapper<ReportResult> reportMapper() {
        return (rs, rowNum) -> new ReportResult(
                rs.getString("report_id"),
                rs.getString("title"),
                rs.getString("risk_level"),
                fromJsonList(rs.getString("section_json")),
                rs.getString("markdown"),
                toLocalDateTime(rs.getTimestamp("generated_at")),
                rs.getString("markdown_download_url"),
                rs.getString("pdf_download_url")
        );
    }

    private RowMapper<KnowledgeContribution> knowledgeMapper() {
        return (rs, rowNum) -> new KnowledgeContribution(
                rs.getString("id"),
                rs.getString("title"),
                rs.getString("device_type"),
                rs.getString("fault_name"),
                fromJsonList(rs.getString("symptom_json")),
                fromJsonList(rs.getString("image_feature_json")),
                rs.getString("cause"),
                rs.getString("solution"),
                rs.getString("content"),
                rs.getString("submitter"),
                rs.getString("status"),
                rs.getString("review_note"),
                toLocalDateTime(rs.getTimestamp("created_at")),
                toLocalDateTime(rs.getTimestamp("reviewed_at")),
                rs.getString("markdown_download_url"),
                rs.getString("pdf_download_url")
        );
    }

    private String toJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (Exception ex) {
            throw new IllegalArgumentException("列表字段序列化失败", ex);
        }
    }

    private List<String> fromJsonList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST_TYPE);
        } catch (Exception ex) {
            return List.of();
        }
    }

    private Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private String formatTime(LocalDateTime value) {
        return value == null ? "" : DISPLAY_TIME.format(value);
    }

    private Timestamp parseDisplayTime(String value) {
        if (value == null || value.isBlank()) {
            return toTimestamp(LocalDateTime.now());
        }
        try {
            return Timestamp.valueOf(value.replace('T', ' ').substring(0, 19));
        } catch (Exception ignored) {
            try {
                return Timestamp.valueOf(value.replace('T', ' ').substring(0, 16) + ":00");
            } catch (Exception ignoredAgain) {
                return toTimestamp(LocalDateTime.now());
            }
        }
    }
}
