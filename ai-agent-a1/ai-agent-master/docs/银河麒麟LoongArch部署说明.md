# 银河麒麟 LoongArch 部署说明

本文档用于比赛远程虚拟机部署。赛题硬性要求是软件运行在自主指令系统 LoongArch 架构和银河麒麟高级服务器操作系统 V11/V10 上，因此最终提交前需要在大赛提供的虚拟机中完成一次真实启动验证。

## 1. 环境确认

在服务器执行：

```bash
uname -m
cat /etc/os-release
```

期望结果：

- 架构包含 `loongarch64`
- 系统为银河麒麟高级服务器操作系统 V11/V10

如果 `uname -m` 显示 `x86_64`，只能说明普通电脑演示可用，不能作为最终赛题验收环境。

## 2. 安装基础依赖

优先使用系统自带软件源安装，避免下载 x86 版本导致无法运行。

```bash
sudo dnf update -y
sudo dnf install -y git unzip curl fontconfig
```

如果系统使用 `apt`：

```bash
sudo apt update
sudo apt install -y git unzip curl fontconfig
```

## 3. 安装 Java 21

项目后端使用 Spring Boot 3 和 Java 21。检查命令：

```bash
java -version
javac -version
```

若系统源提供 OpenJDK 21：

```bash
sudo dnf install -y java-21-openjdk java-21-openjdk-devel
```

如果源里没有 Java 21，需要使用大赛平台或龙芯生态提供的 LoongArch64 JDK 包。不要使用 Windows 或 x86_64 JDK。

配置示例：

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
export PATH=$JAVA_HOME/bin:$PATH
```

## 4. 安装 Node.js 和 npm

前端构建需要 Node.js 18+。检查命令：

```bash
node -v
npm -v
```

优先使用银河麒麟/龙芯源中的 Node.js：

```bash
sudo dnf install -y nodejs npm
```

如果系统源版本过低，需要下载 LoongArch64 对应的 Node.js 包，不能使用 x64 安装包。

## 5. 配置模型密钥

项目支持云端 Qwen/DashScope。

```bash
cd ai-agent-a1/ai-agent-master
cp .env.example .env
vi .env
```

至少填写：

```bash
DASHSCOPE_API_KEY=你的DashScope密钥
DASHSCOPE_CHAT_MODEL=qwen-plus
DASHSCOPE_COMPATIBLE_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
```

不要把真实 `.env` 提交到 Git。

## 6. 构建前端

```bash
cd ai-agent-a1/ai-agent-master/ai-agent-frontend
npm install
npm run build
```

构建产物在：

```text
ai-agent-frontend/dist
```

## 7. 构建后端

```bash
cd ai-agent-a1/ai-agent-master
chmod +x mvnw
./mvnw -DskipTests package
```

构建成功后会生成：

```text
target/ai-agent-0.0.1-SNAPSHOT.jar
```

## 8. 启动后端

```bash
cd ai-agent-a1/ai-agent-master
set -a
source .env
set +a
java -jar target/ai-agent-0.0.1-SNAPSHOT.jar
```

健康检查：

```bash
curl http://127.0.0.1:8123/api/health
```

## 9. 启动前端预览

比赛演示阶段可以使用 Vite：

```bash
cd ai-agent-a1/ai-agent-master/ai-agent-frontend
npm run dev -- --host 0.0.0.0 --port 5173
```

访问：

```text
http://服务器IP:5173
```

若要正式部署，可将 `dist` 放到 Nginx，并把 `/api` 反向代理到 `http://127.0.0.1:8123/api`。

## 10. 验收演示路径

1. 打开首页，进入设备检修中台。
2. 查看态势看板、设备台账和故障案例。
3. 在多模态诊断中输入温度、振动、电流、图片特征，生成风险诊断和作业单。
4. 在检修作业单中演示状态流转和归档。
5. 在知识库中提交一条现场经验，专家审核通过后观察其进入案例库和文件中心。
6. 在报告中心修正 AI 输出，并归档修正记录。
7. 打开文件中心查看 Markdown/PDF 报告。
8. 在 AI 知识问答中提问，确认回答可引用本地知识库。

## 11. 当前默认部署策略

- 默认演示模式不强制依赖 PostgreSQL、PgVector 或 MCP，优先保证比赛现场稳定启动。
- PostgreSQL + PgVector 配置已保留，适合后续做真实持久化和向量检索。
- 文件中心生成的 Markdown/PDF 位于 `tmp/` 目录，默认不提交到 Git。
