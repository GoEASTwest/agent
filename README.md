# A1 多模态设备检修知识检索与作业系统

本仓库用于软件杯 A1 方向比赛项目演示，包含两个本地项目：

- `ai-agent-a1/ai-agent-master`：主项目，Spring Boot + Spring AI + Vue 3，负责设备检修中台、AI 问答、RAG、诊断、作业单和报告闭环。
- `image-backend-a1/image-backend-master`：图片后端候选融合模块，提供图片上传、审核、空间、标签和相似检索能力。

## 基于原项目的二次开发修改说明

当前 `Li` 分支是在原始 AI Agent 项目基础上进行的二次开发，主要修改如下：

- 项目定位从原来的“旋转机械故障诊断 AI 智能体”，扩展为“软件杯 A1 多模态设备检修知识检索与作业系统”。
- 新增检修工作台前端页面，覆盖设备态势看板、设备台账、角色权限、模块管理、故障案例、多模态诊断、图片/视觉分析入口、作业单、作业流转、报告中心和 AI 知识问答。
- 将普通 AI 问答和工具智能体改为通过 DashScope OpenAI 兼容模式调用 Qwen，支持在 `.env` 中配置 `DASHSCOPE_API_KEY`、`DASHSCOPE_CHAT_MODEL` 等参数。
- 新增兼容模式聊天客户端和工具智能体，实现 Markdown 文件生成、PDF 生成、文件列表、文件下载、网页抓取、公开资源下载和可选联网搜索。
- 新增“检修文件中心”页面 `/files`，可集中查看、下载、复制工具智能体生成的 Markdown、PDF 和下载资源。
- 新增 Markdown 渲染能力，前端使用 `marked` + `dompurify` 将 AI 返回内容渲染成可读页面，而不是直接显示 Markdown 源码。
- 为工具智能体增加结构化执行步骤展示，前端可显示“分析任务、调用工具、生成结果”等过程状态。
- 新增后端文件列表和下载接口，支持 `/api/ai/manus/files` 和 `/api/ai/manus/files/download`。
- 新增 PDF 中文字体处理，解决中文生成 PDF 时乱码或字体缺失问题。
- 优化本地启动脚本和环境变量模板，支持 `.env` 加载、Java 21 检查、前后端一键启动。
- 默认关闭 PostgreSQL + PgVector 和 MCP 自动连接，保留相关配置，方便先稳定演示，再按需要切换到持久化和向量检索部署。
- 出于安全考虑，终端命令执行工具未开放给当前工具智能体，真实 API Key 不写入代码，生成文件保存在 `tmp/` 目录且不纳入 Git。

## 当前完成度

主项目已经具备可演示闭环：

- 设备态势看板
- 设备台账
- 角色权限
- 模块管理
- 故障案例知识库
- 多模态诊断
- Qwen 视觉分析入口
- 检修作业单
- 作业流转日志
- RAG 知识库
- 报告中心
- AI 知识问答

默认演示模式不强制依赖 PostgreSQL 或 MCP，便于比赛现场稳定启动。数据库表结构和 PgVector 配置已保留，后续可切换到持久化部署。

## 启动主项目

环境要求：

- Java 21
- Node.js 18+
- DashScope/Qwen API Key

可先复制环境变量模板：

```powershell
cd "ai-agent-a1\ai-agent-master"
Copy-Item .env.example .env
```

后端：

```powershell
cd "ai-agent-a1\ai-agent-master"
$env:DASHSCOPE_API_KEY="your-dashscope-api-key"
.\scripts\start-backend.ps1
```

前端：

```powershell
cd "ai-agent-a1\ai-agent-master"
.\scripts\start-frontend.ps1
```

访问：

```text
http://127.0.0.1:5173/love-app
```

## 重要说明

- 不要把真实 API Key 写入代码。
- 当前主业务数据使用内存演示数据。
- 默认已关闭 PostgreSQL + PgVector 和 MCP 自动连接，便于本地演示稳定启动。
- 若启用 PostgreSQL + PgVector，需要补充真实数据库配置并打开 `app.vectorstore.pgvector.enabled`。
