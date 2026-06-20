package com.hp.aiagent.maintenance.service;

import com.hp.aiagent.maintenance.model.DashboardSummary;
import com.hp.aiagent.maintenance.model.DeviceAsset;
import com.hp.aiagent.maintenance.model.DiagnosisResult;
import com.hp.aiagent.maintenance.model.FaultCase;
import com.hp.aiagent.maintenance.model.CompletionItem;
import com.hp.aiagent.maintenance.model.ImageAnalysisRequest;
import com.hp.aiagent.maintenance.model.ImageAnalysisResult;
import com.hp.aiagent.maintenance.model.InspectionRequest;
import com.hp.aiagent.maintenance.model.MaintenanceTask;
import com.hp.aiagent.maintenance.model.ModuleCapability;
import com.hp.aiagent.maintenance.model.ReportResult;
import com.hp.aiagent.maintenance.model.RoleProfile;
import com.hp.aiagent.maintenance.model.TaskFlowEvent;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class MaintenanceService {

    private final List<DeviceAsset> devices = new CopyOnWriteArrayList<>();
    private final List<FaultCase> faultCases = new CopyOnWriteArrayList<>();
    private final List<MaintenanceTask> tasks = new CopyOnWriteArrayList<>();
    private final List<ReportResult> reports = new CopyOnWriteArrayList<>();
    private final List<RoleProfile> roles = new CopyOnWriteArrayList<>();
    private final List<ModuleCapability> modules = new CopyOnWriteArrayList<>();
    private final List<TaskFlowEvent> taskFlowEvents = new CopyOnWriteArrayList<>();

    public MaintenanceService() {
        devices.addAll(List.of(
                new DeviceAsset("DEV-FAN-01", "一号引风机", "风机", "锅炉房 A 区", "运行", "关注",
                        List.of("振动", "温度", "电流", "噪声"), "2026-06-10 09:20"),
                new DeviceAsset("DEV-PUMP-02", "循环水泵二号", "泵", "动力站 B 区", "运行", "预警",
                        List.of("入口压力", "出口压力", "振动", "温度"), "2026-06-10 15:42"),
                new DeviceAsset("DEV-MOTOR-03", "输送线主电机", "电机", "产线 3 号位", "待检修", "严重",
                        List.of("电流", "绝缘电阻", "温度", "外观图片"), "2026-06-11 08:10"),
                new DeviceAsset("DEV-GEAR-04", "减速齿轮箱", "齿轮箱", "包装线末端", "运行", "正常",
                        List.of("油温", "振动", "噪声", "油液"), "2026-06-09 17:05")
        ));

        faultCases.addAll(List.of(
                new FaultCase("CASE-001", "风机", "轴承早期磨损",
                        List.of("温度升高", "周期性异响", "振动增大"),
                        List.of("油污", "轴承座发热", "轻微磨痕"),
                        "润滑不足或轴承游隙异常导致滚动体局部磨损",
                        "补充润滑，采集频谱，检查轴承游隙，必要时计划停机更换", 3),
                new FaultCase("CASE-002", "泵", "汽蚀与入口堵塞",
                        List.of("压力波动", "流量下降", "泵体振动"),
                        List.of("入口滤网污堵", "管路锈蚀", "密封处漏液"),
                        "入口阻力过大或液位不足导致汽蚀",
                        "检查入口阀门、滤网和液位，排气后复测振动与压力", 4),
                new FaultCase("CASE-003", "电机", "绕组过热与绝缘下降",
                        List.of("外壳高温", "电流异常", "焦味"),
                        List.of("焦痕", "变色", "绝缘破损"),
                        "长期过载、散热不良或绝缘老化引起绕组局部过热",
                        "立即停机断电，测绝缘电阻，检查接线端子和散热通道", 5),
                new FaultCase("CASE-004", "齿轮箱", "齿面点蚀",
                        List.of("啮合噪声", "油液金属屑", "振动边频"),
                        List.of("金属屑", "齿面麻点", "油液浑浊"),
                        "载荷冲击或润滑油污染导致齿面疲劳",
                        "取样化验油液，检查齿面，过滤或更换润滑油", 3)
        ));

        tasks.add(new MaintenanceTask("TASK-1001", "DEV-MOTOR-03", "主电机绝缘异常复检", "P1", "待处理",
                List.of("断电挂牌上锁", "测量三相绝缘电阻", "检查接线端子焦痕", "清理散热通道", "复测空载电流"),
                List.of("绝缘表", "红外测温仪", "接线端子", "绝缘胶带"),
                List.of("绝缘电阻满足企业标准", "空载电流三相平衡", "试运行 30 分钟无焦味和异常温升"),
                LocalDateTime.now().minusHours(2)));

        roles.addAll(List.of(
                new RoleProfile("inspector", "巡检员", "现场采集异常、上传图片和填写巡检参数",
                        List.of("查看设备台账", "提交诊断", "上传图片", "查看本人作业单")),
                new RoleProfile("maintainer", "检修员", "接收作业单并执行安全隔离、拆检和复测",
                        List.of("查看作业单", "更新处理中", "提交待验收", "补充检修记录")),
                new RoleProfile("expert", "专家", "复核 AI 结论、确认风险等级和修正案例知识",
                        List.of("专家复核", "修正诊断", "沉淀案例", "批准归档")),
                new RoleProfile("admin", "管理员", "维护用户、权限、模块开关和知识库",
                        List.of("模块管理", "角色管理", "全部数据", "系统配置"))
        ));

        modules.addAll(List.of(
                new ModuleCapability("dashboard", "态势看板", "管理员", "已启用", 95,
                        List.of("风险分布", "异常设备统计", "待处理作业", "最近作业"),
                        List.of("设备台账", "作业单")),
                new ModuleCapability("asset", "设备台账管理", "管理员", "已启用", 88,
                        List.of("设备列表", "传感器清单", "风险标记", "巡检时间"),
                        List.of("内存设备库")),
                new ModuleCapability("case", "案例知识库管理", "专家", "已启用", 86,
                        List.of("故障案例", "症状标签", "图片特征", "处理方案"),
                        List.of("RAG 文档", "案例匹配")),
                new ModuleCapability("diagnosis", "多模态诊断", "巡检员", "已启用", 90,
                        List.of("参数评分", "证据链", "相似案例", "自动作业单"),
                        List.of("设备台账", "案例库", "图片分析")),
                new ModuleCapability("vision", "Qwen 视觉分析", "巡检员", "已启用", 78,
                        List.of("图片 URL 分析", "特征抽取", "视觉结论", "本地兜底"),
                        List.of("DashScope", "Qwen-VL", "案例库")),
                new ModuleCapability("workflow", "作业流转", "检修员", "已启用", 84,
                        List.of("待派工", "处理中", "待验收", "专家复核", "已归档"),
                        List.of("角色权限", "报告中心")),
                new ModuleCapability("report", "报告中心", "专家", "已启用", 82,
                        List.of("报告生成", "Markdown 导出", "报告沉淀", "复盘材料"),
                        List.of("诊断结果", "作业单"))
        ));
    }

    public List<DeviceAsset> listDevices() {
        return devices;
    }

    public List<FaultCase> listFaultCases() {
        return faultCases;
    }

    public List<RoleProfile> listRoles() {
        return roles;
    }

    public List<ModuleCapability> listModules() {
        return modules;
    }

    public List<TaskFlowEvent> listTaskFlowEvents() {
        return taskFlowEvents.stream()
                .sorted(Comparator.comparing(TaskFlowEvent::operatedAt).reversed())
                .limit(50)
                .toList();
    }

    public List<MaintenanceTask> listTasks() {
        return tasks.stream()
                .sorted(Comparator.comparing(MaintenanceTask::createdAt).reversed())
                .toList();
    }

    public List<String> listKnowledgeDocuments() {
        return List.of(
                "paper1.md - 正常运行状态诊断知识",
                "paper2.md - 早期故障诊断知识",
                "paper3.md - 严重故障诊断知识",
                "maintenance_workflow.md - 检修作业闭环知识"
        );
    }

    public List<CompletionItem> completionOverview() {
        return List.of(
                new CompletionItem("前端工作台", "已实现", 95,
                        "看板、台账、案例、诊断、图片分析、作业单、知识库和 AI 问答均可演示",
                        "补充大屏适配和答辩录屏素材"),
                new CompletionItem("检修业务后端", "已实现", 90,
                        "设备、案例、任务、诊断、图片特征和报告接口已跑通",
                        "接入数据库 Repository，替换内存演示数据"),
                new CompletionItem("大模型问答", "已实现", 85,
                        "DashScope/Qwen 接入成功，支持同步和 SSE 流式问答",
                        "增加失败降级话术和模型调用审计"),
                new CompletionItem("角色权限", "已实现", 82,
                        "已内置巡检员、检修员、专家、管理员四类角色和操作权限",
                        "接入登录态后按真实用户鉴权"),
                new CompletionItem("RAG 知识库", "原型完成", 75,
                        "本地 Markdown 知识库已加载到内存向量库并用于检修问答",
                        "生产部署时启用 PostgreSQL + PgVector 持久化"),
                new CompletionItem("多模态图片能力", "已实现", 80,
                        "已支持图片描述特征抽取、Qwen-VL 图片 URL 分析、相似案例匹配和诊断联动",
                        "融合 image-backend 的上传、审核、标签和相似检索"),
                new CompletionItem("作业闭环", "已实现", 90,
                        "诊断结果可自动生成作业单，支持角色约束、状态流转、流转日志和报告导出",
                        "补充负责人、审批、归档和复盘沉淀流程"),
                new CompletionItem("模块管理", "已实现", 84,
                        "已沉淀模块清单、负责人角色、依赖关系、启用状态和成熟度",
                        "接入配置中心后支持动态开关"),
                new CompletionItem("工程部署", "演示可用", 65,
                        "Java 21、前后端启动脚本、环境变量模板和演示模式已整理",
                        "安装 Node.js 18+ 后补充前端构建验证；接入数据库初始化脚本")
        );
    }

    public DashboardSummary dashboard() {
        Map<String, Integer> riskDistribution = new LinkedHashMap<>();
        for (String risk : List.of("正常", "关注", "预警", "严重")) {
            riskDistribution.put(risk, 0);
        }
        devices.forEach(device -> riskDistribution.computeIfPresent(device.riskLevel(), (key, value) -> value + 1));

        int warningCount = (int) devices.stream()
                .filter(device -> !"正常".equals(device.riskLevel()))
                .count();
        int openTaskCount = (int) tasks.stream()
                .filter(task -> !"已归档".equals(task.status()))
                .count();
        return new DashboardSummary(
                devices.size(),
                warningCount,
                openTaskCount,
                faultCases.size(),
                riskDistribution,
                listTasks().stream().limit(5).toList()
        );
    }

    public DiagnosisResult diagnose(InspectionRequest request) {
        int score = 0;
        List<String> evidence = new ArrayList<>();
        List<String> possibleCauses = new ArrayList<>();
        List<String> recommendedActions = new ArrayList<>();

        if (request.temperature() != null) {
            if (request.temperature() >= 85) {
                score += 35;
                evidence.add("温度达到 " + request.temperature() + "℃，超过常规巡检关注阈值");
            } else if (request.temperature() >= 70) {
                score += 18;
                evidence.add("温度达到 " + request.temperature() + "℃，建议持续跟踪");
            }
        }
        if (request.vibration() != null) {
            if (request.vibration() >= 7.1) {
                score += 35;
                evidence.add("振动 RMS " + request.vibration() + "mm/s，达到预警区间");
            } else if (request.vibration() >= 4.5) {
                score += 18;
                evidence.add("振动 RMS " + request.vibration() + "mm/s，有劣化趋势");
            }
        }
        if (request.current() != null && request.current() >= 120) {
            score += 20;
            evidence.add("电流 " + request.current() + "A，存在负载或电气异常风险");
        }
        if (containsAny(request.description(), List.of("异响", "焦味", "漏油", "压力波动", "裂纹"))) {
            score += 20;
            evidence.add("现场描述包含明显异常现象");
        }
        if (request.imageFeatures() != null && !request.imageFeatures().isEmpty()) {
            score += Math.min(25, request.imageFeatures().size() * 8);
            evidence.add("图片特征包含：" + String.join("、", request.imageFeatures()));
        }

        List<FaultCase> similarCases = matchCases(request).stream().limit(3).toList();
        similarCases.forEach(faultCase -> possibleCauses.add(faultCase.faultName() + "：" + faultCase.cause()));
        if (possibleCauses.isEmpty()) {
            possibleCauses.add("信息不足，建议补充频谱、温度趋势和现场图片后复判");
        }

        String riskLevel = riskLevel(score);
        recommendedActions.addAll(actionsForRisk(riskLevel));
        similarCases.stream().findFirst().ifPresent(faultCase -> recommendedActions.add(faultCase.solution()));

        MaintenanceTask generatedTask = createTask(request, riskLevel, recommendedActions);
        tasks.add(generatedTask);

        return new DiagnosisResult(
                riskLevel,
                Math.min(score, 100),
                evidence,
                possibleCauses,
                recommendedActions,
                similarCases,
                generatedTask
        );
    }

    public ImageAnalysisResult analyzeImage(ImageAnalysisRequest request) {
        String text = ((request.visualDescription() == null ? "" : request.visualDescription()) + " "
                + (request.fileName() == null ? "" : request.fileName())).toLowerCase(Locale.ROOT);

        List<String> detectedFeatures = new ArrayList<>();
        Map<String, List<String>> featureKeywords = Map.of(
                "油污", List.of("油", "漏油", "oil"),
                "锈蚀", List.of("锈", "rust"),
                "焦痕", List.of("焦", "烧", "burn"),
                "裂纹", List.of("裂", "crack"),
                "磨损", List.of("磨", "wear"),
                "漏液", List.of("漏", "渗"),
                "变色", List.of("变色", "发黑", "发黄"),
                "金属屑", List.of("金属屑", "铁屑"),
                "绝缘破损", List.of("绝缘", "破皮")
        );
        featureKeywords.forEach((feature, keywords) -> {
            if (keywords.stream().anyMatch(text::contains)) {
                detectedFeatures.add(feature);
            }
        });

        if (detectedFeatures.isEmpty()) {
            detectedFeatures.add("待人工复核");
        }

        InspectionRequest inspectionRequest = new InspectionRequest(
                null,
                request.deviceType(),
                request.visualDescription(),
                null,
                null,
                null,
                detectedFeatures
        );
        List<FaultCase> similarCases = matchCases(inspectionRequest).stream().limit(3).toList();
        List<String> tips = new ArrayList<>();
        tips.add("拍摄同一部位的远景、近景和铭牌照片，便于定位设备和部件");
        tips.add("补充温度、振动、电流或压力数据，避免只凭外观判断");
        tips.add("将图片特征与历史案例进行相似检索后再生成作业单");
        similarCases.stream().findFirst().ifPresent(faultCase -> tips.add("相似案例建议：" + faultCase.solution()));

        return new ImageAnalysisResult(request.fileName(), detectedFeatures, similarCases, tips);
    }

    public MaintenanceTask updateTaskStatus(String taskId, String status) {
        return updateTaskStatus(taskId, status, "maintainer", "快速状态更新");
    }

    public MaintenanceTask updateTaskStatus(String taskId, String status, String operatorRole, String note) {
        for (int i = 0; i < tasks.size(); i++) {
            MaintenanceTask task = tasks.get(i);
            if (task.id().equals(taskId)) {
                validateTransition(task.status(), status, operatorRole);
                MaintenanceTask updatedTask = new MaintenanceTask(
                        task.id(),
                        task.deviceId(),
                        task.title(),
                        task.priority(),
                        status,
                        task.steps(),
                        task.spareParts(),
                        task.acceptanceCriteria(),
                        task.createdAt()
                );
                tasks.set(i, updatedTask);
                taskFlowEvents.add(new TaskFlowEvent(
                        task.id(),
                        task.status(),
                        status,
                        operatorRole == null || operatorRole.isBlank() ? "maintainer" : operatorRole,
                        note == null || note.isBlank() ? "状态流转" : note,
                        LocalDateTime.now()
                ));
                return updatedTask;
            }
        }
        throw new IllegalArgumentException("Task not found: " + taskId);
    }

    public ReportResult generateReport(DiagnosisResult diagnosisResult) {
        String reportId = "RPT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        List<String> sections = List.of("风险结论", "证据链", "可能原因", "相似案例", "检修作业建议", "验收标准");
        String markdown = """
                # 设备检修诊断报告

                ## 风险结论
                风险等级：%s
                评分：%d

                ## 证据链
                %s

                ## 可能原因
                %s

                ## 检修作业建议
                %s

                ## 自动生成作业单
                %s
                """.formatted(
                diagnosisResult.riskLevel(),
                diagnosisResult.score(),
                toMarkdownList(diagnosisResult.evidence()),
                toMarkdownList(diagnosisResult.possibleCauses()),
                toMarkdownList(diagnosisResult.recommendedActions()),
                diagnosisResult.generatedTask() == null ? "未生成" : diagnosisResult.generatedTask().title()
        );
        ReportResult report = new ReportResult(reportId, "设备检修诊断报告", diagnosisResult.riskLevel(), sections, markdown, LocalDateTime.now());
        reports.add(0, report);
        return report;
    }

    public List<ReportResult> listReports() {
        return reports.stream().limit(20).toList();
    }

    private List<FaultCase> matchCases(InspectionRequest request) {
        String text = ((request.deviceType() == null ? "" : request.deviceType()) + " "
                + (request.description() == null ? "" : request.description()) + " "
                + (request.imageFeatures() == null ? "" : String.join(" ", request.imageFeatures())))
                .toLowerCase(Locale.ROOT);

        return faultCases.stream()
                .sorted(Comparator.comparingInt((FaultCase faultCase) -> matchScore(faultCase, text)).reversed())
                .filter(faultCase -> matchScore(faultCase, text) > 0)
                .toList();
    }

    private int matchScore(FaultCase faultCase, String text) {
        int score = text.contains(faultCase.deviceType().toLowerCase(Locale.ROOT)) ? 3 : 0;
        for (String symptom : faultCase.symptoms()) {
            if (text.contains(symptom.toLowerCase(Locale.ROOT))) {
                score += 2;
            }
        }
        for (String feature : faultCase.imageFeatures()) {
            if (text.contains(feature.toLowerCase(Locale.ROOT))) {
                score += 2;
            }
        }
        return score + faultCase.severity();
    }

    private MaintenanceTask createTask(InspectionRequest request, String riskLevel, List<String> actions) {
        String priority = switch (riskLevel) {
            case "严重" -> "P1";
            case "预警" -> "P2";
            case "关注" -> "P3";
            default -> "P4";
        };
        String deviceId = request.deviceId() == null || request.deviceId().isBlank()
                ? "UNKNOWN"
                : request.deviceId();
        return new MaintenanceTask(
                "TASK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT),
                deviceId,
                riskLevel + "风险检修作业单",
                priority,
                "待派工",
                actions,
                List.of("红外测温仪", "振动采集仪", "万用表", "绝缘手套", "挂牌锁具"),
                List.of("异常参数回落至关注阈值以下", "试运行无异常噪声、焦味、漏油", "检修记录和图片归档"),
                LocalDateTime.now()
        );
    }

    private String riskLevel(int score) {
        if (score >= 75) {
            return "严重";
        }
        if (score >= 50) {
            return "预警";
        }
        if (score >= 25) {
            return "关注";
        }
        return "正常";
    }

    private List<String> actionsForRisk(String riskLevel) {
        return switch (riskLevel) {
            case "严重" -> List.of("立即停机或降载运行", "执行断电、泄压、挂牌上锁", "安排维修人员现场复检", "同步生成故障报告并通知负责人");
            case "预警" -> List.of("计划 24 小时内检修", "补采振动频谱、温度趋势和电流曲线", "核查润滑、紧固和冷却状态", "准备关键备件");
            case "关注" -> List.of("缩短巡检周期", "记录趋势数据", "复核测点和传感器状态", "与历史案例做相似比对");
            default -> List.of("维持例行巡检", "保存本次记录", "按周期复测关键参数");
        };
    }

    private boolean containsAny(String text, List<String> keywords) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return keywords.stream().anyMatch(text::contains);
    }

    private String toMarkdownList(List<String> items) {
        if (items == null || items.isEmpty()) {
            return "- 暂无";
        }
        return String.join("\n", items.stream().map(item -> "- " + item).toList());
    }

    private void validateTransition(String fromStatus, String toStatus, String operatorRole) {
        String role = operatorRole == null || operatorRole.isBlank() ? "maintainer" : operatorRole;
        Map<String, List<String>> allowedRoleStatus = Map.of(
                "inspector", List.of("待派工"),
                "maintainer", List.of("处理中", "待验收"),
                "expert", List.of("专家复核", "已归档"),
                "admin", List.of("待派工", "处理中", "待验收", "专家复核", "已归档")
        );
        if (!allowedRoleStatus.getOrDefault(role, List.of()).contains(toStatus)) {
            throw new IllegalArgumentException("角色 " + role + " 无权流转到 " + toStatus);
        }
        Map<String, List<String>> allowedTransitions = Map.of(
                "待处理", List.of("待派工", "处理中"),
                "待派工", List.of("处理中"),
                "处理中", List.of("待验收"),
                "待验收", List.of("专家复核", "已归档"),
                "专家复核", List.of("处理中", "已归档"),
                "已归档", List.of()
        );
        if (!allowedTransitions.getOrDefault(fromStatus, List.of("待派工")).contains(toStatus)
                && !"admin".equals(role)) {
            throw new IllegalArgumentException("不允许从 " + fromStatus + " 流转到 " + toStatus);
        }
    }
}
