package com.hp.aiagent.maintenance.controller;

import com.hp.aiagent.maintenance.model.DashboardSummary;
import com.hp.aiagent.maintenance.model.DeviceCreateRequest;
import com.hp.aiagent.maintenance.model.DeviceAsset;
import com.hp.aiagent.maintenance.model.DiagnosisResult;
import com.hp.aiagent.maintenance.model.FaultCase;
import com.hp.aiagent.maintenance.model.CompletionItem;
import com.hp.aiagent.maintenance.model.ImageAnalysisRequest;
import com.hp.aiagent.maintenance.model.ImageAnalysisResult;
import com.hp.aiagent.maintenance.model.InspectionRecord;
import com.hp.aiagent.maintenance.model.InspectionRequest;
import com.hp.aiagent.maintenance.model.KnowledgeContribution;
import com.hp.aiagent.maintenance.model.KnowledgeContributionRequest;
import com.hp.aiagent.maintenance.model.KnowledgeReviewRequest;
import com.hp.aiagent.maintenance.model.KnowledgeSearchRequest;
import com.hp.aiagent.maintenance.model.MaintenanceTask;
import com.hp.aiagent.maintenance.model.MaintenanceTaskCreateRequest;
import com.hp.aiagent.maintenance.model.ModuleCapability;
import com.hp.aiagent.maintenance.model.PageResult;
import com.hp.aiagent.maintenance.model.ReportCorrectionRecord;
import com.hp.aiagent.maintenance.model.ReportCorrectionRequest;
import com.hp.aiagent.maintenance.model.ReportResult;
import com.hp.aiagent.maintenance.model.RoleProfile;
import com.hp.aiagent.maintenance.model.TaskArchiveResult;
import com.hp.aiagent.maintenance.model.TaskFlowEvent;
import com.hp.aiagent.maintenance.model.TaskFlowUpdateRequest;
import com.hp.aiagent.maintenance.model.TaskStatusUpdateRequest;
import com.hp.aiagent.maintenance.model.VisionAnalysisRequest;
import com.hp.aiagent.maintenance.model.VisionAnalysisResult;
import com.hp.aiagent.maintenance.service.MaintenanceService;
import com.hp.aiagent.maintenance.service.VisionAnalysisService;
import com.hp.aiagent.rag.LocalKnowledgeService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/maintenance")
public class MaintenanceController {

    @Resource
    private MaintenanceService maintenanceService;

    @Resource
    private VisionAnalysisService visionAnalysisService;

    @GetMapping("/dashboard")
    public DashboardSummary dashboard() {
        return maintenanceService.dashboard();
    }

    @GetMapping("/completion")
    public List<CompletionItem> completion() {
        return maintenanceService.completionOverview();
    }

    @PostMapping("/demo/reset")
    public DashboardSummary resetDemoData() {
        return maintenanceService.resetDemoData();
    }

    @GetMapping("/devices")
    public List<DeviceAsset> devices() {
        return maintenanceService.listDevices();
    }

    @PostMapping("/devices")
    public DeviceAsset createDevice(@RequestBody DeviceCreateRequest request) {
        return maintenanceService.createDevice(request);
    }

    @GetMapping("/roles")
    public List<RoleProfile> roles() {
        return maintenanceService.listRoles();
    }

    @GetMapping("/modules")
    public List<ModuleCapability> modules() {
        return maintenanceService.listModules();
    }

    @GetMapping("/cases")
    public List<FaultCase> cases() {
        return maintenanceService.listFaultCases();
    }

    @GetMapping("/tasks")
    public List<MaintenanceTask> tasks() {
        return maintenanceService.listTasks();
    }

    @GetMapping("/tasks/page")
    public PageResult<MaintenanceTask> tasksPage(@RequestParam(required = false) String status,
                                                 @RequestParam(defaultValue = "1") Integer page,
                                                 @RequestParam(defaultValue = "20") Integer size) {
        return maintenanceService.listTasksPage(status, page, size);
    }

    @PostMapping("/tasks")
    public MaintenanceTask createTask(@RequestBody MaintenanceTaskCreateRequest request) {
        return maintenanceService.createTask(request);
    }

    @GetMapping("/knowledge")
    public List<String> knowledge() {
        return maintenanceService.listKnowledgeDocuments();
    }

    @PostMapping("/knowledge/search")
    public LocalKnowledgeService.KnowledgeSearchResult searchKnowledge(@RequestBody KnowledgeSearchRequest request) {
        return maintenanceService.searchKnowledge(request.query());
    }

    @GetMapping("/knowledge/contributions")
    public List<KnowledgeContribution> knowledgeContributions() {
        return maintenanceService.listKnowledgeContributions();
    }

    @PostMapping("/knowledge/contributions")
    public KnowledgeContribution submitKnowledge(@RequestBody KnowledgeContributionRequest request) {
        return maintenanceService.submitKnowledge(request);
    }

    @PostMapping("/knowledge/contributions/{id}/review")
    public KnowledgeContribution reviewKnowledge(@PathVariable String id, @RequestBody KnowledgeReviewRequest request) {
        return maintenanceService.reviewKnowledge(id, request);
    }

    @PostMapping("/diagnose")
    public DiagnosisResult diagnose(@RequestBody InspectionRequest request) {
        return maintenanceService.diagnose(request);
    }

    @GetMapping("/inspections")
    public List<InspectionRecord> inspections(@RequestParam(required = false) String deviceId,
                                              @RequestParam(required = false) String riskLevel,
                                              @RequestParam(defaultValue = "1") Integer page,
                                              @RequestParam(defaultValue = "20") Integer size) {
        return maintenanceService.listInspectionRecords(page, size, deviceId, riskLevel);
    }

    @PostMapping("/image/analyze")
    public ImageAnalysisResult analyzeImage(@RequestBody ImageAnalysisRequest request) {
        return maintenanceService.analyzeImage(request);
    }

    @PostMapping("/vision/analyze")
    public VisionAnalysisResult analyzeVision(@RequestBody VisionAnalysisRequest request) {
        return visionAnalysisService.analyze(request);
    }

    @PostMapping("/vision/upload")
    public VisionAnalysisResult analyzeUploadedVision(@RequestParam(defaultValue = "其他") String deviceType,
                                                      @RequestParam(required = false) String visualDescription,
                                                      @RequestParam(required = false) String question,
                                                      @RequestParam("file") MultipartFile file) throws IOException {
        return visionAnalysisService.analyzeUploadedImage(
                deviceType,
                visualDescription,
                question,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getBytes()
        );
    }

    @PostMapping("/tasks/{taskId}/status")
    public MaintenanceTask updateTaskStatus(@PathVariable String taskId, @RequestBody TaskStatusUpdateRequest request) {
        return maintenanceService.updateTaskStatus(taskId, request.status());
    }

    @PostMapping("/tasks/{taskId}/flow")
    public MaintenanceTask updateTaskFlow(@PathVariable String taskId, @RequestBody TaskFlowUpdateRequest request) {
        return maintenanceService.updateTaskStatus(taskId, request.status(), request.operatorRole(), request.note());
    }

    @PostMapping("/tasks/{taskId}/archive")
    public TaskArchiveResult archiveTask(@PathVariable String taskId) {
        return maintenanceService.archiveTask(taskId);
    }

    @GetMapping("/tasks/flow")
    public List<TaskFlowEvent> taskFlowEvents() {
        return maintenanceService.listTaskFlowEvents();
    }

    @PostMapping("/reports")
    public ReportResult generateReport(@RequestBody DiagnosisResult diagnosisResult) {
        return maintenanceService.generateReport(diagnosisResult);
    }

    @PostMapping("/reports/corrections")
    public ReportResult correctReport(@RequestBody ReportCorrectionRequest request) {
        return maintenanceService.correctReport(request);
    }

    @GetMapping("/reports")
    public List<ReportResult> reports() {
        return maintenanceService.listReports();
    }

    @GetMapping("/reports/page")
    public PageResult<ReportResult> reportsPage(@RequestParam(required = false) String riskLevel,
                                                @RequestParam(defaultValue = "1") Integer page,
                                                @RequestParam(defaultValue = "20") Integer size) {
        return maintenanceService.listReportsPage(riskLevel, page, size);
    }

    @GetMapping("/reports/corrections")
    public List<ReportCorrectionRecord> reportCorrections(@RequestParam(required = false) String reportId,
                                                          @RequestParam(defaultValue = "1") Integer page,
                                                          @RequestParam(defaultValue = "20") Integer size) {
        return maintenanceService.listReportCorrections(page, size, reportId);
    }
}
