package com.hp.aiagent.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ConditionalOnBean(name = "pgVectorVectorStore")
public class VectorKnowledgeService {

    private final VectorStore vectorStore;

    public VectorKnowledgeService(@Qualifier("pgVectorVectorStore") VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public LocalKnowledgeService.KnowledgeContext retrieve(String query) {
        if (query == null || query.isBlank()) {
            return LocalKnowledgeService.KnowledgeContext.empty();
        }
        Filter.Expression expression = new FilterExpressionBuilder()
                .eq("domain", "maintenance")
                .build();
        List<Document> documents = vectorStore.similaritySearch(SearchRequest.builder()
                .query(query)
                .topK(5)
                .similarityThreshold(0.45)
                .filterExpression(expression)
                .build());
        if (documents == null || documents.isEmpty()) {
            return LocalKnowledgeService.KnowledgeContext.empty();
        }
        String context = String.join("\n\n", documents.stream()
                .map(document -> "来源：PgVector：" + sourceName(document) + "\n" + safeSnippet(document.getText()))
                .toList());
        List<String> citations = documents.stream()
                .map(document -> "PgVector：" + sourceName(document))
                .distinct()
                .toList();
        return new LocalKnowledgeService.KnowledgeContext(context, citations);
    }

    private String sourceName(Document document) {
        Object filename = document.getMetadata().get("filename");
        Object chunkNo = document.getMetadata().get("chunkNo");
        String source = filename == null ? "本地检修知识库" : filename.toString();
        return chunkNo == null ? source : source + " #" + chunkNo;
    }

    private String safeSnippet(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.length() > 700 ? text.substring(0, 700) + "..." : text;
    }
}
