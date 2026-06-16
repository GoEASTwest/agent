# A1 多模态设备检修知识检索与作业系统

本仓库用于软件杯 A1 方向比赛项目演示，包含两个本地项目：

- `ai-agent-a1/ai-agent-master`：主项目，Spring Boot + Spring AI + Vue 3，负责设备检修中台、AI 问答、RAG、诊断、作业单和报告闭环。
- `image-backend-a1/image-backend-master`：图片后端候选融合模块，提供图片上传、审核、空间、标签和相似检索能力。

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

后端：

```powershell
cd "ai-agent-a1\ai-agent-master"
$env:api_key="your-dashscope-api-key"
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
- 若启用 PostgreSQL + PgVector，需要补充真实数据库配置并打开 `app.vectorstore.pgvector.enabled`。
