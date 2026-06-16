<template>
  <main class="home-shell">
    <section class="hero">
      <div class="hero-copy">
        <p class="eyebrow">Software Cup A1 Prototype</p>
        <h1>多模态设备检修知识检索与作业系统</h1>
        <p class="summary">
          面向巡检、故障诊断和检修复盘，把设备图片、现象描述、知识库条目和作业步骤汇聚到一个智能检修工作台。
        </p>
        <div class="actions">
          <router-link to="/love-app" class="primary-action">进入检修工作台</router-link>
          <router-link to="/manus" class="secondary-action">打开工具智能体</router-link>
        </div>
      </div>

      <div class="status-panel" aria-label="系统能力概览">
        <div class="panel-head">
          <span>实时检修链路</span>
          <strong>Ready</strong>
        </div>
        <div class="pipeline">
          <div v-for="item in pipeline" :key="item.title" class="pipeline-step">
            <span class="dot"></span>
            <div>
              <strong>{{ item.title }}</strong>
              <p>{{ item.desc }}</p>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="modules">
      <article v-for="module in modules" :key="module.title" class="module-card">
        <span>{{ module.tag }}</span>
        <h2>{{ module.title }}</h2>
        <p>{{ module.desc }}</p>
      </article>
    </section>
  </main>
</template>

<script>
export default {
  name: 'Home',
  data() {
    return {
      pipeline: [
        { title: '现场输入', desc: '文本描述、设备照片、巡检参数统一进入会话' },
        { title: '知识召回', desc: 'RAG 检索检修规程、故障案例和作业票模板' },
        { title: '作业闭环', desc: '生成诊断结论、风险等级、备件建议和检修报告' }
      ],
      modules: [
        {
          tag: 'RAG',
          title: '检修知识问答',
          desc: '围绕轴承、电机、泵、风机等设备构建知识库，让回答带出处、带步骤、可复核。'
        },
        {
          tag: 'Vision',
          title: '故障图像辅助识别',
          desc: '预留图片上传和案例图库入口，可接入 Qwen-VL、CLIP 或本地故障图像检索。'
        },
        {
          tag: 'Workflow',
          title: '作业单生成',
          desc: '把问答结果转成检修任务、风险提示、工具备件清单和验收要点，适合现场演示。'
        }
      ]
    };
  }
};
</script>

<style scoped>
.home-shell {
  min-height: 100vh;
  background:
    linear-gradient(135deg, rgba(8, 24, 31, 0.92), rgba(17, 47, 52, 0.84)),
    repeating-linear-gradient(90deg, rgba(255, 255, 255, 0.04) 0 1px, transparent 1px 72px);
  color: #f6fbf8;
  padding: 48px clamp(20px, 5vw, 72px);
}

.hero {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(320px, 0.7fr);
  gap: 40px;
  align-items: center;
  min-height: 58vh;
}

.eyebrow {
  color: #72d3a3;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0;
  text-transform: uppercase;
}

h1 {
  max-width: 860px;
  margin: 14px 0 18px;
  font-size: clamp(42px, 6vw, 76px);
  line-height: 1.02;
  letter-spacing: 0;
}

.summary {
  max-width: 660px;
  color: #d7e3dc;
  font-size: 18px;
  line-height: 1.8;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-top: 34px;
}

.primary-action,
.secondary-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 46px;
  padding: 0 18px;
  border-radius: 6px;
  text-decoration: none;
  font-weight: 700;
}

.primary-action {
  background: #72d3a3;
  color: #092018;
}

.secondary-action {
  border: 1px solid rgba(246, 251, 248, 0.36);
  color: #f6fbf8;
}

.status-panel {
  border: 1px solid rgba(246, 251, 248, 0.18);
  border-radius: 8px;
  background: rgba(246, 251, 248, 0.08);
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.24);
  backdrop-filter: blur(18px);
  padding: 24px;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 18px;
  border-bottom: 1px solid rgba(246, 251, 248, 0.18);
}

.panel-head strong {
  color: #72d3a3;
}

.pipeline {
  display: grid;
  gap: 22px;
  margin-top: 22px;
}

.pipeline-step {
  display: grid;
  grid-template-columns: 18px 1fr;
  gap: 14px;
}

.dot {
  width: 10px;
  height: 10px;
  margin-top: 7px;
  border-radius: 50%;
  background: #e6b85c;
  box-shadow: 0 0 0 6px rgba(230, 184, 92, 0.14);
}

.pipeline-step strong {
  display: block;
  margin-bottom: 5px;
}

.pipeline-step p {
  margin: 0;
  color: #c5d4cd;
  line-height: 1.6;
}

.modules {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
  margin-top: 36px;
}

.module-card {
  min-height: 184px;
  border: 1px solid rgba(246, 251, 248, 0.16);
  border-radius: 8px;
  background: rgba(246, 251, 248, 0.07);
  padding: 22px;
}

.module-card span {
  color: #e6b85c;
  font-size: 13px;
  font-weight: 800;
}

.module-card h2 {
  margin: 12px 0 10px;
  font-size: 21px;
}

.module-card p {
  color: #d7e3dc;
  line-height: 1.7;
  margin: 0;
}

@media (max-width: 880px) {
  .hero,
  .modules {
    grid-template-columns: 1fr;
  }
}
</style>
