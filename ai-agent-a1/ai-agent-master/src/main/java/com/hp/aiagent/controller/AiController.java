package com.hp.aiagent.controller;

import com.hp.aiagent.agent.DashScopeCompatibleToolAgent;
import com.hp.aiagent.agent.YuManus;
import com.hp.aiagent.app.DashScopeCompatibleChatClient;
import com.hp.aiagent.app.MyApp;
import com.hp.aiagent.Constant.FileConstant;
import jakarta.annotation.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private MyApp myApp;

    @Resource
    private DashScopeCompatibleChatClient compatibleChatClient;

    @Resource
    private DashScopeCompatibleToolAgent compatibleToolAgent;

    @Resource
    private ToolCallback[] allTools;

    @Resource
    private ChatModel dashscopeChatModel;

    @GetMapping("/app/chat/sync")
    public String doChatWithLoveAppSync(String message, String chatId) {

        return myApp.doChat(message, chatId);
    }


    @GetMapping(value = "/app/chat/sse")
    public Flux<ServerSentEvent<String>> doChatWithLoveAppSSE(String message, String chatId) {
        return myApp.doChatByStream(message, chatId)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .data(chunk)
                        .build())
                .onErrorResume(ex -> Flux.just(ServerSentEvent.<String>builder()
                        .data("AI 调用失败：" + ex.getMessage())
                        .build()))
                .concatWithValues(ServerSentEvent.<String>builder()
                        .data("[DONE]")
                        .build());
    }

    @GetMapping("/app/chat/sse/emitter")
    public SseEmitter doChatWithLoveAppSseEmitter(String message, String chatId) {

        SseEmitter emitter = new SseEmitter(180000L);

        myApp.doChatByStream(message, chatId)
                .subscribe(

                        chunk -> {
                            try {
                                emitter.send(chunk);
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        },

                        emitter::completeWithError,

                        emitter::complete
                );

        return emitter;
    }


    @GetMapping("/manus/chat")
    public Flux<ServerSentEvent<String>> doChatWithManus(String message) {
        return compatibleToolAgent.run(message)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .data(chunk)
                        .build())
                .onErrorResume(ex -> Flux.just(ServerSentEvent.<String>builder()
                        .data("工具智能体调用失败：" + ex.getMessage())
                        .build()))
                .concatWithValues(ServerSentEvent.<String>builder()
                        .data("[DONE]")
                        .build());
    }

    @GetMapping("/manus/files/download")
    public ResponseEntity<UrlResource> downloadManusFile(String type, String name, Boolean inline) throws IOException {
        Path baseDir = resolveDownloadBaseDir(type);
        Path target = baseDir.resolve(name == null ? "" : name.replace('\\', '/')).normalize();
        if (!target.startsWith(baseDir) || !Files.isRegularFile(target)) {
            return ResponseEntity.notFound().build();
        }

        UrlResource resource = toUrlResource(target);
        String contentType = Files.probeContentType(target);
        if (contentType == null || contentType.isBlank()) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, (Boolean.TRUE.equals(inline)
                                ? ContentDisposition.inline()
                                : ContentDisposition.attachment())
                        .filename(target.getFileName().toString())
                        .build()
                        .toString())
                .contentLength(Files.size(target))
                .body(resource);
    }

    @GetMapping("/manus/files")
    public List<ManusFileItem> listManusFiles() throws IOException {
        List<ManusFileItem> files = new ArrayList<>();
        collectManusFiles(files, "file");
        collectManusFiles(files, "pdf");
        collectManusFiles(files, "download");
        files.sort(Comparator.comparing(ManusFileItem::modifiedAt).reversed());
        return files;
    }

    private Path resolveDownloadBaseDir(String type) {
        Path root = Paths.get(FileConstant.FILE_SAVE_DIR).toAbsolutePath().normalize();
        return switch (type == null ? "" : type) {
            case "file" -> root.resolve("file").normalize();
            case "pdf" -> root.resolve("pdf").normalize();
            case "download" -> root.resolve("download").normalize();
            default -> throw new IllegalArgumentException("Unsupported file type: " + type);
        };
    }

    private void collectManusFiles(List<ManusFileItem> result, String type) throws IOException {
        Path baseDir = resolveDownloadBaseDir(type);
        if (!Files.exists(baseDir)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(baseDir, 4)) {
            paths.filter(Files::isRegularFile)
                    .forEach(path -> result.add(toManusFileItem(type, baseDir, path)));
        }
    }

    private ManusFileItem toManusFileItem(String type, Path baseDir, Path path) {
        try {
            String name = baseDir.relativize(path).toString().replace('\\', '/');
            String encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20");
            String downloadUrl = "/ai/manus/files/download?type=" + type + "&name=" + encodedName;
            return new ManusFileItem(
                    type,
                    name,
                    Files.size(path),
                    Files.getLastModifiedTime(path).toInstant(),
                    downloadUrl
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read file metadata: " + path, e);
        }
    }

    private UrlResource toUrlResource(Path target) throws MalformedURLException {
        return new UrlResource(target.toUri());
    }

    public record ManusFileItem(String type, String name, long size, Instant modifiedAt, String downloadUrl) {
    }

}
