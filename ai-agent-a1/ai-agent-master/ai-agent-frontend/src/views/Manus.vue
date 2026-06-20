<template>
  <main class="agent-page">
    <nav class="top-links">
      <router-link to="/" class="back">返回首页</router-link>
      <router-link to="/files" class="back">文件中心</router-link>
    </nav>
    <section class="agent-card">
      <header>
        <div>
          <p>Tool Agent</p>
          <h1>检修资料处理智能体</h1>
        </div>
      </header>

      <div class="chat-history" ref="chatHistory">
        <div class="message ai">
          <div class="content">可以让我整理检修规程、生成报告草稿、保存 Markdown 文件、生成 PDF 或下载公开维护资料。</div>
        </div>
        <div v-for="(msg, index) in messages" :key="index" :class="['message', msg.role]">
          <div class="content">
            <div v-if="msg.steps?.length" class="agent-steps">
              <div v-for="step in msg.steps" :key="step.id" :class="['agent-step', step.status]">
                <span class="step-dot"></span>
                <div>
                  <strong>{{ step.title }}</strong>
                  <small>{{ step.detail }}</small>
                </div>
              </div>
            </div>

            <MarkdownMessage v-if="msg.role === 'ai' && msg.content" :content="msg.content" />
            <p v-else-if="msg.role !== 'ai'" class="message-text">{{ msg.content }}</p>

            <div v-if="msg.files?.length" class="download-list">
              <a
                v-for="file in msg.files"
                :key="file.url"
                class="download-link"
                :href="file.url"
                target="_blank"
                rel="noopener"
              >
                {{ file.label }}
              </a>
            </div>
          </div>
        </div>
      </div>

      <form class="input-area" @submit.prevent="sendMessage">
        <input v-model="inputMessage" placeholder="输入工具任务，例如：整理风机轴承检修 checklist 并生成 PDF" />
        <button type="submit" :disabled="isStreaming">{{ isStreaming ? '执行中' : '发送' }}</button>
      </form>
    </section>
  </main>
</template>

<script>
import { apiUrl } from '../api';
import MarkdownMessage from '../components/MarkdownMessage.vue';

const AGENT_EVENT_PREFIX = '__AGENT_EVENT__';

export default {
  name: 'Manus',
  components: {
    MarkdownMessage
  },
  data() {
    return {
      messages: [],
      inputMessage: '',
      eventSource: null,
      isStreaming: false
    };
  },
  mounted() {
    this.scrollToBottom();
  },
  methods: {
    sendMessage() {
      if (!this.inputMessage.trim() || this.isStreaming) return;
      const message = this.inputMessage.trim();
      this.messages.push({ role: 'user', content: message, files: [], steps: [], rawContent: message });
      this.inputMessage = '';
      this.connectSSE(message);
      this.scrollToBottom();
    },
    connectSSE(message) {
      if (this.eventSource) {
        this.eventSource.close();
      }

      const url = apiUrl(`/ai/manus/chat?message=${encodeURIComponent(message)}`);
      this.eventSource = new EventSource(url);
      this.isStreaming = true;
      const aiMessageIndex = this.messages.push({ role: 'ai', content: '', files: [], steps: [], rawContent: '' }) - 1;
      let hasReceivedContent = false;
      let finished = false;

      this.eventSource.onmessage = (event) => {
        const data = event.data;
        if (data === '[DONE]') {
          finished = true;
          this.finalizeMessage(aiMessageIndex);
          this.eventSource.close();
          this.isStreaming = false;
          return;
        }

        hasReceivedContent = true;
        if (data.startsWith(AGENT_EVENT_PREFIX)) {
          this.applyAgentEvent(aiMessageIndex, data.slice(AGENT_EVENT_PREFIX.length));
        } else {
          this.messages[aiMessageIndex].rawContent += data;
          this.updateMessageContent(aiMessageIndex);
        }
        this.scrollToBottom();
      };

      this.eventSource.onerror = () => {
        if (!finished && !hasReceivedContent) {
          this.messages[aiMessageIndex].rawContent += '\n\n工具智能体连接失败，请检查后端服务和模型密钥。';
          this.updateMessageContent(aiMessageIndex);
        }
        this.eventSource.close();
        this.isStreaming = false;
      };
    },
    applyAgentEvent(index, payload) {
      try {
        const event = JSON.parse(payload);
        if (event.kind !== 'step') return;
        const message = this.messages[index];
        const existingIndex = message.steps.findIndex((step) => step.id === event.id);
        const step = {
          id: event.id,
          title: event.title,
          status: event.status,
          detail: event.detail
        };
        if (existingIndex >= 0) {
          message.steps.splice(existingIndex, 1, step);
        } else {
          message.steps.push(step);
        }
      } catch (error) {
        this.messages[index].rawContent += payload;
        this.updateMessageContent(index);
      }
    },
    updateMessageContent(index) {
      const parsed = this.parseDownloadLinks(this.messages[index].rawContent);
      this.messages[index].content = parsed.content;
      this.messages[index].files = parsed.files;
    },
    finalizeMessage(index) {
      this.updateMessageContent(index);
      this.scrollToBottom();
    },
    parseDownloadLinks(text) {
      const files = [];
      const cleanedLines = [];
      const seen = new Set();
      const downloadPattern = /Download URL:\s*(\/api)?(\/ai\/manus\/files\/download\?type=([a-z]+)&name=([^\s]+))/i;
      const markdownDownloadPattern = /\[[^\]]*下载[^\]]*\]\((\/api)?(\/ai\/manus\/files\/download\?type=([a-z]+)&name=([^)]+))\)/i;

      for (const line of text.split('\n')) {
        const match = line.match(downloadPattern) || line.match(markdownDownloadPattern);
        if (!match) {
          cleanedLines.push(line);
          continue;
        }

        const type = match[3];
        const encodedName = match[4];
        const url = apiUrl(match[2]);
        if (!seen.has(url)) {
          seen.add(url);
          files.push({
            url,
            label: this.downloadLabel(type, encodedName)
          });
        }
      }

      return {
        content: cleanedLines.join('\n').trim(),
        files
      };
    },
    downloadLabel(type, encodedName) {
      const name = decodeURIComponent(encodedName);
      const labelMap = {
        file: '下载 Markdown',
        pdf: '下载 PDF',
        download: '下载资源'
      };
      return `${labelMap[type] || '下载文件'}：${name}`;
    },
    scrollToBottom() {
      this.$nextTick(() => {
        const chatHistory = this.$refs.chatHistory;
        if (chatHistory) {
          chatHistory.scrollTop = chatHistory.scrollHeight;
        }
      });
    }
  },
  beforeUnmount() {
    if (this.eventSource) {
      this.eventSource.close();
    }
  }
};
</script>

<style scoped>
.agent-page {
  min-height: 100vh;
  background: #101b1d;
  padding: 28px;
  color: #ecf5f1;
}

.top-links {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
}

.back {
  color: #8cd8ae;
  text-decoration: none;
  font-weight: 700;
}

.agent-card {
  max-width: 980px;
  height: calc(100vh - 94px);
  margin: 24px auto 0;
  display: flex;
  flex-direction: column;
  border: 1px solid rgba(236, 245, 241, 0.16);
  border-radius: 8px;
  background: #f7fbf8;
  color: #10231d;
  overflow: hidden;
}

header {
  padding: 20px 24px;
  border-bottom: 1px solid #d6e0da;
}

header p {
  margin: 0 0 6px;
  color: #2f8a61;
  font-weight: 800;
}

h1 {
  margin: 0;
  font-size: 28px;
}

.chat-history {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

.message {
  display: flex;
  margin: 12px 0;
}

.content {
  max-width: min(760px, 84%);
  border-radius: 8px;
  padding: 13px 15px;
  line-height: 1.75;
  white-space: normal;
  word-break: break-word;
}

.message-text {
  margin: 0;
  white-space: pre-wrap;
}

.agent-steps {
  display: grid;
  gap: 8px;
  margin-bottom: 12px;
}

.agent-step {
  display: grid;
  grid-template-columns: 14px minmax(0, 1fr);
  gap: 9px;
  align-items: start;
  padding: 8px 10px;
  border: 1px solid #d7e4dc;
  border-radius: 6px;
  background: #f7fbf8;
}

.step-dot {
  width: 9px;
  height: 9px;
  margin-top: 8px;
  border-radius: 50%;
  background: #9ba9a1;
}

.agent-step.running .step-dot {
  background: #2d7ff9;
}

.agent-step.complete .step-dot {
  background: #1f8f5f;
}

.agent-step.error .step-dot {
  background: #c63f3f;
}

.agent-step strong {
  display: block;
  font-size: 14px;
}

.agent-step small {
  display: block;
  margin-top: 2px;
  color: #597267;
  line-height: 1.5;
}

.download-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.download-link {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  padding: 0 12px;
  border: 1px solid #b7d1c2;
  border-radius: 6px;
  background: #eef7f1;
  color: #15563d;
  font-weight: 800;
  text-decoration: none;
}

.download-link:hover {
  border-color: #1f6f53;
  background: #dff0e5;
}

.user {
  justify-content: flex-end;
}

.user .content {
  background: #14231d;
  color: #ffffff;
}

.ai .content {
  background: #ffffff;
  border: 1px solid #dce5df;
}

.input-area {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 104px;
  gap: 12px;
  padding: 18px;
  border-top: 1px solid #d6e0da;
}

input {
  min-height: 46px;
  border: 1px solid #cad6d0;
  border-radius: 6px;
  padding: 0 14px;
}

button {
  min-height: 46px;
  border: 0;
  border-radius: 6px;
  background: #1f6f53;
  color: #ffffff;
  font-weight: 800;
}
</style>
