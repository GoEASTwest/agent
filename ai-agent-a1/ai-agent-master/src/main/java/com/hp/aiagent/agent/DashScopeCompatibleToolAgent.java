package com.hp.aiagent.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hp.aiagent.Constant.FileConstant;
import com.hp.aiagent.tools.PdfFontProvider;
import com.hp.aiagent.tools.WebSearchTool;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Slf4j
public class DashScopeCompatibleToolAgent {

    private static final int MAX_TOOL_ROUNDS = 6;
    private static final int MAX_MODEL_TOOL_RESULT_CHARS = 12000;
    private static final int MAX_UI_TOOL_RESULT_CHARS = 1200;
    private static final int MAX_SCRAPED_TEXT_CHARS = 12000;
    private static final long MAX_DOWNLOAD_BYTES = 30L * 1024L * 1024L;
    private static final String AGENT_EVENT_PREFIX = "__AGENT_EVENT__";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final String chatCompletionsUrl;
    private final WebSearchTool webSearchTool;
    private final Path fileDir;
    private final Path pdfDir;
    private final Path downloadDir;

    public DashScopeCompatibleToolAgent(
            ObjectMapper objectMapper,
            @Value("${spring.ai.dashscope.api-key:}") String apiKey,
            @Value("${spring.ai.dashscope.chat.options.model:qwen-turbo}") String model,
            @Value("${dashscope.compatible.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}") String baseUrl,
            @Value("${search-api.api-key:}") String searchApiKey) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.chatCompletionsUrl = stripTrailingSlash(baseUrl) + "/chat/completions";
        this.webSearchTool = new WebSearchTool(searchApiKey);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        Path root = Paths.get(FileConstant.FILE_SAVE_DIR).toAbsolutePath().normalize();
        this.fileDir = root.resolve("file").normalize();
        this.pdfDir = root.resolve("pdf").normalize();
        this.downloadDir = root.resolve("download").normalize();
    }

    public Flux<String> run(String userMessage) {
        return Flux.<String>create(sink -> {
            try {
                runBlocking(userMessage, sink);
                sink.complete();
            } catch (Exception e) {
                log.error("DashScope compatible tool agent failed", e);
                if (!sink.isCancelled()) {
                    sink.next("工具智能体调用失败：" + e.getMessage());
                    sink.complete();
                }
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private void runBlocking(String userMessage, reactor.core.publisher.FluxSink<String> sink)
            throws IOException, InterruptedException {
        if (userMessage == null || userMessage.isBlank()) {
            emit(sink, "请输入要处理的工具任务。");
            return;
        }

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(message("system", systemPrompt()));
        messages.add(message("user", userMessage));
        emitStep(sink, "analysis", "分析任务", "running", "正在判断是否需要调用文件、网页或 PDF 工具。");

        for (int round = 1; round <= MAX_TOOL_ROUNDS; round++) {
            JsonNode response = chatCompletion(messages, true);
            JsonNode assistantMessage = response.path("choices").path(0).path("message");
            JsonNode toolCalls = assistantMessage.path("tool_calls");
            String content = assistantMessage.path("content").asText("");

            if (!toolCalls.isArray() || toolCalls.isEmpty()) {
                emitStep(sink, "analysis", "分析任务", "complete", "无需继续调用工具。");
                emitStep(sink, "final-answer", "生成最终答复", "complete", "已生成面向现场检修人员的答复。");
                emit(sink, content.isBlank() ? "任务已完成，但模型没有返回文本。" : content);
                return;
            }

            emitStep(sink, "analysis", "分析任务", "complete", "已识别到 " + toolCalls.size() + " 个工具调用。");
            messages.add(assistantToolMessage(content, toolCalls));

            for (JsonNode toolCall : toolCalls) {
                String toolCallId = toolCall.path("id").asText("");
                JsonNode function = toolCall.path("function");
                String toolName = function.path("name").asText("");
                String arguments = function.path("arguments").asText("{}");
                JsonNode args = parseArguments(arguments);
                String stepId = "tool-" + round + "-" + normalizeStepId(toolName) + "-" + Math.abs(toolCallId.hashCode());
                String friendlyName = friendlyToolName(toolName);

                emitStep(sink, stepId, friendlyName, "running", summarizeToolArgs(toolName, args));
                String result = executeTool(toolName, args);
                boolean failed = result.startsWith("Tool execution failed")
                        || result.startsWith("Unsupported tool")
                        || result.startsWith("Download failed")
                        || result.startsWith("Download refused");
                emitStep(sink, stepId, friendlyName, failed ? "error" : "complete",
                        truncate(result, MAX_UI_TOOL_RESULT_CHARS));

                messages.add(toolMessage(toolCallId, toolName, truncate(result, MAX_MODEL_TOOL_RESULT_CHARS)));
                if (sink.isCancelled()) {
                    return;
                }
            }
        }

        emitStep(sink, "final-answer", "生成最终答复", "running", "正在汇总工具结果。");
        messages.add(message("user", "请基于以上工具执行结果给出最终答复，不要再调用工具。"));
        JsonNode finalResponse = chatCompletion(messages, false);
        String finalContent = finalResponse.path("choices").path(0).path("message").path("content").asText("");
        emitStep(sink, "final-answer", "生成最终答复", "complete", "已完成工具结果汇总。");
        emit(sink, finalContent.isBlank() ? "已完成工具调用，请查看上方工具结果。" : finalContent);
    }

    private JsonNode chatCompletion(List<Map<String, Object>> messages, boolean includeTools)
            throws IOException, InterruptedException {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Missing DASHSCOPE_API_KEY.");
        }

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("stream", false);
        body.put("temperature", 0.2);
        body.set("messages", objectMapper.valueToTree(messages));
        if (includeTools) {
            body.set("tools", toolSchemas());
            body.put("tool_choice", "auto");
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(chatCompletionsUrl))
                .timeout(Duration.ofMinutes(3))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() >= 400) {
            throw dashScopeError(response.statusCode(), response.body());
        }
        return objectMapper.readTree(response.body());
    }

    private ArrayNode toolSchemas() {
        ArrayNode tools = objectMapper.createArrayNode();
        tools.add(tool("read_text_file", "Read a UTF-8 text file saved by the agent.",
                parameters(properties(prop("fileName", "string", "Relative file name under the agent file directory.")),
                        "fileName")));
        tools.add(tool("write_text_file", "Write UTF-8 text content to a file for the user.",
                parameters(properties(
                                prop("fileName", "string", "Relative file name, for example maintenance-checklist.md."),
                                prop("content", "string", "Full text content to write.")),
                        "fileName", "content")));
        tools.add(tool("list_saved_files", "List files saved by the agent.",
                parameters(properties())));
        tools.add(tool("scrape_web_page", "Fetch a public web page and return readable text content.",
                parameters(properties(prop("url", "string", "Public http or https URL to read.")), "url")));
        tools.add(tool("download_resource", "Download a public http or https resource to the agent download directory.",
                parameters(properties(
                                prop("url", "string", "Public http or https URL to download."),
                                prop("fileName", "string", "Relative file name to save as.")),
                        "url", "fileName")));
        tools.add(tool("generate_pdf", "Generate a PDF file from text content.",
                parameters(properties(
                                prop("fileName", "string", "Relative PDF file name, for example report.pdf."),
                                prop("content", "string", "Text content to include in the PDF.")),
                        "fileName", "content")));
        tools.add(tool("search_web", "Search the web when SEARCH_API_KEY is configured.",
                parameters(properties(prop("query", "string", "Search keywords.")), "query")));
        return tools;
    }

    private ObjectNode tool(String name, String description, ObjectNode parameters) {
        ObjectNode tool = objectMapper.createObjectNode();
        tool.put("type", "function");
        ObjectNode function = tool.putObject("function");
        function.put("name", name);
        function.put("description", description);
        function.set("parameters", parameters);
        return tool;
    }

    private ObjectNode parameters(ObjectNode properties, String... required) {
        ObjectNode parameters = objectMapper.createObjectNode();
        parameters.put("type", "object");
        parameters.set("properties", properties);
        ArrayNode requiredNode = parameters.putArray("required");
        for (String item : required) {
            requiredNode.add(item);
        }
        return parameters;
    }

    private ObjectNode properties(ObjectNode... props) {
        ObjectNode properties = objectMapper.createObjectNode();
        for (ObjectNode prop : props) {
            properties.set(prop.path("name").asText(), prop.path("schema"));
        }
        return properties;
    }

    private ObjectNode prop(String name, String type, String description) {
        ObjectNode prop = objectMapper.createObjectNode();
        ObjectNode schema = prop.putObject("schema");
        schema.put("type", type);
        schema.put("description", description);
        prop.put("name", name);
        return prop;
    }

    private String executeTool(String toolName, JsonNode args) {
        try {
            return switch (toolName) {
                case "read_text_file" -> readTextFile(requiredText(args, "fileName"));
                case "write_text_file" -> writeTextFile(requiredText(args, "fileName"), requiredText(args, "content"));
                case "list_saved_files" -> listSavedFiles();
                case "scrape_web_page" -> scrapeWebPage(requiredText(args, "url"));
                case "download_resource" -> downloadResource(requiredText(args, "url"), requiredText(args, "fileName"));
                case "generate_pdf" -> generatePdf(requiredText(args, "fileName"), requiredText(args, "content"));
                case "search_web" -> webSearchTool.searchWeb(requiredText(args, "query"));
                default -> "Unsupported tool: " + toolName;
            };
        } catch (Exception e) {
            return "Tool execution failed: " + e.getMessage();
        }
    }

    private String readTextFile(String fileName) throws IOException {
        Path target = resolveUnder(fileDir, fileName);
        if (!Files.exists(target)) {
            return "File not found: " + target;
        }
        return Files.readString(target, StandardCharsets.UTF_8);
    }

    private String writeTextFile(String fileName, String content) throws IOException {
        Path target = resolveUnder(fileDir, fileName);
        Files.createDirectories(target.getParent());
        Files.writeString(target, content == null ? "" : content, StandardCharsets.UTF_8);
        return "File written successfully to: " + target + downloadHint("file", fileDir, target);
    }

    private String listSavedFiles() throws IOException {
        if (!Files.exists(fileDir)) {
            return "No saved text files yet: " + fileDir;
        }
        try (Stream<Path> paths = Files.walk(fileDir, 4)) {
            List<String> files = paths
                    .filter(Files::isRegularFile)
                    .map(path -> fileDir.relativize(path).toString())
                    .sorted()
                    .collect(Collectors.toList());
            if (files.isEmpty()) {
                return "No saved text files yet: " + fileDir;
            }
            return "Saved text files under " + fileDir + ":\n" + String.join("\n", files);
        }
    }

    private String scrapeWebPage(String url) throws IOException {
        URI uri = requireHttpUri(url);
        org.jsoup.nodes.Document document = Jsoup.connect(uri.toString())
                .userAgent("Mozilla/5.0 ai-agent maintenance assistant")
                .timeout(15000)
                .get();
        String title = document.title() == null ? "" : document.title();
        String body = document.body() == null ? "" : document.body().text();
        return truncate("Title: " + title + "\n\n" + body, MAX_SCRAPED_TEXT_CHARS);
    }

    private String downloadResource(String url, String fileName) throws IOException, InterruptedException {
        URI uri = requireHttpUri(url);
        Path target = resolveUnder(downloadDir, fileName);
        Files.createDirectories(target.getParent());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofMinutes(2))
                .GET()
                .build();
        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() >= 400) {
            return "Download failed with HTTP status: " + response.statusCode();
        }
        long contentLength = response.headers().firstValueAsLong("content-length").orElse(-1L);
        if (contentLength > MAX_DOWNLOAD_BYTES) {
            response.body().close();
            return "Download refused because the file is larger than 30 MB.";
        }
        try (InputStream input = response.body(); OutputStream output = Files.newOutputStream(target)) {
            copyWithLimit(input, output, MAX_DOWNLOAD_BYTES);
        }
        return "Resource downloaded successfully to: " + target + downloadHint("download", downloadDir, target);
    }

    private String generatePdf(String fileName, String content) throws IOException {
        Path target = resolveUnder(pdfDir, withExtension(fileName, ".pdf"));
        Files.createDirectories(target.getParent());
        try (PdfWriter writer = new PdfWriter(target.toFile());
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {
            PdfFont font = PdfFontProvider.createChineseFont();
            document.setFont(font);
            for (String line : (content == null ? "" : content).split("\\R", -1)) {
                document.add(new Paragraph(line.isBlank() ? " " : line));
            }
        }
        return "PDF generated successfully to: " + target + downloadHint("pdf", pdfDir, target);
    }

    private String downloadHint(String type, Path baseDir, Path target) {
        String relativeName = baseDir.relativize(target).toString().replace('\\', '/');
        String encodedName = URLEncoder.encode(relativeName, StandardCharsets.UTF_8).replace("+", "%20");
        return "\nDownload URL: /ai/manus/files/download?type=" + type + "&name=" + encodedName;
    }

    private void copyWithLimit(InputStream input, OutputStream output, long maxBytes) throws IOException {
        byte[] buffer = new byte[8192];
        long total = 0L;
        int read;
        while ((read = input.read(buffer)) != -1) {
            total += read;
            if (total > maxBytes) {
                throw new IOException("Downloaded file exceeds 30 MB limit.");
            }
            output.write(buffer, 0, read);
        }
    }

    private URI requireHttpUri(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL is required.");
        }
        URI uri = URI.create(url.trim());
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("Only http and https URLs are allowed.");
        }
        return uri;
    }

    private Path resolveUnder(Path baseDir, String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("fileName is required.");
        }
        Path target = baseDir.resolve(fileName.replace('\\', '/')).normalize();
        if (!target.startsWith(baseDir)) {
            throw new IllegalArgumentException("File path must stay under: " + baseDir);
        }
        return target;
    }

    private String requiredText(JsonNode args, String fieldName) {
        String value = args.path(fieldName).asText("");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value;
    }

    private Map<String, Object> message(String role, String content) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    private Map<String, Object> assistantToolMessage(String content, JsonNode toolCalls) {
        Map<String, Object> message = message("assistant", content == null ? "" : content);
        message.put("tool_calls", objectMapper.convertValue(toolCalls, Object.class));
        return message;
    }

    private Map<String, Object> toolMessage(String toolCallId, String toolName, String content) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", "tool");
        message.put("tool_call_id", toolCallId);
        message.put("name", toolName);
        message.put("content", content);
        return message;
    }

    private JsonNode parseArguments(String arguments) {
        try {
            return objectMapper.readTree(arguments == null || arguments.isBlank() ? "{}" : arguments);
        } catch (IOException e) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("raw", arguments);
            return node;
        }
    }

    private RuntimeException dashScopeError(int statusCode, String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode error = root.path("error");
            String code = error.path("code").asText(root.path("code").asText(""));
            String message = error.path("message").asText(root.path("message").asText(body));
            return new IllegalStateException("DashScope " + statusCode + " " + code + ": " + message);
        } catch (Exception e) {
            return new IllegalStateException("DashScope " + statusCode + ": " + body);
        }
    }

    private String systemPrompt() {
        return """
                你是检修资料处理工具智能体，负责帮助现场人员整理检修规程、生成 checklist、报告草稿、资料摘要和可归档文件。
                你可以调用工具读写文本文件、列出已保存文件、抓取公开网页、下载公开资源、生成 PDF、搜索网页。
                只有当任务确实需要保存、读取、抓取、下载、生成 PDF 或搜索时才调用工具；普通问答直接回答。
                生成文件后，在最终回答中明确告诉用户文件保存路径和下一步建议。
                当前没有开放终端命令执行工具，不要声称已经执行本地命令。
                回答要直接、结构清晰、面向现场检修人员可执行。
                """;
    }

    private String friendlyToolName(String toolName) {
        return switch (toolName) {
            case "read_text_file" -> "读取文本文件";
            case "write_text_file" -> "写入文本文件";
            case "list_saved_files" -> "列出已保存文件";
            case "scrape_web_page" -> "抓取网页内容";
            case "download_resource" -> "下载资源";
            case "generate_pdf" -> "生成 PDF";
            case "search_web" -> "网页搜索";
            default -> toolName;
        };
    }

    private String summarizeToolArgs(String toolName, JsonNode args) {
        return switch (toolName) {
            case "read_text_file", "write_text_file" -> "文件：" + args.path("fileName").asText("-");
            case "download_resource" -> "保存为：" + args.path("fileName").asText("-") + "，来源：" + args.path("url").asText("-");
            case "generate_pdf" -> "PDF：" + args.path("fileName").asText("-");
            case "scrape_web_page" -> "网页：" + args.path("url").asText("-");
            case "search_web" -> "关键词：" + args.path("query").asText("-");
            default -> "准备执行工具。";
        };
    }

    private void emitStep(reactor.core.publisher.FluxSink<String> sink, String id, String title, String status, String detail) {
        if (sink.isCancelled()) {
            return;
        }
        try {
            ObjectNode event = objectMapper.createObjectNode();
            event.put("kind", "step");
            event.put("id", id);
            event.put("title", title);
            event.put("status", status);
            event.put("detail", detail == null ? "" : detail);
            sink.next(AGENT_EVENT_PREFIX + objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            log.debug("Failed to emit agent step event", e);
        }
    }

    private void emit(reactor.core.publisher.FluxSink<String> sink, String value) {
        if (!sink.isCancelled() && value != null && !value.isBlank()) {
            sink.next(value);
        }
    }

    private String truncate(String value, int maxChars) {
        if (value == null || value.length() <= maxChars) {
            return value;
        }
        return value.substring(0, maxChars) + "\n...内容已截断...";
    }

    private String withExtension(String fileName, String extension) {
        return fileName.toLowerCase(Locale.ROOT).endsWith(extension) ? fileName : fileName + extension;
    }

    private String normalizeStepId(String value) {
        return value == null ? "tool" : value.replaceAll("[^a-zA-Z0-9_-]", "-");
    }

    private String stripTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "https://dashscope.aliyuncs.com/compatible-mode/v1";
        }
        return value.replaceAll("/+$", "");
    }
}
