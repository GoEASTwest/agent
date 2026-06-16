<template>
  <main class="agent-page">
    <router-link to="/" class="back">← 返回首页</router-link>
    <section class="agent-card">
      <header>
        <div>
          <p>Tool Agent</p>
          <h1>检修资料处理智能体</h1>
        </div>
      </header>

      <div class="chat-history" ref="chatHistory">
        <div class="message ai">
          <div class="content">可以让我检索资料、整理检修规程、生成报告草稿或下载公开维护手册。</div>
        </div>
        <div v-for="(msg, index) in messages" :key="index" :class="['message', msg.role]">
          <div class="content">{{ msg.content }}</div>
        </div>
      </div>

      <form class="input-area" @submit.prevent="sendMessage">
        <input v-model="inputMessage" placeholder="输入工具任务，例如：整理风机轴承检修 checklist" />
        <button type="submit" :disabled="isStreaming">{{ isStreaming ? '执行中' : '发送' }}</button>
      </form>
    </section>
  </main>
</template>

<script>
export default {
  name: 'Manus',
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
      this.messages.push({ role: 'user', content: message });
      this.inputMessage = '';
      this.connectSSE(message);
      this.scrollToBottom();
    },
    connectSSE(message) {
      const url = `http://localhost:8123/api/ai/manus/chat?message=${encodeURIComponent(message)}`;
      this.eventSource = new EventSource(url);
      this.isStreaming = true;
      const aiMessageIndex = this.messages.push({ role: 'ai', content: '' }) - 1;

      this.eventSource.onmessage = (event) => {
        const data = event.data;
        if (data === '[DONE]') {
          this.eventSource.close();
          this.isStreaming = false;
        } else {
          this.messages[aiMessageIndex].content += data;
          this.scrollToBottom();
        }
      };

      this.eventSource.onerror = () => {
        this.messages[aiMessageIndex].content += '\n\n工具智能体连接失败，请检查后端服务和模型密钥。';
        this.eventSource.close();
        this.isStreaming = false;
      };
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
  white-space: pre-wrap;
  word-break: break-word;
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
