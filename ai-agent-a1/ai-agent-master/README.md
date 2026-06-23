# 旋转机械故障诊断AI 智能体

## 项目概述

这是一个基于Spring AI和Vue.js构建的智能旋转机械故障诊断系统，专门用于风机、泵、轴承、齿轮箱、电机等旋转设备的故障诊断和预测性维护。系统集成了先进的AI技术、RAG（检索增强生成）知识库、向量数据库和多种诊断工具，为工业设备维护提供智能化的解决方案。

## 当前分支二次开发说明

当前 `Li` 分支在原项目基础上做了面向软件杯 A1 场景的二次开发，重点不是只保留原来的通用故障诊断对话，而是扩展成一个可演示的“设备检修知识检索与作业闭环系统”。主要改动如下：

### 1. 业务场景扩展

- 将项目主题调整为“多模态设备检修知识检索与作业系统”。
- 新增检修工作台页面 `/love-app`，覆盖设备看板、台账、角色、模块、案例、诊断、图片分析、作业单、作业流转、完成度、知识库和报告归档。
- 后端新增/完善检修演示数据和接口，支持前端在没有数据库的情况下完成演示闭环。

### 2. Qwen / DashScope 调用方式调整

- 新增 `DashScopeCompatibleChatClient`，普通 AI 问答改为通过 DashScope OpenAI 兼容模式调用。
- 新增 `DashScopeCompatibleToolAgent`，工具智能体也改为兼容模式 function calling，规避部分模型在 Spring AI Alibaba 原生工具调用中的不稳定问题。
- 支持通过环境变量配置：
  - `DASHSCOPE_API_KEY`
  - `DASHSCOPE_CHAT_MODEL`
  - `DASHSCOPE_COMPATIBLE_BASE_URL`
- 默认兼容模式 Base URL 为 `https://dashscope.aliyuncs.com/compatible-mode/v1`。

### 3. 工具智能体增强

- `/manus` 页面从原来的普通工具对话升级为检修资料处理智能体。
- 支持生成 Markdown 检修规程、生成 PDF、读取/列出已保存文件、抓取公开网页、下载公开资源。
- `SEARCH_API_KEY` 为可选配置，未配置时可跳过联网搜索能力。
- 出于安全考虑，当前兼容模式工具智能体未开放终端命令执行。
- 后端 SSE 增加 `[DONE]` 结束标记，避免浏览器把正常结束误判为连接异常。
- 工具执行过程会输出结构化事件，前端可展示“分析任务、工具执行中、执行完成/失败、生成最终回答”等步骤。

### 4. 文件中心和下载能力

- 新增后端文件列表接口：
  - `GET /api/ai/manus/files`
- 新增后端文件下载接口：
  - `GET /api/ai/manus/files/download?type=file|pdf|download&name=文件名`
- 新增前端页面 `/files`，集中展示工具智能体生成的 Markdown、PDF 和下载资源。
- 文件中心支持按类型筛选、下载文件、复制下载链接。

### 5. 知识沉淀、审核与修正闭环

- 新增一线检修经验提交能力，支持填写设备类型、故障名称、症状、图片特征、原因、处理方案和现场经验。
- 新增专家审核流程，知识条目状态支持 `待审核`、`已通过`、`已驳回`。
- 专家审核通过后，经验会自动转入故障案例库，并生成 Markdown/PDF 归档文件进入文件中心。
- 新增知识检索接口 `POST /api/maintenance/knowledge/search`，可统一检索本地 Markdown、故障案例库和已审核经验，并返回引用来源、命中片段和匹配分。
- 检修工作台“知识库”页新增“知识检索与引用”面板，可直接输入故障现象验证检索效果。
- AI 知识问答会复用同一套动态知识来源，审核通过的现场经验可被后续问答引用。
- 报告中心新增“专家修正 AI 输出”，可修正风险等级、证据、原因和处理措施，并归档修正记录。
- 该闭环对应赛题中“知识沉淀与更新”和“手动标注与修正大模型输出结果”的要求。

### 6. 可选 PostgreSQL 持久化

- 默认仍使用内存演示数据，保证没有数据库时也能稳定启动。
- 新增 `APP_JDBC_PERSISTENCE_ENABLED` 开关，设置为 `true` 后启用 PostgreSQL 业务持久化。
- 持久化范围覆盖设备台账、故障案例、检修作业单、作业流转记录、诊断报告、知识提交与审核记录。
- 诊断请求会写入诊断历史，AI 诊断自动生成的作业单会保存到作业单表。
- AI 专家修正记录新增独立持久化表，同时保留修正报告归档。
- 报告、任务、诊断历史新增分页/筛选接口，便于后续扩展大量记录场景。
- 后端启动时会自动执行 `src/main/resources/sql/maintenance_schema.sql` 和 `src/main/resources/sql/maintenance_seed.sql`，初始化业务表和演示数据。
- PgVector 保持独立开关，打开后可使用真实向量知识库；未打开时仍保留内存演示兜底。

### 7. PgVector 真实向量 RAG

- 新增 `APP_PGVECTOR_ENABLED` 开关，设置为 `true` 后启用 PostgreSQL + PgVector 向量知识库。
- 启用 PgVector 后，系统会将 `src/main/resources/document/*.md` 切分为 chunks，写入 `vector_store` 表。
- AI 知识问答会优先使用 PgVector 相似度检索；PgVector 未启用或无命中时，自动回退到本地 Markdown、故障案例和已审核经验检索。

### 8. 视觉分析作业闭环

- Qwen-VL / 本地规则视觉分析结果会返回可见缺陷、风险等级、相似案例和检修建议。
- 视觉分析页支持上传本地图片进行特征识别兜底，也支持公网图片 URL 调用 Qwen-VL。
- 前端“视觉分析”页支持将视觉结果同步到多模态诊断输入。
- 支持基于视觉结果一键生成诊断与检修作业单，并复用诊断历史、任务、报告等持久化链路。

### 9. Markdown 和 PDF 展示优化

- 前端新增 `MarkdownMessage.vue`，使用 `marked` + `dompurify` 渲染 AI 返回内容，避免直接显示 Markdown 源码。
- 检修工作台 AI 问答和工具智能体回答都已接入 Markdown 渲染。
- 新增 `PdfFontProvider`，并调整 PDF 生成逻辑，解决中文 PDF 字体和乱码问题。

### 10. 本地运行稳定性优化

- 新增 `.env.example`，方便配置 DashScope、数据库、搜索等环境变量。
- 优化 `scripts/start-backend.ps1`，支持加载 `.env`、检查 Java 21、默认关闭 PgVector/MCP 自动连接。
- 优化 `scripts/start-frontend.ps1`，自动检查依赖并启动 Vite。
- 新增检修工作台“一键重置演示数据”，可恢复设备、案例、作业单和知识审核种子数据，避免答辩演示数据混乱。
- 调整 `vite.config.js` 和统一前端 `src/api.js`，解决本地开发时 `/api` 代理到后端的问题。
- 默认演示模式使用内存数据，不强制依赖 PostgreSQL、PgVector 或 MCP，便于比赛现场稳定启动。

### 11. 作业审批流程增强

- 作业单状态流转补充为 `待派工 -> 处理中 -> 待验收 -> 专家复核 -> 已归档`。
- 专家验收不通过时支持流转到 `驳回整改`，再回到 `处理中` 或 `待验收`。
- 每次流转都会生成作业流转日志，报告归档时会写入流转记录，便于答辩展示真实检修闭环。

### 12. 银河麒麟 / LoongArch 部署补充

- 新增 `docs/银河麒麟LoongArch部署说明.md`，说明在比赛指定虚拟机上检查 `loongarch64` 架构、安装 Java 21、安装 Node.js、配置 DashScope、构建和启动项目的步骤。
- 当前 Windows 环境已完成前后端构建验证；最终提交前仍需在大赛提供的银河麒麟 LoongArch 虚拟机中完成真实运行验证。

### 13. 赛题要求对照材料

- 新增 `docs/赛题要求对照表.md`，按赛题硬性运行环境、基本功能、数据持久化、非功能要求和答辩演示路径逐项对应页面、接口和关键文件。
- 该文档用于本地收尾自检，也可作为后续答辩材料的结构基础。

### 14. 与原项目保留关系

- 原有 Spring Boot、Vue 3、Spring AI、RAG、工具类、MCP 搜索器等结构仍保留。
- PostgreSQL + PgVector 配置仍保留，后续可按需要重新打开。
- 原有通用工具类仍存在，但当前演示主链路优先使用 DashScope 兼容模式的聊天和工具智能体。
- 真实 API Key 不写入代码，生成文件保存在 `tmp/` 目录，默认不提交到 Git。


## 🏗️ 技术架构

### 后端技术栈
- **框架**: Spring Boot 3.4.9 + Java 21
- **AI框架**: Spring AI + 阿里云DashScope AI
- **数据库**: PostgreSQL + PgVector (向量数据库)
- **文档处理**: iText PDF + Markdown解析
- **工具库**: Hutool AI + JSoup网页抓取
- **API文档**: Knife4j (Swagger)

### 前端技术栈
- **框架**: Vue 3 + Vite
- **路由**: Vue Router 4
- **HTTP客户端**: Axios
- **实时通信**: Server-Sent Events (SSE)

### 核心模块
- **MCP搜索器**: 独立的MCP (Model Context Protocol) 搜索服务
- **RAG系统**: 基于向量数据库的知识检索增强
- **工具集**: 多种AI工具集成

## 🎯 核心功能

### 1. AI故障诊断大师
**路径**: `/app`

专业的旋转机械故障诊断专家，具备以下能力：

#### 诊断场景覆盖
- **正常运行状态**: 建立振动基线、温度监控、噪声分析、电流监测
- **早期故障状态**: 轴承点蚀、轻微不对中、齿轮磨损、电机断条检测
- **严重故障状态**: 轴承损坏、主轴裂纹、齿轮断齿、紧急停机判断

#### 专业诊断能力
- 振动频谱分析（FFT、包络谱分析）
- 温度异常监测
- 噪声特征识别
- 电流频谱分析
- 油液分析建议

#### 交互式诊断流程
1. **状态评估**: 根据用户描述判断设备当前状态
2. **信息收集**: 引导用户提供关键参数（振动、温度、频率等）
3. **专业分析**: 基于专业知识库进行故障分析
4. **处理建议**: 提供针对性的维修和预防措施

### 2. AI超级智能体 (YuManus)
**路径**: `/manus`

通用AI助手，集成多种工具，能够：
- 文件操作（读取、写入、搜索）
- 网络搜索和信息抓取
- PDF报告生成
- 终端命令执行
- 资源下载和管理

## 📚 知识库系统

### 专业文档库
系统内置了完整的旋转机械故障诊断知识库：

1. **paper1.md**: 正常运行状态诊断
   - 振动基线建立方法
   - 风机正常频谱特征
   - 轴承温度监控标准
   - 齿轮箱声音判断
   - 电机电流分析

2. **paper2.md**: 早期故障诊断
   - 轴承早期点蚀检测
   - 轻微不对中识别
   - 齿轮早期磨损判断
   - 电机断条检测

3. **paper3.md**: 严重故障诊断
   - 轴承严重损坏紧急信号
   - 主轴裂纹检测方法
   - 齿轮断齿故障特征

### RAG增强检索
- **向量存储**: 使用PgVector存储文档向量
- **智能检索**: 基于语义相似度的文档检索
- **上下文增强**: 结合检索内容进行更准确的回答

## 🛠️ 工具集成

### 核心工具集
- **WebSearchTool**: 网络搜索和信息获取
- **FileOperationTool**: 文件系统操作
- **PDFGenerationTool**: 诊断报告生成
- **WebScrapingTool**: 网页内容抓取
- **TerminalOperationTool**: 系统命令执行
- **ResourceDownloadTool**: 资源下载管理

### MCP工具集成
- 支持MCP协议的工具调用
- 可扩展的工具生态系统
- 标准化的工具接口

## 🚀 快速开始

### 环境要求
- Java 21+
- Node.js 18+
- Maven Wrapper，已随项目提供
- DashScope/Qwen API Key
- PostgreSQL + PgVector，仅在启用持久化向量库时需要

### 后端启动
```powershell
# 1. 配置 AI API 密钥，也可以复制 .env.example 为 .env 后填写
$env:DASHSCOPE_API_KEY="your-dashscope-api-key"

# 2. 启动应用
.\scripts\start-backend.ps1
```

默认演示模式使用内存业务数据，并关闭 PostgreSQL + PgVector 与 MCP 自动连接。若需要启用业务持久化，请设置 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`，并打开 `APP_JDBC_PERSISTENCE_ENABLED=true`。若需要启用真实向量检索，请确认 PostgreSQL 已安装 pgvector 扩展，并打开 `APP_PGVECTOR_ENABLED=true`。

银河麒麟/LoongArch 虚拟机中如果 `.env` 没有被 Java 进程正确读取，建议直接使用数据库持久化启动脚本。脚本会强制开启 PostgreSQL 业务持久化、关闭 PgVector，并默认连接 `jdbc:postgresql://127.0.0.1:5432/ai_agent`：

```bash
cd ~/桌面/agent/ai-agent-a1/ai-agent-master
chmod +x start-backend-db.sh
./start-backend-db.sh
```

### 本地交付前检查

```powershell
.\scripts\check-local.ps1
```

该脚本会检查当前分支是否为 `Li`、Java 21、Node.js/npm、关键环境变量，并依次执行后端 `package` 和前端 `build`。建议每次功能收尾和提交前运行一次。

### 前端启动
```powershell
.\scripts\start-frontend.ps1
```

### MCP搜索器启动
```bash
cd mcp-searcher
mvn spring-boot:run
```

## 📡 API接口

### 主要端点
- `GET /ai/app/chat/sync` - 同步诊断对话
- `GET /ai/app/chat/sse` - 流式诊断对话
- `GET /ai/manus/chat` - 超级智能体对话
- `GET /health` - 健康检查

### API文档
访问 `http://localhost:8123/api/swagger-ui.html` 查看完整API文档

## 🔧 配置说明

### 应用配置
```yaml
spring:
  application:
    name: ai-agent
  datasource:
    url: jdbc:postgresql://localhost:5432/ai_agent
  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY:${api_key:}}
    chat:
      options:
        model: qwq-plus

server:
  port: 8123
  servlet:
    context-path: /api
```

### 向量数据库配置
系统支持PgVector向量数据库，用于RAG功能：
- 索引类型: HNSW
- 距离度量: COSINE_DISTANCE
- 维度: 1536 (默认)

## 💡 使用场景

### 工业维护
- **预防性维护**: 基于设备状态预测维护需求
- **故障诊断**: 快速定位设备故障原因
- **知识管理**: 积累和传承诊断经验
- **培训支持**: 为维护人员提供学习平台

### 典型工作流程
1. **设备状态描述**: 用户描述设备当前状态和异常现象
2. **参数收集**: AI引导收集关键诊断参数
3. **智能分析**: 基于知识库进行专业分析
4. **诊断报告**: 生成详细的诊断报告和处理建议
5. **跟踪记录**: 保存诊断历史，便于后续参考

## 🔍 项目结构

```
ai-agent/
├── src/main/java/com/hp/aiagent/
│   ├── agent/              # AI代理实现
│   │   ├── BaseAgent.java  # 基础代理类
│   │   ├── YuManus.java    # 超级智能体
│   │   └── ToolCallAgent.java # 工具调用代理
│   ├── app/                # 应用核心
│   │   └── MyApp.java      # 故障诊断应用
│   ├── controller/         # REST控制器
│   ├── tools/              # 工具集成
│   ├── rag/                # RAG系统
│   ├── chatmemory/         # 对话记忆
│   └── config/             # 配置类
├── ai-agent-frontend/      # Vue前端
├── mcp-searcher/           # MCP搜索服务
└── src/main/resources/
    └── document/           # 知识库文档
```




## 📞 联系方式

- 项目维护者: Li
- 邮箱: 1548610658@qq.com
- 项目链接: ([GoEASTwest/agent](https://github.com/GoEASTwest/agent))

---
