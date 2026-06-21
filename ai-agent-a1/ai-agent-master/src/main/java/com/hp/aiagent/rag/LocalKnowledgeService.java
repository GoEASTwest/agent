package com.hp.aiagent.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@Slf4j
public class LocalKnowledgeService {

    private static final int MAX_SNIPPET_CHARS = 700;
    private static final int MAX_CONTEXT_CHARS = 2600;
    private static final Pattern SPLIT_PATTERN = Pattern.compile("[\\s,，。；;：:、/\\\\()（）\\[\\]【】\\-]+");
    private static final List<String> DOMAIN_KEYWORDS = List.of(
            "风机", "泵", "电机", "轴承", "齿轮箱", "振动", "温度", "电流", "压力", "噪声",
            "润滑", "绝缘", "频谱", "过热", "磨损", "裂纹", "漏油", "锈蚀", "焦痕", "变色",
            "汽蚀", "不对中", "断齿", "点蚀", "检修", "巡检", "作业单", "验收", "归档"
    );

    private final ResourcePatternResolver resourcePatternResolver;
    private volatile List<KnowledgeDocument> documents;

    public LocalKnowledgeService(ResourcePatternResolver resourcePatternResolver) {
        this.resourcePatternResolver = resourcePatternResolver;
    }

    public KnowledgeContext retrieve(String query) {
        return retrieve(query, List.of());
    }

    public KnowledgeContext retrieve(String query, List<KnowledgeSource> additionalSources) {
        KnowledgeSearchResult searchResult = search(query, additionalSources);
        if (!searchResult.hasMatches()) {
            return KnowledgeContext.empty();
        }
        StringBuilder context = new StringBuilder();
        List<String> citations = new ArrayList<>();
        for (KnowledgeMatch match : searchResult.matches()) {
            if (context.length() >= MAX_CONTEXT_CHARS) {
                break;
            }
            String citation = "%s：%s - %s".formatted(match.sourceType(), match.sourceName(), match.title());
            citations.add(citation);
            context.append("来源：").append(citation).append("\n");
            context.append(match.snippet()).append("\n\n");
        }
        return new KnowledgeContext(context.toString().trim(), citations.stream().distinct().toList());
    }

    public KnowledgeSearchResult search(String query) {
        return search(query, List.of());
    }

    public KnowledgeSearchResult search(String query, List<KnowledgeSource> additionalSources) {
        List<String> keywords = extractKeywords(query);
        if (keywords.isEmpty()) {
            return new KnowledgeSearchResult(query == null ? "" : query.trim(), List.of(), List.of());
        }
        List<KnowledgeDocument> allDocuments = new ArrayList<>(loadDocuments());
        if (additionalSources != null) {
            additionalSources.stream()
                    .filter(source -> source != null && source.content() != null && !source.content().isBlank())
                    .map(source -> new KnowledgeDocument(
                            blankToDefault(source.sourceType(), "业务知识"),
                            blankToDefault(source.sourceName(), "未命名来源"),
                            blankToDefault(source.title(), source.sourceName()),
                            source.content()
                    ))
                    .forEach(allDocuments::add);
        }
        List<KnowledgeMatch> matches = allDocuments.stream()
                .map(document -> matchDocument(document, keywords))
                .filter(match -> match.score() > 0)
                .sorted(Comparator.comparingInt(KnowledgeMatch::score).reversed())
                .limit(6)
                .toList();
        return new KnowledgeSearchResult(query == null ? "" : query.trim(), keywords, matches);
    }

    private KnowledgeMatch matchDocument(KnowledgeDocument document, List<String> keywords) {
        String searchableText = (document.title() + "\n" + document.content()).toLowerCase(Locale.ROOT);
        int score = 0;
        for (String keyword : keywords) {
            String lowerKeyword = keyword.toLowerCase(Locale.ROOT);
            if (document.title().toLowerCase(Locale.ROOT).contains(lowerKeyword)) {
                score += 3;
            }
            if (searchableText.contains(lowerKeyword)) {
                score++;
            }
        }
        if (score == 0) {
            return new KnowledgeMatch(document.sourceType(), document.sourceName(), document.title(), 0, "");
        }
        return new KnowledgeMatch(
                document.sourceType(),
                document.sourceName(),
                document.title(),
                score,
                bestSnippet(document.content(), keywords)
        );
    }

    private String bestSnippet(String content, List<String> keywords) {
        String[] blocks = content.split("\\R\\s*\\R");
        String best = "";
        int bestScore = -1;
        for (String block : blocks) {
            String normalized = block.trim();
            if (normalized.isBlank()) {
                continue;
            }
            String lowerBlock = normalized.toLowerCase(Locale.ROOT);
            int score = 0;
            for (String keyword : keywords) {
                if (lowerBlock.contains(keyword.toLowerCase(Locale.ROOT))) {
                    score++;
                }
            }
            if (score > bestScore) {
                bestScore = score;
                best = normalized;
            }
        }
        if (best.isBlank()) {
            best = content.trim();
        }
        return best.length() > MAX_SNIPPET_CHARS ? best.substring(0, MAX_SNIPPET_CHARS) + "..." : best;
    }

    private List<String> extractKeywords(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        Set<String> keywords = new LinkedHashSet<>();
        for (String keyword : DOMAIN_KEYWORDS) {
            if (query.contains(keyword)) {
                keywords.add(keyword);
            }
        }
        for (String token : SPLIT_PATTERN.split(query)) {
            String normalized = token.trim();
            if (normalized.length() >= 2) {
                keywords.add(normalized);
            }
        }
        return keywords.stream().limit(12).toList();
    }

    private String blankToDefault(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue == null || defaultValue.isBlank() ? "未命名" : defaultValue;
        }
        return value.trim();
    }

    private List<KnowledgeDocument> loadDocuments() {
        List<KnowledgeDocument> current = documents;
        if (current != null) {
            return current;
        }
        try {
            Resource[] resources = resourcePatternResolver.getResources("classpath:document/*.md");
            List<KnowledgeDocument> loaded = new ArrayList<>();
            for (Resource resource : resources) {
                if (!resource.exists()) {
                    continue;
                }
                String filename = resource.getFilename() == null ? "unknown.md" : resource.getFilename();
                String content = resource.getContentAsString(StandardCharsets.UTF_8);
                loaded.add(new KnowledgeDocument("Markdown", filename, filename, content));
            }
            documents = loaded;
            return loaded;
        } catch (IOException e) {
            log.warn("本地 Markdown 知识库读取失败", e);
            return List.of();
        }
    }

    private record KnowledgeDocument(String sourceType, String sourceName, String title, String content) {
    }

    public record KnowledgeSource(String sourceType, String sourceName, String title, String content) {
    }

    public record KnowledgeSearchResult(String query, List<String> keywords, List<KnowledgeMatch> matches) {
        public boolean hasMatches() {
            return matches != null && !matches.isEmpty();
        }
    }

    public record KnowledgeMatch(String sourceType, String sourceName, String title, int score, String snippet) {
    }

    public record KnowledgeContext(String context, List<String> citations) {
        public static KnowledgeContext empty() {
            return new KnowledgeContext("", List.of());
        }

        public boolean hasContext() {
            return context != null && !context.isBlank();
        }
    }
}
