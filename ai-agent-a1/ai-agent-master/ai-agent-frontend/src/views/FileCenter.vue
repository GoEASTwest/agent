<template>
  <main class="file-page">
    <nav class="top-links">
      <router-link to="/">返回首页</router-link>
      <router-link to="/manus">工具智能体</router-link>
    </nav>

    <section class="file-shell">
      <header class="page-head">
        <div>
          <p>Agent Files</p>
          <h1>检修文件中心</h1>
          <span>集中查看工具智能体生成的 Markdown、PDF 和下载资源。</span>
        </div>
        <button type="button" @click="loadFiles" :disabled="loading">
          {{ loading ? '刷新中' : '刷新文件' }}
        </button>
      </header>

      <div class="summary-row">
        <article v-for="item in summary" :key="item.type" :class="{ active: activeType === item.type }">
          <button type="button" @click="activeType = item.type">
            <span>{{ item.label }}</span>
            <strong>{{ item.count }}</strong>
          </button>
        </article>
      </div>

      <section v-if="error" class="notice error">
        {{ error }}
      </section>

      <section v-else-if="!loading && !filteredFiles.length" class="notice">
        <h2>暂无文件</h2>
        <p>到工具智能体页面输入“生成一个水泵日常检修 checklist 并生成 PDF”，完成后回到这里刷新。</p>
        <router-link to="/manus">去生成文件</router-link>
      </section>

      <section v-else class="file-grid">
        <article v-for="file in filteredFiles" :key="file.type + file.name" class="file-card">
          <div class="file-top">
            <span>{{ typeLabel(file.type) }}</span>
            <strong>{{ formatBytes(file.size) }}</strong>
          </div>
          <h2 :title="file.name">{{ file.name }}</h2>
          <p>{{ formatTime(file.modifiedAt) }}</p>
          <div class="file-actions">
            <a :href="downloadUrl(file)" target="_blank" rel="noopener">下载</a>
            <button type="button" @click="copyLink(file)">
              {{ copiedKey === file.type + file.name ? '已复制' : '复制链接' }}
            </button>
          </div>
        </article>
      </section>
    </section>
  </main>
</template>

<script>
import { apiUrl, fetchJson } from '../api';

export default {
  name: 'FileCenter',
  data() {
    return {
      files: [],
      activeType: 'all',
      loading: false,
      error: '',
      copiedKey: ''
    };
  },
  computed: {
    filteredFiles() {
      if (this.activeType === 'all') {
        return this.files;
      }
      return this.files.filter((file) => file.type === this.activeType);
    },
    summary() {
      const count = (type) => this.files.filter((file) => file.type === type).length;
      return [
        { type: 'all', label: '全部文件', count: this.files.length },
        { type: 'file', label: 'Markdown', count: count('file') },
        { type: 'pdf', label: 'PDF', count: count('pdf') },
        { type: 'download', label: '下载资源', count: count('download') }
      ];
    }
  },
  mounted() {
    this.loadFiles();
  },
  methods: {
    async loadFiles() {
      this.loading = true;
      this.error = '';
      try {
        this.files = await fetchJson('/ai/manus/files');
      } catch (error) {
        this.error = `文件列表读取失败：${error.message || error}`;
      } finally {
        this.loading = false;
      }
    },
    downloadUrl(file) {
      return apiUrl(file.downloadUrl);
    },
    typeLabel(type) {
      return {
        file: 'Markdown',
        pdf: 'PDF',
        download: '资源'
      }[type] || '文件';
    },
    formatBytes(value) {
      if (!Number.isFinite(value) || value <= 0) {
        return '0 B';
      }
      if (value < 1024) {
        return `${value} B`;
      }
      if (value < 1024 * 1024) {
        return `${(value / 1024).toFixed(1)} KB`;
      }
      return `${(value / 1024 / 1024).toFixed(1)} MB`;
    },
    formatTime(value) {
      if (!value) {
        return '未知时间';
      }
      return new Date(value).toLocaleString('zh-CN', {
        hour12: false,
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      });
    },
    async copyLink(file) {
      const fullUrl = new URL(this.downloadUrl(file), window.location.origin).toString();
      await navigator.clipboard.writeText(fullUrl);
      this.copiedKey = file.type + file.name;
      window.setTimeout(() => {
        if (this.copiedKey === file.type + file.name) {
          this.copiedKey = '';
        }
      }, 1400);
    }
  }
};
</script>

<style scoped>
.file-page {
  min-height: 100vh;
  background: #eef3ef;
  color: #14231d;
  padding: 28px;
}

.top-links {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-bottom: 22px;
}

.top-links a {
  color: #1f6f53;
  font-weight: 800;
  text-decoration: none;
}

.file-shell {
  max-width: 1120px;
  margin: 0 auto;
}

.page-head {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
  border: 1px solid #d4dfd9;
  border-radius: 8px;
  background: #ffffff;
  padding: 24px;
  box-shadow: 0 12px 34px rgba(16, 35, 29, 0.08);
}

.page-head p {
  margin: 0 0 8px;
  color: #2b7c5a;
  font-size: 13px;
  font-weight: 900;
}

.page-head h1 {
  margin: 0 0 8px;
  font-size: 34px;
  letter-spacing: 0;
}

.page-head span {
  color: #65756e;
  line-height: 1.6;
}

.page-head button,
.file-actions button,
.file-actions a,
.notice a {
  min-height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  font-weight: 800;
  text-decoration: none;
  cursor: pointer;
}

.page-head button {
  border: 0;
  background: #15241e;
  color: #ffffff;
  padding: 0 18px;
}

.summary-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin: 16px 0;
}

.summary-row article {
  border: 1px solid #d4dfd9;
  border-radius: 8px;
  background: #ffffff;
  overflow: hidden;
}

.summary-row article.active {
  border-color: #2b7c5a;
}

.summary-row button {
  width: 100%;
  min-height: 76px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border: 0;
  background: transparent;
  color: #14231d;
  padding: 0 18px;
  cursor: pointer;
}

.summary-row span {
  color: #65756e;
  font-weight: 800;
}

.summary-row strong {
  font-size: 30px;
}

.notice {
  border: 1px solid #d4dfd9;
  border-radius: 8px;
  background: #ffffff;
  padding: 28px;
}

.notice h2 {
  margin: 0 0 10px;
}

.notice p {
  margin: 0 0 18px;
  color: #65756e;
  line-height: 1.7;
}

.notice a {
  background: #1f6f53;
  color: #ffffff;
  padding: 0 16px;
}

.notice.error {
  border-color: #efb7b7;
  color: #9f1d1d;
}

.file-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.file-card {
  min-height: 192px;
  display: flex;
  flex-direction: column;
  border: 1px solid #d4dfd9;
  border-radius: 8px;
  background: #ffffff;
  padding: 18px;
  box-shadow: 0 12px 34px rgba(16, 35, 29, 0.08);
}

.file-top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.file-top span {
  border-radius: 99px;
  background: #edf3ef;
  color: #1f6f53;
  padding: 5px 9px;
  font-size: 12px;
  font-weight: 900;
}

.file-top strong {
  color: #65756e;
  font-size: 13px;
}

.file-card h2 {
  margin: 18px 0 8px;
  font-size: 19px;
  line-height: 1.35;
  word-break: break-word;
}

.file-card p {
  margin: 0;
  color: #65756e;
}

.file-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: auto;
  padding-top: 18px;
}

.file-actions a {
  background: #1f6f53;
  color: #ffffff;
  padding: 0 14px;
}

.file-actions button {
  border: 1px solid #c7d4ce;
  background: #ffffff;
  color: #14231d;
  padding: 0 14px;
}

@media (max-width: 900px) {
  .summary-row,
  .file-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 620px) {
  .file-page {
    padding: 20px;
  }

  .page-head,
  .summary-row,
  .file-grid {
    grid-template-columns: 1fr;
  }

  .page-head {
    display: grid;
  }
}
</style>
