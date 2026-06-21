package com.hp.aiagent.app;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.hp.aiagent.advisor.MyLoggerAdvisor;
import com.hp.aiagent.advisor.ReReadingAdvisor;
import com.hp.aiagent.chatmemory.FileBasedChatMemory;
import com.hp.aiagent.rag.AppRagCustomAdvisorFactory;
import com.hp.aiagent.rag.LocalKnowledgeService;
import com.hp.aiagent.rag.QueryRewriter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

/**
 * @Auther:hp
 * @Date:2025/9/19
 * @Description:com.hp.aiagent.app
 **/
@Component
@Slf4j
public class MyApp {
    private final ChatClient chatClient;
    private final DashScopeCompatibleChatClient compatibleChatClient;
    private final LocalKnowledgeService localKnowledgeService;
    private static final String SYSTEM_PROMPT = """
            你是“多模态设备检修知识检索与作业系统”的检修智能体，面向风机、泵、轴承、齿轮箱、电机等工业设备。
            你的目标不是泛泛聊天，而是辅助现场人员完成知识检索、故障诊断、风险判断和检修作业闭环。

            回答时请优先使用如下结构：
            1. 现象归纳：用简短语言复述设备、部位、异常现象和关键参数。
            2. 风险等级：给出正常、关注、预警、严重四档之一，并说明依据。
            3. 可能原因：列出 2 到 4 个候选原因，按可能性排序。
            4. 需要补充的信息：如果信息不足，追问测点、振动值、温度、电流、频谱、润滑、图片可见特征等。
            5. 检修作业建议：给出可执行步骤，包括安全隔离、检查项目、工具备件、验收要点。
            6. 知识库依据：说明依据来自检修规程、故障案例或设备诊断经验；不确定时明确标注。

            当用户描述图片、外观缺陷或现场照片时，请按多模态检修思路分析可见特征，例如漏油、锈蚀、烧蚀、裂纹、磨损、松动、异物和颜色异常。
            当存在人身安全或设备扩大损坏风险时，先提示停机、断电、挂牌上锁、泄压和个人防护。
            不要编造不存在的传感器读数、标准编号或检修记录。
            """;

    public MyApp(ChatModel dashscopeChatModel, DashScopeCompatibleChatClient compatibleChatClient,
                 LocalKnowledgeService localKnowledgeService) {
        this.compatibleChatClient = compatibleChatClient;
        this.localKnowledgeService = localKnowledgeService;
        String fileDir = System.getProperty("user.dir") + "/tmp/chat-memory";
        ChatMemory chatMemory = new FileBasedChatMemory(fileDir);
//        ChatMemory chatMemory = new InMemoryChatMemory();
        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(chatMemory),
                        //自定义拦截器
                        new MyLoggerAdvisor()
                        //自定义 ReReadingAdvisor 重读增强推理
                        // ,new ReReadingAdvisor()
                )
                .build();

    }

    public String doChat(String message, String chatId) {
        LocalKnowledgeService.KnowledgeContext knowledgeContext = localKnowledgeService.retrieve(message);
        String content = compatibleChatClient.chat(SYSTEM_PROMPT, enrichWithLocalKnowledge(message, knowledgeContext))
                + citationText(knowledgeContext);
        log.info("content:{}", content);
        return content;

    }

    record Report(String title, List<String> suggestions) {
    }

    public Report doChatWithReport(String message, String chatId) {
        Report report = chatClient
                .prompt()
                .system(SYSTEM_PROMPT + "每次对话后都要生成诊断结果，标题为{用户名}的诊断报告，内容为建议列表")
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .entity(Report.class);
        log.info("loveReport: {}", report);
        return report;
    }

    //rag知识库问答
    @Autowired(required = false)
    @Qualifier("AppVectorStore")
    private VectorStore AppVectorStore;

    //pgvector向量库
    @Autowired(required = false)
    @Qualifier("pgVectorVectorStore")
    private VectorStore pgVectorVectorStore;

    //查询重写器使用
    @Resource
    private QueryRewriter queryRewriter;

    public String doChatWithRag(String message, String chatId) {
        if (AppVectorStore == null) {
            return doChat(message, chatId);
        }
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .advisors(new MyLoggerAdvisor())

                //本地知识库rag问答
                //.advisors(new QuestionAnswerAdvisor(AppVectorStore))

                //pgvector 向量存储的rag库
                //.advisors(new QuestionAnswerAdvisor(pgVectorVectorStore))

                .advisors(
                        AppRagCustomAdvisorFactory.createLoveAppRagCustomAdvisor(
                                AppVectorStore, "轴承"
                        )
                )

                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    /*使用工具*/
    @Resource
    private ToolCallback[] allTools;

    public String doChatWithTools(String message, String chatId) {
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))

                .advisors(new MyLoggerAdvisor())
                .tools(allTools)
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    /*mcp服务*/
    @Autowired(required = false)
    private ToolCallbackProvider toolCallbackProvider;

    public String doChatWithMcp(String message, String chatId) {
        if (toolCallbackProvider == null) {
            return "MCP 工具服务未启用，当前可使用检修诊断、RAG 问答和本地工具能力。";
        }
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))

                .advisors(new MyLoggerAdvisor())
                .tools(toolCallbackProvider)
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    public Flux<String> doChatByStream(String message, String chatId) {
        Flux<String> response;
        if (AppVectorStore == null) {
            LocalKnowledgeService.KnowledgeContext knowledgeContext = localKnowledgeService.retrieve(message);
            response = compatibleChatClient.stream(SYSTEM_PROMPT, enrichWithLocalKnowledge(message, knowledgeContext))
                    .concatWithValues(citationText(knowledgeContext));
        } else {
            response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .advisors(
                        AppRagCustomAdvisorFactory.createLoveAppRagCustomAdvisor(
                                AppVectorStore, "设备检修"
                        )
                )
                .stream()
                .content();
        }
        return response.onErrorResume(ex -> {
            log.error("DashScope chat stream failed", ex);
            return Flux.just("AI 服务调用失败：" + simplifyAiError(ex));
        });
    }

    private String enrichWithLocalKnowledge(String message, LocalKnowledgeService.KnowledgeContext knowledgeContext) {
        if (knowledgeContext == null || !knowledgeContext.hasContext()) {
            return message;
        }
        return """
                请优先结合以下本地 Markdown 知识库片段回答。若片段不足以支撑结论，请明确说明“知识库未覆盖该细节”，不要编造标准编号或数值。

                【本地知识库片段】
                %s

                【用户问题】
                %s
                """.formatted(knowledgeContext.context(), message);
    }

    private String citationText(LocalKnowledgeService.KnowledgeContext knowledgeContext) {
        if (knowledgeContext == null || !knowledgeContext.hasContext() || knowledgeContext.citations().isEmpty()) {
            return "";
        }
        return "\n\n**知识库引用**\n" + String.join("\n", knowledgeContext.citations().stream()
                .map(item -> "- " + item)
                .toList());
    }

    private String simplifyAiError(Throwable ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return "请检查 DashScope API Key、模型名称和账号额度。";
        }
        if (message.contains("AllocationQuota.FreeTierOnly")) {
            return "千问模型免费额度已用完，且账号开启了仅使用免费额度模式。请到 DashScope 控制台关闭仅免费额度模式、开通付费，或改用仍有额度的模型。";
        }
        if (message.contains("only support stream mode")) {
            return "当前模型只支持流式调用，请使用前端聊天的流式接口，或把 DASHSCOPE_CHAT_MODEL 改为 qwen-turbo、qwen-plus 等非纯推理模型。";
        }
        if (message.contains("401") || message.contains("Unauthorized")) {
            return "DashScope API Key 无效或没有被后端读取到，请检查 .env 里的 DASHSCOPE_API_KEY。";
        }
        return message;
    }


}
