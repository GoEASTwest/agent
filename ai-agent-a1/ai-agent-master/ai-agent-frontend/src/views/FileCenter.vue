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
          <span>集中查看、预览和下载工具智能体生成的 Markdown、PDF 与下载资源。</span>
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

      <section class="toolbar">
        <label>
          <span>搜索文件</span>
          <input v-model.trim="searchText" placeholder="输入文件名关键词" />
        </label>
        <label>
          <span>排序方式</span>
          <select v-model="sortMode">
            <option value="modified-desc">最新优先</option>
            <option value="modified-asc">最早优先</option>
            <option value="name-asc">名称 A-Z</option>
            <option value="size-desc">文件从大到小</option>
          </select>
        </label>
      </section>

      <section v-if="error" class="notice error">
        {{ error }}
      </section>

      <section v-else-if="!loading && !visibleFiles.length" class="notice">
        <h2>暂无匹配文件</h2>
        <p>可以调整筛选条件，或到工具智能体页面生成一份检修规程和 PDF。</p>
        <router-link to="/manus">去生成文件</router-link>
      </section>

      <section v-else class="workspace">
        <div class="file-grid">
          <article
            v-for="file in visibleFiles"
            :key="file.type + file.name"
            :class="['file-card', { selected: selectedFileKey === file.type + file.name }]"
          >
            <div class="file-top">
              <span>{{ typeLabel(file.type) }}</span>
              <strong>{{ formatBytes(file.size) }}</strong>
            </div>
            <h2 :title="file.name">{{ file.name }}</h2>
            <p>{{ formatTime(file.modifiedAt) }}</p>
            <div class="file-actions">
              <button v-if="file.type === 'file'" type="button" @click="previewMarkdown(file)">
                预览
              </button>
              <a v-else :href="inlineUrl(file)" target="_blank" rel="noopener">打开</a>
              <a :href="downloadUrl(file)" target="_blank" rel="noopener">下载</a>
              <button type="button" @click="copyLink(file)">
                {{ copiedKey === file.type + file.name ? '已复制' : '复制链接' }}
              </button>
            </div>
          </article>
        </div>

        <aside class="preview-panel">
          <div v-if="selectedFile" class="preview-head">
            <div>
              <span>{{ typeLabel(selectedFile.type) }}</span>
              <h2>{{ selectedFile.name }}</h2>
              <p>{{ formatTime(selectedFile.modifiedAt) }} · {{ formatBytes(selectedFile.size) }}</p>
            </div>
            <a :href="downloadUrl(selectedFile)" target="_blank" rel="noopener">下载</a>
          </div>

          <div v-if="previewLoading" class="preview-empty">正在读取预览内容...</div>
          <div v-else-if="previewError" class="preview-empty error">{{ previewError }}</div>
          <MarkdownMessage v-else-if="selectedFile && previewContent" :content="previewContent" />
          <div v-else class="preview-empty">
            <h2>选择一份 Markdown 文件预览</h2>
            <p>PDF 和其他资源可直接点击“打开”在浏览器中查看。</p>
          </div>
        </aside>
      </section>
    </section>
  </main>
</template>

<script>
import { apiUrl, fetchJson } from '../api';
import MarkdownMessage from '../components/MarkdownMessage.vue';

export default {
  name: 'FileCenter',
  components: {
    MarkdownMessage
  },
  data() {
    return {
      files: [],
      activeType: 'all',
      searchText: '',
      sortMode: 'modified-desc',
      loading: false,
      error: '',
      copiedKey: '',
      selectedFile: null,
      previewContent: '',
      previewLoading: false,
      previewError: ''
    };
  },
  computed: {
    selectedFileKey() {
      return this.selectedFile ? this.selectedFile.type + this.selectedFile.name : '';
    },
    visibleFiles() {
      const keyword = this.searchText.toLowerCase();
      const filtered = this.files.filter((file) => {
        const typeMatched = this.activeType === 'all' || file.type === this.activeType;
        const keywordMatched = !keyword || file.name.toLowerCase().includes(keyword);
        return typeMatched && keywordMatched;
      });
      return [...filtered].sort((a, b) => {
        if (this.sortMode === 'modified-asc') {
          return new Date(a.modifiedAt) - new Date(b.modifiedAt);
        }
        if (this.sortMode === 'name-asc') {
          return a.name.localeCompare(b.name);
        }
        if (this.sortMode === 'size-desc') {
          return b.size - a.size;
        }
        return new Date(b.modifiedAt) - new Date(a.modifiedAt);
      });
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
    inlineUrl(file) {
      const separator = file.downloadUrl.includes('?') ? '&' : '?';
      return apiUrl(`${file.downloadUrl}${separator}inline=true`);
    },
    async previewMarkdown(file) {
      this.selectedFile = file;
      this.previewContent = '';
      this.previewError = '';
      this.previewLoading = true;
      try {
        const response = await fetch(this.inlineUrl(file));
        if (!response.ok) {
          throw new Error(`HTTP ${response.status}`);
        }
        this.previewContent = await response.text();
      } catch (error) {
        this.previewError = `预览读取失败：${error.message || error}`;
      } finally {
        this.previewLoading = false;
      }
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
  max-width: 1280px;
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
.notice a,
.preview-head a {
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

.toolbar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 220px;
  gap: 12px;
  margin-bottom: 16px;
}

.toolbar label {
  display: grid;
  gap: 7px;
  color: #52655b;
  font-weight: 800;
}

.toolbar input,
.toolbar select {
  width: 100%;
  min-height: 42px;
  border: 1px solid #c7d4ce;
  border-radius: 6px;
  background: #ffffff;
  color: #14231d;
  padding: 0 12px;
  font: inherit;
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

.notice.error,
.preview-empty.error {
  border-color: #efb7b7;
  color: #9f1d1d;
}

.workspace {
  display: grid;
  grid-template-columns: minmax(0, 0.9fr) minmax(360px, 0.8fr);
  gap: 16px;
  align-items: start;
}

.file-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
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

.file-card.selected {
  border-color: #1f6f53;
  box-shadow: 0 0 0 3px rgba(31, 111, 83, 0.12);
}

.file-top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.file-top span,
.preview-head span {
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

.file-actions a,
.file-actions button:first-child,
.preview-head a {
  border: 0;
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

.preview-panel {
  position: sticky;
  top: 18px;
  max-height: calc(100vh - 36px);
  overflow: auto;
  border: 1px solid #d4dfd9;
  border-radius: 8px;
  background: #ffffff;
  padding: 18px;
  box-shadow: 0 12px 34px rgba(16, 35, 29, 0.08);
}

.preview-head {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: flex-start;
  border-bottom: 1px solid #e2eae5;
  margin-bottom: 16px;
  padding-bottom: 14px;
}

.preview-head h2 {
  margin: 10px 0 6px;
  font-size: 20px;
  word-break: break-word;
}

.preview-head p {
  margin: 0;
  color: #65756e;
  line-height: 1.5;
}

.preview-empty {
  border: 1px dashed #c7d4ce;
  border-radius: 8px;
  padding: 24px;
  color: #65756e;
  line-height: 1.7;
}

.preview-empty h2 {
  margin: 0 0 10px;
  color: #14231d;
}

.preview-empty p {
  margin: 0;
}

@media (max-width: 1080px) {
  .workspace {
    grid-template-columns: 1fr;
  }

  .preview-panel {
    position: static;
    max-height: none;
  }
}

@media (max-width: 760px) {
  .summary-row,
  .toolbar,
  .file-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 620px) {
  .file-page {
    padding: 20px;
  }

  .page-head {
    display: grid;
  }
}
</style>
