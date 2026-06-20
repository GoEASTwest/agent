<template>
  <div class="markdown-message" v-html="html"></div>
</template>

<script>
import DOMPurify from 'dompurify';
import { marked } from 'marked';

marked.setOptions({
  breaks: true,
  gfm: true
});

export default {
  name: 'MarkdownMessage',
  props: {
    content: {
      type: String,
      default: ''
    }
  },
  computed: {
    html() {
      return DOMPurify.sanitize(marked.parse(this.content || ''));
    }
  }
};
</script>

<style scoped>
.markdown-message {
  line-height: 1.75;
  word-break: break-word;
}

.markdown-message :deep(h1),
.markdown-message :deep(h2),
.markdown-message :deep(h3),
.markdown-message :deep(h4) {
  margin: 14px 0 8px;
  line-height: 1.35;
}

.markdown-message :deep(h1) {
  font-size: 22px;
}

.markdown-message :deep(h2) {
  font-size: 19px;
}

.markdown-message :deep(h3) {
  font-size: 17px;
}

.markdown-message :deep(p) {
  margin: 8px 0;
}

.markdown-message :deep(ul),
.markdown-message :deep(ol) {
  margin: 8px 0;
  padding-left: 22px;
}

.markdown-message :deep(li) {
  margin: 4px 0;
}

.markdown-message :deep(table) {
  width: 100%;
  margin: 12px 0;
  border-collapse: collapse;
  font-size: 14px;
}

.markdown-message :deep(th),
.markdown-message :deep(td) {
  border: 1px solid #cddbd3;
  padding: 8px 10px;
  text-align: left;
  vertical-align: top;
}

.markdown-message :deep(th) {
  background: #eef5f0;
  font-weight: 800;
}

.markdown-message :deep(code) {
  padding: 2px 5px;
  border-radius: 4px;
  background: #eef2f0;
  font-family: Consolas, Monaco, monospace;
  font-size: 0.92em;
}

.markdown-message :deep(pre) {
  max-width: 100%;
  overflow-x: auto;
  margin: 12px 0;
  padding: 12px;
  border-radius: 6px;
  background: #10231d;
  color: #f6fbf8;
}

.markdown-message :deep(pre code) {
  padding: 0;
  background: transparent;
  color: inherit;
}

.markdown-message :deep(blockquote) {
  margin: 10px 0;
  padding: 6px 12px;
  border-left: 3px solid #2f8a61;
  background: #f2f7f4;
}
</style>
