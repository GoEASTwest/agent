package com.hp.aiagent.maintenance.service;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.hp.aiagent.maintenance.model.FaultCase;
import com.hp.aiagent.maintenance.model.ImageAnalysisRequest;
import com.hp.aiagent.maintenance.model.ImageAnalysisResult;
import com.hp.aiagent.maintenance.model.VisionAnalysisRequest;
import com.hp.aiagent.maintenance.model.VisionAnalysisResult;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class VisionAnalysisService {

    @Resource
    private ChatModel dashscopeChatModel;

    @Resource
    private MaintenanceService maintenanceService;

    public VisionAnalysisResult analyze(VisionAnalysisRequest request) {
        ImageAnalysisResult fallback = maintenanceService.analyzeImage(new ImageAnalysisRequest(
                request.deviceType(),
                request.imageUrl() == null || request.imageUrl().isBlank() ? "text-only-inspection" : request.imageUrl(),
                request.visualDescription()
        ));
        List<String> actions = buildActions(fallback.similarCases());
        String riskLevel = estimateRiskLevel(request.visualDescription(), fallback.detectedFeatures());
        if (request.imageUrl() == null || request.imageUrl().isBlank()) {
            return new VisionAnalysisResult(
                    "local-rule",
                    "feature-keyword-matcher",
                    fallback.detectedFeatures(),
                    riskLevel,
                    "未提供图片 URL，已根据现场图片描述抽取可见缺陷并匹配案例。",
                    fallback.similarCases(),
                    actions
            );
        }

        try {
            String promptText = """
                    你是工业设备检修视觉诊断助手。请根据图片和现场描述输出：
                    1. 可见缺陷特征；
                    2. 风险判断；
                    3. 可能故障部位；
                    4. 检修动作建议。
                    设备类型：%s
                    现场描述：%s
                    补充问题：%s
                    回答要简洁，避免编造图片中不可见的参数。
                    """.formatted(nullToEmpty(request.deviceType()), nullToEmpty(request.visualDescription()), nullToEmpty(request.question()));
            UserMessage userMessage = new UserMessage(
                    promptText,
                    List.of(new Media(MimeTypeUtils.IMAGE_JPEG, new URL(request.imageUrl())))
            );
            String content = dashscopeChatModel.call(new Prompt(
                    List.of(userMessage),
                    DashScopeChatOptions.builder()
                            .withModel("qwen-vl-plus")
                            .withMultiModel(true)
                            .withVlHighResolutionImages(true)
                            .build()
            )).getResult().getOutput().getText();

            return new VisionAnalysisResult(
                    "dashscope",
                    "qwen-vl-plus",
                    fallback.detectedFeatures(),
                    riskLevel,
                    content,
                    fallback.similarCases(),
                    actions
            );
        } catch (Exception ex) {
            return new VisionAnalysisResult(
                    "local-rule",
                    "feature-keyword-matcher",
                    fallback.detectedFeatures(),
                    riskLevel,
                    "Qwen-VL 调用未完成，已切换到本地特征分析。原因：" + ex.getMessage(),
                    fallback.similarCases(),
                    actions
            );
        }
    }

    public VisionAnalysisResult analyzeUploadedImage(String deviceType,
                                                     String visualDescription,
                                                     String question,
                                                     String originalFileName,
                                                     String contentType,
                                                     byte[] content) {
        ImageAnalysisResult fallback = maintenanceService.analyzeUploadedImage(deviceType, visualDescription, originalFileName, content);
        List<String> actions = buildActions(fallback.similarCases());
        actions.add("上传图片已保存到文件中心 upload 目录，可用于复盘和答辩演示");
        String riskLevel = estimateRiskLevel(visualDescription, fallback.detectedFeatures());
        if (content == null || content.length == 0) {
            return new VisionAnalysisResult(
                    "local-rule",
                    "feature-keyword-matcher",
                    fallback.detectedFeatures(),
                    riskLevel,
                    "上传图片为空，已根据文件名和现场描述进行本地特征分析。",
                    fallback.similarCases(),
                    actions
            );
        }

        try {
            String safeFileName = originalFileName == null || originalFileName.isBlank()
                    ? "uploaded-maintenance-image"
                    : originalFileName;
            String promptText = """
                    你是工业设备检修视觉诊断助手。请直接分析用户上传的设备故障图片，并结合现场描述输出：
                    1. 图片中可见缺陷特征；
                    2. 风险等级判断；
                    3. 可能故障部位和原因；
                    4. 现场检修建议；
                    5. 是否建议生成检修作业单。
                    设备类型：%s
                    文件名：%s
                    现场描述：%s
                    补充问题：%s
                    回答要面向检修现场，避免编造图片中不可见的参数。
                    """.formatted(nullToEmpty(deviceType), safeFileName, nullToEmpty(visualDescription), nullToEmpty(question));
            Media uploadedImage = Media.builder()
                    .mimeType(resolveImageMimeType(contentType, safeFileName))
                    .data(new ByteArrayResource(content, safeFileName))
                    .name(safeFileName)
                    .build();
            String contentText = dashscopeChatModel.call(new Prompt(
                    List.of(new UserMessage(promptText, List.of(uploadedImage))),
                    DashScopeChatOptions.builder()
                            .withModel("qwen-vl-plus")
                            .withMultiModel(true)
                            .withVlHighResolutionImages(true)
                            .build()
            )).getResult().getOutput().getText();

            return new VisionAnalysisResult(
                    "dashscope-upload",
                    "qwen-vl-plus",
                    fallback.detectedFeatures(),
                    estimateRiskLevel(visualDescription + " " + contentText, fallback.detectedFeatures()),
                    contentText,
                    fallback.similarCases(),
                    actions
            );
        } catch (Exception ex) {
            return new VisionAnalysisResult(
                    "local-rule",
                    "feature-keyword-matcher",
                    fallback.detectedFeatures(),
                    riskLevel,
                    "Qwen-VL 上传图片分析未完成，已切换到本地特征分析。原因：" + ex.getMessage(),
                    fallback.similarCases(),
                    actions
            );
        }
    }

    private List<String> buildActions(List<FaultCase> similarCases) {
        List<String> actions = new ArrayList<>();
        actions.add("补拍设备铭牌、故障部位近景和周边环境远景");
        actions.add("将图片特征同步写入诊断输入，结合温度、振动、电流复判");
        actions.add("检修前执行断电、泄压、挂牌上锁和个人防护");
        similarCases.stream().findFirst().ifPresent(faultCase -> actions.add("相似案例建议：" + faultCase.solution()));
        return actions;
    }

    private String estimateRiskLevel(String description, List<String> features) {
        String text = ((description == null ? "" : description) + " " + String.join(" ", features)).toLowerCase();
        if (containsAny(text, List.of("焦", "烧", "裂纹", "绝缘破损", "大量漏油", "严重", "停机"))) {
            return "严重";
        }
        if (containsAny(text, List.of("漏油", "漏液", "变色", "金属屑", "磨损", "预警"))) {
            return "预警";
        }
        if (features == null || features.isEmpty() || features.contains("待人工复核")) {
            return "关注";
        }
        return "关注";
    }

    private boolean containsAny(String text, List<String> keywords) {
        return keywords.stream().anyMatch(text::contains);
    }

    private MimeType resolveImageMimeType(String contentType, String fileName) {
        if (contentType != null && !contentType.isBlank() && contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            try {
                return MimeTypeUtils.parseMimeType(contentType);
            } catch (Exception ignored) {
                // Continue to filename inference below.
            }
        }
        String lowerFileName = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        if (lowerFileName.endsWith(".png")) {
            return MimeTypeUtils.parseMimeType("image/png");
        }
        if (lowerFileName.endsWith(".webp")) {
            return MimeTypeUtils.parseMimeType("image/webp");
        }
        if (lowerFileName.endsWith(".gif")) {
            return MimeTypeUtils.parseMimeType("image/gif");
        }
        return MimeTypeUtils.IMAGE_JPEG;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
