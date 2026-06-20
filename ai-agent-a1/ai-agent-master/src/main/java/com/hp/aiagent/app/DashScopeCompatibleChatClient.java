package com.hp.aiagent.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Slf4j
public class DashScopeCompatibleChatClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final String chatCompletionsUrl;

    public DashScopeCompatibleChatClient(
            ObjectMapper objectMapper,
            @Value("${spring.ai.dashscope.api-key:}") String apiKey,
            @Value("${spring.ai.dashscope.chat.options.model:qwen-turbo}") String model,
            @Value("${dashscope.compatible.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}") String baseUrl) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.chatCompletionsUrl = stripTrailingSlash(baseUrl) + "/chat/completions";
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    public String chat(String systemPrompt, String message) {
        try {
            HttpResponse<String> response = httpClient.send(
                    request(false, systemPrompt, message),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            if (response.statusCode() >= 400) {
                throw dashScopeError(response.statusCode(), response.body());
            }
            JsonNode root = objectMapper.readTree(response.body());
            String content = root.path("choices").path(0).path("message").path("content").asText("");
            if (content.isBlank()) {
                throw new IllegalStateException("DashScope returned an empty answer.");
            }
            return content;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to call DashScope compatible chat API.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("DashScope compatible chat API call was interrupted.", e);
        }
    }

    public Flux<String> stream(String systemPrompt, String message) {
        return Flux.<String>create(sink -> {
            try {
                HttpResponse<Stream<String>> response = httpClient.send(
                        request(true, systemPrompt, message),
                        HttpResponse.BodyHandlers.ofLines()
                );
                try (Stream<String> lines = response.body()) {
                    if (response.statusCode() >= 400) {
                        String body = lines.collect(Collectors.joining("\n"));
                        sink.error(dashScopeError(response.statusCode(), body));
                        return;
                    }
                    lines.forEach(line -> {
                        if (sink.isCancelled()) {
                            return;
                        }
                        parseStreamContent(line).ifPresent(sink::next);
                    });
                }
                sink.complete();
            } catch (IOException e) {
                sink.error(new IllegalStateException("Failed to call DashScope compatible stream API.", e));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                sink.error(new IllegalStateException("DashScope compatible stream API call was interrupted.", e));
            } catch (RuntimeException e) {
                sink.error(e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private HttpRequest request(boolean stream, String systemPrompt, String message) throws IOException {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Missing DASHSCOPE_API_KEY.");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", message)
        ));
        body.put("stream", stream);
        body.put("temperature", 0.8);

        return HttpRequest.newBuilder()
                .uri(URI.create(chatCompletionsUrl))
                .timeout(Duration.ofMinutes(3))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                .build();
    }

    private java.util.Optional<String> parseStreamContent(String line) {
        if (line == null || line.isBlank() || !line.startsWith("data:")) {
            return java.util.Optional.empty();
        }
        String payload = line.substring("data:".length()).trim();
        if ("[DONE]".equals(payload)) {
            return java.util.Optional.empty();
        }
        try {
            JsonNode delta = objectMapper.readTree(payload).path("choices").path(0).path("delta");
            JsonNode content = delta.get("content");
            if (content == null || content.isNull()) {
                return java.util.Optional.empty();
            }
            String value = content.asText("");
            return value.isBlank() ? java.util.Optional.empty() : java.util.Optional.of(value);
        } catch (IOException e) {
            log.debug("Ignoring malformed DashScope stream line: {}", line);
            return java.util.Optional.empty();
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

    private String stripTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "https://dashscope.aliyuncs.com/compatible-mode/v1";
        }
        return value.replaceAll("/+$", "");
    }
}
