<template>
  <main class="workspace">
    <aside class="sidebar">
      <router-link to="/" class="back">← A1 项目首页</router-link>
      <h1>设备检修中台</h1>
      <p>知识检索、故障诊断、图片特征、作业单和报告归档集中在一个流程里。</p>

      <nav>
        <button
          v-for="tab in tabs"
          :key="tab.key"
          :class="{ active: activeTab === tab.key }"
          @click="activeTab = tab.key"
        >
          <span>{{ tab.index }}</span>{{ tab.label }}
        </button>
      </nav>
    </aside>

    <section class="content">
      <header class="topbar">
        <div>
          <small>{{ backendOnline ? '后端接口在线' : '演示数据模式' }}</small>
          <h2>{{ currentTitle }}</h2>
        </div>
        <div class="topbar-actions">
          <button class="ghost-button" @click="loadAll">刷新数据</button>
          <button class="danger-button" @click="resetDemoData">重置演示数据</button>
        </div>
      </header>

      <section v-if="activeTab === 'dashboard'" class="dashboard">
        <div class="delivery-strip">
          <article v-for="item in completionItems.slice(0, 4)" :key="item.module">
            <span>{{ item.status }}</span>
            <strong>{{ item.module }}</strong>
            <div class="progress"><i :style="{ width: item.percent + '%' }"></i></div>
            <small>{{ item.percent }}% · {{ item.result }}</small>
          </article>
        </div>

        <div class="metric-grid">
          <article v-for="metric in metrics" :key="metric.label" class="metric-card">
            <span>{{ metric.label }}</span>
            <strong>{{ metric.value }}</strong>
            <p>{{ metric.desc }}</p>
          </article>
        </div>

        <div class="two-column">
          <article class="panel">
            <h3>风险分布</h3>
            <div class="risk-row" v-for="risk in riskRows" :key="risk.name">
              <span>{{ risk.name }}</span>
              <div class="bar"><i :style="{ width: risk.width }"></i></div>
              <strong>{{ risk.count }}</strong>
            </div>
          </article>

          <article class="panel">
            <h3>最近作业单</h3>
            <div v-for="task in tasks.slice(0, 4)" :key="task.id" class="task-mini">
              <strong>{{ task.title }}</strong>
              <span>{{ task.priority }} · {{ task.status }}</span>
            </div>
          </article>
        </div>
      </section>

      <section v-if="activeTab === 'devices'" class="cards-grid">
        <form class="quick-form wide-card" @submit.prevent="createDevice">
          <h3>新建设备</h3>
          <label>
            名称
            <input v-model="deviceForm.name" placeholder="例如：循环水泵三号" />
          </label>
          <label>
            类型
            <select v-model="deviceForm.type">
              <option>风机</option>
              <option>泵</option>
              <option>电机</option>
              <option>齿轮箱</option>
              <option>其他</option>
            </select>
          </label>
          <label>
            位置
            <input v-model="deviceForm.location" placeholder="例如：动力站 C 区" />
          </label>
          <label>
            风险
            <select v-model="deviceForm.riskLevel">
              <option>正常</option>
              <option>关注</option>
              <option>预警</option>
              <option>严重</option>
            </select>
          </label>
          <label class="wide">
            监测项
            <input v-model="deviceForm.sensorsText" placeholder="温度、振动、电流" />
          </label>
          <button class="primary-button" type="submit">添加设备</button>
        </form>
        <article v-for="device in devices" :key="device.id" class="device-card">
          <div class="card-head">
            <span>{{ device.type }}</span>
            <strong :class="['risk', riskClass(device.riskLevel)]">{{ device.riskLevel }}</strong>
          </div>
          <h3>{{ device.name }}</h3>
          <p>{{ device.location }}</p>
          <div class="chips">
            <i v-for="sensor in device.sensors" :key="sensor">{{ sensor }}</i>
          </div>
          <small>最近巡检：{{ device.lastInspectionTime }}</small>
        </article>
      </section>

      <section v-if="activeTab === 'roles'" class="management-grid">
        <article v-for="role in roles" :key="role.id" class="management-card">
          <div class="card-head">
            <span>{{ role.id }}</span>
            <strong>{{ role.name }}</strong>
          </div>
          <p>{{ role.scene }}</p>
          <div class="chips">
            <i v-for="permission in role.permissions" :key="permission">{{ permission }}</i>
          </div>
        </article>
      </section>

      <section v-if="activeTab === 'modules'" class="management-grid">
        <article v-for="module in modules" :key="module.id" class="management-card">
          <div class="card-head">
            <span>{{ module.status }}</span>
            <strong>{{ module.maturity }}%</strong>
          </div>
          <h3>{{ module.name }}</h3>
          <p>负责人角色：{{ module.ownerRole }}</p>
          <div class="progress"><i :style="{ width: module.maturity + '%' }"></i></div>
          <strong>功能</strong>
          <div class="chips">
            <i v-for="item in module.functions" :key="item">{{ item }}</i>
          </div>
          <strong>依赖</strong>
          <p>{{ module.dependencies.join(' / ') }}</p>
        </article>
      </section>

      <section v-if="activeTab === 'cases'" class="cases">
        <article v-for="item in cases" :key="item.id" class="case-card">
          <div>
            <span>{{ item.deviceType }}</span>
            <h3>{{ item.faultName }}</h3>
            <p>{{ item.cause }}</p>
          </div>
          <div class="case-detail">
            <strong>症状</strong>
            <p>{{ item.symptoms.join(' / ') }}</p>
            <strong>图像特征</strong>
            <p>{{ item.imageFeatures.join(' / ') }}</p>
          </div>
        </article>
      </section>

      <section v-if="activeTab === 'diagnose'" class="diagnose">
        <form class="diagnose-form" @submit.prevent="runDiagnosis">
          <label>
            设备
            <select v-model="inspection.deviceId">
              <option v-for="device in devices" :key="device.id" :value="device.id">
                {{ device.name }}
              </option>
            </select>
          </label>
          <label>
            类型
            <select v-model="inspection.deviceType">
              <option>风机</option>
              <option>泵</option>
              <option>电机</option>
              <option>齿轮箱</option>
            </select>
          </label>
          <label>
            温度 ℃
            <input v-model.number="inspection.temperature" type="number" min="0" />
          </label>
          <label>
            振动 RMS mm/s
            <input v-model.number="inspection.vibration" type="number" step="0.1" min="0" />
          </label>
          <label>
            电流 A
            <input v-model.number="inspection.current" type="number" min="0" />
          </label>
          <label class="wide">
            现场描述
            <textarea v-model="inspection.description"></textarea>
          </label>
          <label class="wide">
            图片可见特征
            <div class="feature-picker">
              <button
                v-for="feature in featureOptions"
                :key="feature"
                type="button"
                :class="{ selected: inspection.imageFeatures.includes(feature) }"
                @click="toggleFeature(feature)"
              >
                {{ feature }}
              </button>
            </div>
          </label>
          <button class="primary-button" type="submit">生成诊断与作业单</button>
        </form>

        <article v-if="diagnosis" class="diagnosis-result">
          <div class="result-head">
            <span>风险等级</span>
            <strong :class="['risk', riskClass(diagnosis.riskLevel)]">{{ diagnosis.riskLevel }}</strong>
            <em>{{ diagnosis.score }} 分</em>
          </div>
          <h3>证据链</h3>
          <ul>
            <li v-for="item in diagnosis.evidence" :key="item">{{ item }}</li>
          </ul>
          <h3>可能原因</h3>
          <ul>
            <li v-for="item in diagnosis.possibleCauses" :key="item">{{ item }}</li>
          </ul>
          <h3>作业建议</h3>
          <ul>
            <li v-for="item in diagnosis.recommendedActions" :key="item">{{ item }}</li>
          </ul>
          <button class="ghost-button" @click="downloadReport">导出报告</button>
        </article>
      </section>

      <section v-if="activeTab === 'image'" class="diagnose">
        <form class="diagnose-form" @submit.prevent="runImageAnalysis">
          <label>
            设备类型
            <select v-model="imageForm.deviceType">
              <option>风机</option>
              <option>泵</option>
              <option>电机</option>
              <option>齿轮箱</option>
            </select>
          </label>
          <label>
            文件名
            <input v-model="imageForm.fileName" placeholder="motor-terminal-burn.jpg" />
          </label>
          <label>
            上传图片
            <input type="file" accept="image/*" @change="onVisionFileChange" />
          </label>
          <label class="wide">
            图片描述
            <textarea v-model="imageForm.visualDescription" placeholder="描述图片中看到的焦痕、漏油、锈蚀、裂纹等特征"></textarea>
          </label>
          <label class="wide">
            Qwen 视觉图片 URL
            <input v-model="visionForm.imageUrl" placeholder="https://example.com/fault-image.jpg" />
          </label>
          <label class="wide">
            视觉问题
            <textarea v-model="visionForm.question" placeholder="请判断可见缺陷、风险等级和检修建议"></textarea>
          </label>
          <button class="primary-button" type="submit">分析图片特征</button>
          <button class="ghost-button" type="button" @click="runUploadedImageAnalysis" :disabled="!visionUploadFile">
            上传图片并分析
          </button>
          <button class="ghost-button" type="button" @click="runVisionAnalysis">调用 Qwen 视觉分析</button>
        </form>

        <article v-if="imageAnalysis" class="diagnosis-result">
          <div class="result-head">
            <span>图片分析</span>
            <strong>{{ imageAnalysis.fileName }}</strong>
          </div>
          <h3>识别特征</h3>
          <div class="chips">
            <i v-for="feature in imageAnalysis.detectedFeatures" :key="feature">{{ feature }}</i>
          </div>
          <h3>相似案例</h3>
          <ul>
            <li v-for="item in imageAnalysis.similarCases" :key="item.id">
              {{ item.faultName }}：{{ item.solution }}
            </li>
          </ul>
          <h3>采集建议</h3>
          <ul>
            <li v-for="tip in imageAnalysis.inspectionTips" :key="tip">{{ tip }}</li>
          </ul>
        </article>

        <article v-if="visionAnalysis" class="diagnosis-result">
          <div class="result-head">
            <span>{{ visionAnalysis.provider }}</span>
            <strong>{{ visionAnalysis.model }}</strong>
            <em>{{ visionAnalysis.riskLevel }}</em>
          </div>
          <h3>视觉结论</h3>
          <p>{{ visionAnalysis.conclusion }}</p>
          <h3>识别特征</h3>
          <div class="chips">
            <i v-for="feature in visionAnalysis.detectedFeatures" :key="feature">{{ feature }}</i>
          </div>
          <h3>建议动作</h3>
          <ul>
            <li v-for="action in visionAnalysis.recommendedActions" :key="action">{{ action }}</li>
          </ul>
          <div class="report-actions">
            <button class="ghost-button" type="button" @click="applyVisionToInspection">同步到诊断表单</button>
            <button class="primary-button" type="button" @click="diagnoseFromVision">生成诊断与作业单</button>
          </div>
        </article>
      </section>

      <section v-if="activeTab === 'tasks'" class="tasks">
        <form class="quick-form" @submit.prevent="createTask">
          <h3>新建检修任务</h3>
          <label>
            设备
            <select v-model="taskForm.deviceId">
              <option v-for="device in devices" :key="device.id" :value="device.id">
                {{ device.name }}（{{ device.id }}）
              </option>
            </select>
          </label>
          <label>
            标题
            <input v-model="taskForm.title" placeholder="例如：循环水泵入口压力波动复检" />
          </label>
          <label>
            优先级
            <select v-model="taskForm.priority">
              <option>P1</option>
              <option>P2</option>
              <option>P3</option>
              <option>P4</option>
            </select>
          </label>
          <label>
            状态
            <select v-model="taskForm.status">
              <option>待派工</option>
              <option>处理中</option>
              <option>待验收</option>
              <option>专家复核</option>
              <option>已归档</option>
            </select>
          </label>
          <label class="wide">
            作业步骤
            <textarea v-model="taskForm.stepsText" placeholder="每行一条，例如：&#10;断电挂牌上锁&#10;复测振动和温度"></textarea>
          </label>
          <label class="wide">
            备件工具
            <input v-model="taskForm.sparePartsText" placeholder="红外测温仪、振动采集仪、挂牌锁具" />
          </label>
          <label class="wide">
            验收标准
            <textarea v-model="taskForm.acceptanceText" placeholder="每行一条，例如：&#10;异常参数回落&#10;试运行 30 分钟无异常"></textarea>
          </label>
          <button class="primary-button" type="submit">创建任务</button>
        </form>
        <article v-for="task in tasks" :key="task.id" class="task-card">
          <div class="card-head">
            <span>{{ task.id }}</span>
            <strong>{{ task.priority }} · {{ task.status }}</strong>
          </div>
          <h3>{{ task.title }}</h3>
          <p>设备编号：{{ task.deviceId }}</p>
          <div class="task-columns">
            <div>
              <strong>步骤</strong>
              <ol>
                <li v-for="step in task.steps" :key="step">{{ step }}</li>
              </ol>
            </div>
            <div>
              <strong>备件工具</strong>
              <p>{{ task.spareParts.join(' / ') }}</p>
              <strong>验收</strong>
              <p>{{ task.acceptanceCriteria.join(' / ') }}</p>
            </div>
          </div>
          <div class="task-actions">
            <button
              v-for="flow in taskFlows"
              :key="flow.status"
              @click="updateTaskFlow(task.id, flow)"
            >
              {{ flow.status }}
            </button>
            <button @click="archiveTask(task)">归档作业单</button>
          </div>
          <div v-if="taskArchives[task.id]" class="task-archive">
            <span>已生成归档文件</span>
            <a
              v-if="taskArchives[task.id].markdownDownloadUrl"
              :href="apiDownloadUrl(taskArchives[task.id].markdownDownloadUrl)"
              target="_blank"
              rel="noopener"
            >
              Markdown
            </a>
            <a
              v-if="taskArchives[task.id].pdfDownloadUrl"
              :href="apiInlineUrl(taskArchives[task.id].pdfDownloadUrl)"
              target="_blank"
              rel="noopener"
            >
              PDF
            </a>
            <router-link to="/files">文件中心</router-link>
          </div>
        </article>
      </section>

      <section v-if="activeTab === 'workflow'" class="workflow">
        <article class="panel">
          <h3>作业状态机</h3>
          <div class="flow-line">
            <span v-for="status in workflowStatuses" :key="status">{{ status }}</span>
          </div>
        </article>
        <article class="task-card" v-for="event in taskFlowEvents" :key="event.taskId + event.operatedAt">
          <div class="card-head">
            <span>{{ event.operatorRole }}</span>
            <strong>{{ event.fromStatus }} → {{ event.toStatus }}</strong>
          </div>
          <h3>{{ event.taskId }}</h3>
          <p>{{ event.note }}</p>
          <small>{{ formatTime(event.operatedAt) }}</small>
        </article>
      </section>

      <section v-if="activeTab === 'completion'" class="completion-list">
        <article v-for="item in completionItems" :key="item.module" class="completion-card">
          <div class="card-head">
            <span>{{ item.status }}</span>
            <strong>{{ item.percent }}%</strong>
          </div>
          <h3>{{ item.module }}</h3>
          <div class="progress"><i :style="{ width: item.percent + '%' }"></i></div>
          <p>{{ item.result }}</p>
          <small>下一步：{{ item.nextStep }}</small>
        </article>
      </section>

      <section v-if="activeTab === 'knowledge'" class="knowledge-workbench">
        <form class="quick-form" @submit.prevent="submitKnowledge">
          <h3>提交检修经验</h3>
          <label>
            标题
            <input v-model="knowledgeForm.title" placeholder="例如：主电机端子过热处理经验" />
          </label>
          <label>
            设备类型
            <select v-model="knowledgeForm.deviceType">
              <option>风机</option>
              <option>泵</option>
              <option>电机</option>
              <option>齿轮箱</option>
              <option>其他</option>
            </select>
          </label>
          <label>
            故障名称
            <input v-model="knowledgeForm.faultName" placeholder="例如：端子松动过热" />
          </label>
          <label>
            提交人
            <input v-model="knowledgeForm.submitter" placeholder="例如：巡检员李工" />
          </label>
          <label class="wide">
            典型症状
            <input v-model="knowledgeForm.symptomsText" placeholder="焦味、外壳高温、电流异常" />
          </label>
          <label class="wide">
            图片特征
            <input v-model="knowledgeForm.imageFeaturesText" placeholder="焦痕、变色、绝缘破损" />
          </label>
          <label class="wide">
            原因分析
            <textarea v-model="knowledgeForm.cause"></textarea>
          </label>
          <label class="wide">
            处理方案
            <textarea v-model="knowledgeForm.solution"></textarea>
          </label>
          <label class="wide">
            现场经验
            <textarea v-model="knowledgeForm.content"></textarea>
          </label>
          <button class="primary-button" type="submit">提交待审核</button>
        </form>

        <form class="quick-form knowledge-search" @submit.prevent="runKnowledgeSearch">
          <h3>知识检索与引用</h3>
          <label class="wide">
            检索问题
            <input v-model="knowledgeSearch.query" placeholder="例如：电机接线端子焦痕过热如何处理" />
          </label>
          <button class="primary-button" type="submit" :disabled="knowledgeSearch.loading">
            {{ knowledgeSearch.loading ? '检索中' : '检索知识库' }}
          </button>
          <span v-if="knowledgeSearch.result" class="search-summary">
            命中 {{ knowledgeSearch.result.matches.length }} 条 · 关键词 {{ knowledgeSearch.result.keywords.join(' / ') || '无' }}
          </span>
        </form>

        <div v-if="knowledgeSearch.result" class="knowledge-results">
          <article v-if="!knowledgeSearch.result.matches.length" class="empty-state">
            <h3>未命中知识片段</h3>
            <p>可以补充设备类型、故障现象、图片特征或关键参数后再次检索。</p>
          </article>
          <article v-for="match in knowledgeSearch.result.matches" :key="match.sourceType + match.sourceName + match.title" class="knowledge-card">
            <div class="card-head">
              <span>{{ match.sourceType }}</span>
              <strong>{{ match.score }} 分</strong>
            </div>
            <h3>{{ match.title }}</h3>
            <p>{{ match.sourceName }}</p>
            <p>{{ match.snippet }}</p>
          </article>
        </div>

        <div class="knowledge-review-list">
          <article v-for="item in knowledgeContributions" :key="item.id" class="knowledge-card">
            <div class="card-head">
              <span>{{ item.status }}</span>
              <strong>{{ item.id }}</strong>
            </div>
            <h3>{{ item.title }}</h3>
            <p>{{ item.deviceType }} · {{ item.faultName }}</p>
            <div class="chips">
              <i v-for="feature in item.imageFeatures" :key="feature">{{ feature }}</i>
            </div>
            <p>{{ item.reviewNote }}</p>
            <div class="report-actions">
              <button v-if="item.status === '待审核'" class="ghost-button" @click="reviewKnowledge(item, '已通过')">
                审核通过
              </button>
              <button v-if="item.status === '待审核'" class="ghost-button" @click="reviewKnowledge(item, '已驳回')">
                驳回
              </button>
              <a
                v-if="item.markdownDownloadUrl"
                class="ghost-link"
                :href="apiDownloadUrl(item.markdownDownloadUrl)"
                target="_blank"
                rel="noopener"
              >
                Markdown
              </a>
              <a
                v-if="item.pdfDownloadUrl"
                class="ghost-link"
                :href="apiInlineUrl(item.pdfDownloadUrl)"
                target="_blank"
                rel="noopener"
              >
                PDF
              </a>
            </div>
          </article>
        </div>

        <article v-for="doc in knowledgeDocs" :key="doc" class="knowledge-card">
          <span>Knowledge Source</span>
          <h3>{{ doc.split(' - ')[0] }}</h3>
          <p>{{ doc.split(' - ')[1] || '设备检修知识文档' }}</p>
        </article>
      </section>

      <section v-if="activeTab === 'reports'" class="reports">
        <article class="panel wide-card">
          <div class="card-head">
            <h3>持久化记录概览</h3>
            <button class="ghost-button" type="button" @click="loadPersistenceViews">刷新记录</button>
          </div>
          <div class="record-strip">
            <span>诊断历史 {{ inspectionRecords.length }}</span>
            <span>报告总数 {{ reportPage.total || reports.length }}</span>
            <span>修正记录 {{ correctionRecords.length }}</span>
          </div>
        </article>

        <form class="quick-form wide-card" @submit.prevent="submitReportCorrection">
          <h3>专家修正 AI 输出</h3>
          <label>
            原报告编号
            <input v-model="correctionForm.reportId" placeholder="可填写 RPT 编号，也可留空" />
          </label>
          <label>
            修正风险
            <select v-model="correctionForm.correctedRiskLevel">
              <option>正常</option>
              <option>关注</option>
              <option>预警</option>
              <option>严重</option>
            </select>
          </label>
          <label>
            复核人
            <input v-model="correctionForm.reviewer" placeholder="专家姓名" />
          </label>
          <label class="wide">
            修正证据
            <textarea v-model="correctionForm.evidenceText"></textarea>
          </label>
          <label class="wide">
            修正原因
            <textarea v-model="correctionForm.causesText"></textarea>
          </label>
          <label class="wide">
            修正措施
            <textarea v-model="correctionForm.actionsText"></textarea>
          </label>
          <label class="wide">
            复核说明
            <textarea v-model="correctionForm.reviewNote"></textarea>
          </label>
          <button class="primary-button" type="submit">归档修正记录</button>
        </form>

        <article class="report-card wide-card">
          <div class="card-head">
            <h3>诊断历史</h3>
            <span>最近 {{ inspectionRecords.length }} 条</span>
          </div>
          <div v-if="!inspectionRecords.length" class="empty-line">暂无诊断历史，执行一次多模态诊断后会自动写入。</div>
          <div v-for="record in inspectionRecords" :key="record.id" class="record-row">
            <div>
              <strong>{{ record.deviceId }}</strong>
              <p>{{ record.description || '无现场描述' }}</p>
            </div>
            <span :class="['risk', riskClass(record.riskLevel)]">{{ record.riskLevel }}</span>
            <small>{{ record.score }} 分 · {{ formatTime(record.createdAt) }}</small>
          </div>
        </article>

        <article class="panel wide-card">
          <div class="card-head">
            <h3>报告归档</h3>
            <div class="pager-actions">
              <button class="ghost-button" type="button" :disabled="reportPage.page <= 1" @click="changeReportPage(-1)">上一页</button>
              <span>第 {{ reportPage.page }} 页 / 共 {{ reportPage.total }} 条</span>
              <button class="ghost-button" type="button" :disabled="reports.length < reportPage.size" @click="changeReportPage(1)">下一页</button>
            </div>
          </div>
        </article>
        <article v-if="!reports.length" class="empty-state">
          <h3>暂无已归档报告</h3>
          <p>在“多模态诊断”里生成诊断结果并点击导出报告后，这里会沉淀报告记录。</p>
        </article>
        <article v-for="report in reports" :key="report.reportId" class="report-card">
          <div class="card-head">
            <span>{{ report.reportId }}</span>
            <strong :class="['risk', riskClass(report.riskLevel)]">{{ report.riskLevel }}</strong>
          </div>
          <h3>{{ report.title }}</h3>
          <p>{{ formatTime(report.generatedAt) }}</p>
          <div class="chips">
            <i v-for="section in report.sections" :key="section">{{ section }}</i>
          </div>
          <div class="report-actions">
            <a
              v-if="report.markdownDownloadUrl"
              class="ghost-link"
              :href="apiDownloadUrl(report.markdownDownloadUrl)"
              target="_blank"
              rel="noopener"
            >
              下载 Markdown
            </a>
            <a
              v-if="report.pdfDownloadUrl"
              class="ghost-link"
              :href="apiInlineUrl(report.pdfDownloadUrl)"
              target="_blank"
              rel="noopener"
            >
              打开 PDF
            </a>
            <button v-if="!report.markdownDownloadUrl" class="ghost-button" @click="downloadMarkdown(report)">
              下载报告
            </button>
            <router-link class="ghost-link" to="/files">文件中心</router-link>
          </div>
        </article>

        <article class="report-card wide-card">
          <div class="card-head">
            <h3>专家修正记录</h3>
            <span>最近 {{ correctionRecords.length }} 条</span>
          </div>
          <div v-if="!correctionRecords.length" class="empty-line">暂无修正记录，提交专家修正后会自动沉淀。</div>
          <div v-for="record in correctionRecords" :key="record.id" class="record-row">
            <div>
              <strong>{{ record.sourceReportId || '未关联报告' }} → {{ record.correctedReportId }}</strong>
              <p>{{ record.reviewNote }}</p>
            </div>
            <span :class="['risk', riskClass(record.correctedRiskLevel)]">{{ record.correctedRiskLevel }}</span>
            <small>{{ record.reviewer }} · {{ formatTime(record.createdAt) }}</small>
          </div>
        </article>
      </section>

      <section v-if="activeTab === 'chat'" class="chat-layout">
        <div class="chat-history" ref="chatHistory">
          <div class="message ai">
            <div class="bubble">我是检修知识助手，可以结合当前案例库和作业单，帮你生成诊断说明或答辩演示话术。</div>
          </div>
          <div v-for="(msg, index) in messages" :key="index" :class="['message', msg.role]">
            <div class="bubble">
              <MarkdownMessage v-if="msg.role === 'ai'" :content="msg.content" />
              <span v-else>{{ msg.content }}</span>
            </div>
          </div>
        </div>
        <form class="chat-input" @submit.prevent="sendMessage">
          <input v-model="inputMessage" placeholder="输入检修问题，例如：把刚才的诊断整理成答辩讲解" />
          <button type="submit" :disabled="isStreaming">{{ isStreaming ? '生成中' : '发送' }}</button>
        </form>
      </section>
    </section>
  </main>
</template>

<script>
import { apiUrl, fetchJson } from '../api';
import MarkdownMessage from '../components/MarkdownMessage.vue';

const fallbackDevices = [
  {
    id: 'DEV-FAN-01',
    name: '一号引风机',
    type: '风机',
    location: '锅炉房 A 区',
    status: '运行',
    riskLevel: '关注',
    sensors: ['振动', '温度', '电流', '噪声'],
    lastInspectionTime: '2026-06-10 09:20'
  },
  {
    id: 'DEV-PUMP-02',
    name: '循环水泵二号',
    type: '泵',
    location: '动力站 B 区',
    status: '运行',
    riskLevel: '预警',
    sensors: ['入口压力', '出口压力', '振动', '温度'],
    lastInspectionTime: '2026-06-10 15:42'
  },
  {
    id: 'DEV-MOTOR-03',
    name: '输送线主电机',
    type: '电机',
    location: '产线 3 号位',
    status: '待检修',
    riskLevel: '严重',
    sensors: ['电流', '绝缘电阻', '温度', '外观图片'],
    lastInspectionTime: '2026-06-11 08:10'
  }
];

const fallbackCases = [
  {
    id: 'CASE-001',
    deviceType: '风机',
    faultName: '轴承早期磨损',
    symptoms: ['温度升高', '周期性异响', '振动增大'],
    imageFeatures: ['油污', '轴承座发热', '轻微磨痕'],
    cause: '润滑不足或轴承游隙异常导致滚动体局部磨损',
    solution: '补充润滑，采集频谱，检查轴承游隙，必要时计划停机更换',
    severity: 3
  },
  {
    id: 'CASE-002',
    deviceType: '泵',
    faultName: '汽蚀与入口堵塞',
    symptoms: ['压力波动', '流量下降', '泵体振动'],
    imageFeatures: ['入口滤网污堵', '管路锈蚀', '密封处漏液'],
    cause: '入口阻力过大或液位不足导致汽蚀',
    solution: '检查入口阀门、滤网和液位，排气后复测振动与压力',
    severity: 4
  },
  {
    id: 'CASE-003',
    deviceType: '电机',
    faultName: '绕组过热与绝缘下降',
    symptoms: ['外壳高温', '电流异常', '焦味'],
    imageFeatures: ['焦痕', '变色', '绝缘破损'],
    cause: '长期过载、散热不良或绝缘老化引起绕组局部过热',
    solution: '立即停机断电，测绝缘电阻，检查接线端子和散热通道',
    severity: 5
  }
];

const fallbackKnowledgeContributions = [
  {
    id: 'KC-1001',
    title: '主电机端子过热经验',
    deviceType: '电机',
    faultName: '绕组过热与绝缘下降',
    symptoms: ['外壳高温', '电流异常', '焦味'],
    imageFeatures: ['焦痕', '变色', '绝缘破损'],
    cause: '端子松动、散热不良或绝缘老化会导致局部发热并产生焦味',
    solution: '停机断电后复紧端子，测量绝缘电阻，清理散热通道并做空载试运行',
    content: '端子发黑时不要只更换胶带，应同步检查压接力矩、三相电流平衡和端子排温升。',
    submitter: '巡检员张工',
    status: '已通过',
    reviewNote: '专家复核通过，已纳入案例库',
    createdAt: '2026-06-20T09:20:00',
    reviewedAt: '2026-06-20T10:30:00',
    markdownDownloadUrl: '',
    pdfDownloadUrl: ''
  }
];

const fallbackRoles = [
  {
    id: 'inspector',
    name: '巡检员',
    scene: '现场采集异常、上传图片和填写巡检参数',
    permissions: ['查看设备台账', '提交诊断', '上传图片', '查看本人作业单']
  },
  {
    id: 'maintainer',
    name: '检修员',
    scene: '接收作业单并执行安全隔离、拆检和复测',
    permissions: ['查看作业单', '更新处理中', '提交待验收', '补充检修记录']
  },
  {
    id: 'expert',
    name: '专家',
    scene: '复核 AI 结论、确认风险等级和修正案例知识',
    permissions: ['专家复核', '修正诊断', '沉淀案例', '批准归档']
  },
  {
    id: 'admin',
    name: '管理员',
    scene: '维护用户、权限、模块开关和知识库',
    permissions: ['模块管理', '角色管理', '全部数据', '系统配置']
  }
];

const fallbackModules = [
  {
    id: 'dashboard',
    name: '态势看板',
    ownerRole: '管理员',
    status: '已启用',
    maturity: 95,
    functions: ['风险分布', '异常设备统计', '待处理作业', '最近作业'],
    dependencies: ['设备台账', '作业单']
  },
  {
    id: 'case',
    name: '案例知识库管理',
    ownerRole: '专家',
    status: '已启用',
    maturity: 86,
    functions: ['故障案例', '症状标签', '图片特征', '处理方案'],
    dependencies: ['RAG 文档', '案例匹配']
  },
  {
    id: 'diagnosis',
    name: '多模态诊断',
    ownerRole: '巡检员',
    status: '已启用',
    maturity: 90,
    functions: ['参数评分', '证据链', '相似案例', '自动作业单'],
    dependencies: ['设备台账', '案例库', '图片分析']
  },
  {
    id: 'workflow',
    name: '作业流转',
    ownerRole: '检修员',
    status: '已启用',
    maturity: 84,
    functions: ['待派工', '处理中', '待验收', '专家复核', '已归档'],
    dependencies: ['角色权限', '报告中心']
  },
  {
    id: 'report',
    name: '报告中心',
    ownerRole: '专家',
    status: '已启用',
    maturity: 82,
    functions: ['报告生成', 'Markdown 导出', '报告沉淀', '复盘材料'],
    dependencies: ['诊断结果', '作业单']
  }
];

export default {
  name: 'LoveApp',
  components: {
    MarkdownMessage
  },
  data() {
    return {
      tabs: [
        { key: 'dashboard', label: '态势看板', index: '01' },
        { key: 'devices', label: '设备台账', index: '02' },
        { key: 'roles', label: '角色权限', index: '03' },
        { key: 'modules', label: '模块管理', index: '04' },
        { key: 'cases', label: '案例知识库', index: '05' },
        { key: 'diagnose', label: '多模态诊断', index: '06' },
        { key: 'image', label: '视觉分析', index: '07' },
        { key: 'tasks', label: '检修作业单', index: '08' },
        { key: 'workflow', label: '作业流转', index: '09' },
        { key: 'knowledge', label: '知识库', index: '10' },
        { key: 'completion', label: '交付总览', index: '11' },
        { key: 'reports', label: '报告中心', index: '12' },
        { key: 'chat', label: 'AI 知识问答', index: '13' }
      ],
      activeTab: 'dashboard',
      backendOnline: false,
      dashboard: null,
      devices: fallbackDevices,
      roles: fallbackRoles,
      modules: fallbackModules,
      cases: fallbackCases,
      tasks: [],
      deviceForm: {
        name: '循环水泵三号',
        type: '泵',
        location: '动力站 C 区',
        riskLevel: '关注',
        sensorsText: '入口压力、出口压力、振动、温度'
      },
      taskForm: {
        deviceId: 'DEV-MOTOR-03',
        title: '主电机接线端子复检',
        priority: 'P2',
        status: '待派工',
        stepsText: '断电挂牌上锁\n复测温度和电流\n检查接线端子焦痕\n清理散热通道\n试运行验收',
        sparePartsText: '红外测温仪、万用表、绝缘手套、挂牌锁具',
        acceptanceText: '温度回落至关注阈值以下\n三相电流平衡\n无焦味和异常噪声\n检修记录归档'
      },
      taskFlowEvents: [],
      taskArchives: {},
      knowledgeDocs: [
        'paper1.md - 正常运行状态诊断知识',
        'paper2.md - 早期故障诊断知识',
        'paper3.md - 严重故障诊断知识',
        'maintenance_workflow.md - 检修作业闭环知识'
      ],
      knowledgeContributions: fallbackKnowledgeContributions,
      knowledgeForm: {
        title: '主电机端子过热处理经验',
        deviceType: '电机',
        faultName: '端子松动过热',
        submitter: '巡检员李工',
        symptomsText: '焦味、外壳高温、电流异常',
        imageFeaturesText: '焦痕、变色、绝缘破损',
        cause: '端子压接松动导致接触电阻增大，局部温升后引发绝缘老化。',
        solution: '停机断电，复紧端子并测量绝缘电阻，清理散热通道，试运行后复测三相电流。',
        content: '现场发现端子排发黑时，应同时检查压接力矩、端子温升和三相电流平衡，避免只做表面清理。'
      },
      knowledgeSearch: {
        query: '电机接线端子焦痕过热如何处理',
        loading: false,
        result: null
      },
      completionItems: [
        { module: '前端工作台', status: '已实现', percent: 95, result: '核心页面可演示', nextStep: '补充录屏素材' },
        { module: '检修业务后端', status: '已实现', percent: 90, result: '业务接口可调用', nextStep: '接入数据库' },
        { module: '大模型问答', status: '已实现', percent: 85, result: 'Qwen 接入可用', nextStep: '增加调用审计' },
        { module: '工程部署', status: '演示可用', percent: 70, result: '启动脚本与环境模板已整理', nextStep: '补充构建验证' }
      ],
      reports: [],
      reportPage: {
        page: 1,
        size: 10,
        total: 0
      },
      inspectionRecords: [],
      correctionRecords: [],
      correctionForm: {
        reportId: '',
        correctedRiskLevel: '预警',
        reviewer: '专家王工',
        evidenceText: '端子温升复测仍偏高\n三相电流存在轻微不平衡\n照片显示端子局部变色',
        causesText: '端子压接松动可能性较高\n散热通道积尘加重局部温升',
        actionsText: '复紧端子并做绝缘电阻测试\n清理散热风道\n试运行 30 分钟后复测温升',
        reviewNote: '专家修正后建议将风险从严重调整为预警，并纳入案例复盘。'
      },
      diagnosis: null,
      imageAnalysis: null,
      inspection: {
        deviceId: 'DEV-MOTOR-03',
        deviceType: '电机',
        description: '电机外壳温度高，有焦味，巡检照片可见接线端子焦痕。',
        temperature: 92,
        vibration: 5.6,
        current: 128,
        imageFeatures: ['焦痕', '变色']
      },
      imageForm: {
        deviceType: '电机',
        fileName: 'motor-terminal-burn.jpg',
        visualDescription: '图片中电机接线端子附近有焦痕和变色，绝缘层疑似破损。'
      },
      visionForm: {
        deviceType: '电机',
        imageUrl: '',
        question: '请判断图片中可见缺陷、风险等级和检修建议'
      },
      visionUploadFile: null,
      visionAnalysis: null,
      featureOptions: ['油污', '锈蚀', '焦痕', '裂纹', '磨损', '漏液', '变色', '金属屑', '绝缘破损'],
      taskFlows: [
        { status: '待派工', operatorRole: 'admin', note: '管理员确认派工' },
        { status: '处理中', operatorRole: 'maintainer', note: '检修员接单处理' },
        { status: '待验收', operatorRole: 'maintainer', note: '检修完成，提交验收' },
        { status: '专家复核', operatorRole: 'expert', note: '专家复核 AI 诊断和检修记录' },
        { status: '驳回整改', operatorRole: 'expert', note: '验收未通过，退回检修员整改' },
        { status: '已归档', operatorRole: 'expert', note: '验收通过并归档复盘' }
      ],
      workflowStatuses: ['待处理', '待派工', '处理中', '待验收', '专家复核', '驳回整改', '已归档'],
      messages: [],
      inputMessage: '',
      chatId: '',
      eventSource: null,
      isStreaming: false
    };
  },
  computed: {
    currentTitle() {
      return this.tabs.find((tab) => tab.key === this.activeTab)?.label || '检修工作台';
    },
    metrics() {
      const summary = this.dashboard || this.localDashboard();
      return [
        { label: '设备数', value: summary.deviceCount, desc: '纳入台账和巡检链路' },
        { label: '异常设备', value: summary.warningCount, desc: '关注、预警、严重状态' },
        { label: '待处理作业', value: summary.openTaskCount, desc: '诊断自动生成或人工创建' },
        { label: '知识案例', value: summary.knowledgeCount, desc: '故障案例与检修经验' }
      ];
    },
    riskRows() {
      const dist = (this.dashboard || this.localDashboard()).riskDistribution;
      const max = Math.max(...Object.values(dist), 1);
      return Object.entries(dist).map(([name, count]) => ({
        name,
        count,
        width: `${(count / max) * 100}%`
      }));
    }
  },
  mounted() {
    this.chatId = 'a1_' + Date.now();
    this.loadAll();
  },
  methods: {
    async loadAll() {
      try {
        const [dashboard, devices, roles, modules, cases, tasks, flows, completion, reports, contributions] = await Promise.all([
          this.fetchJson('/maintenance/dashboard'),
          this.fetchJson('/maintenance/devices'),
          this.fetchJson('/maintenance/roles'),
          this.fetchJson('/maintenance/modules'),
          this.fetchJson('/maintenance/cases'),
          this.fetchJson('/maintenance/tasks'),
          this.fetchJson('/maintenance/tasks/flow'),
          this.fetchJson('/maintenance/completion'),
          this.fetchJson('/maintenance/reports'),
          this.fetchJson('/maintenance/knowledge/contributions')
        ]);
        const knowledgeDocs = await this.fetchJson('/maintenance/knowledge');
        this.dashboard = dashboard;
        this.devices = devices;
        this.roles = roles;
        this.modules = modules;
        this.cases = cases;
        this.tasks = tasks;
        this.taskFlowEvents = flows;
        this.knowledgeDocs = knowledgeDocs;
        this.completionItems = completion;
        this.reports = reports;
        this.reportPage.total = reports.length;
        this.knowledgeContributions = contributions;
        await this.loadPersistenceViews();
        this.backendOnline = true;
        if (!this.knowledgeSearch.result) {
          await this.runKnowledgeSearch();
        }
      } catch (error) {
        this.backendOnline = false;
        this.dashboard = this.localDashboard();
        this.devices = fallbackDevices;
        this.roles = fallbackRoles;
        this.modules = fallbackModules;
        this.cases = fallbackCases;
        this.knowledgeContributions = this.knowledgeContributions.length ? this.knowledgeContributions : fallbackKnowledgeContributions;
        this.tasks = this.tasks.length ? this.tasks : [this.mockTask()];
        this.reportPage.total = this.reports.length;
        if (!this.knowledgeSearch.result) {
          this.knowledgeSearch.result = this.localKnowledgeSearch();
        }
      }
    },
    async fetchJson(path, options = {}) {
      return fetchJson(path, options);
    },
    async resetDemoData() {
      const confirmed = window.confirm('确定要重置演示数据吗？这会清空当前演示新增的设备、任务、报告和审核记录。');
      if (!confirmed) return;
      try {
        this.dashboard = await this.fetchJson('/maintenance/demo/reset', { method: 'POST' });
        this.imageAnalysis = null;
        this.visionAnalysis = null;
        this.diagnosis = null;
        this.taskArchives = {};
        await this.loadAll();
        this.backendOnline = true;
      } catch (error) {
        this.backendOnline = false;
        this.resetLocalDemoData();
      }
    },
    resetLocalDemoData() {
      this.dashboard = null;
      this.devices = fallbackDevices;
      this.roles = fallbackRoles;
      this.modules = fallbackModules;
      this.cases = fallbackCases;
      this.tasks = [this.mockTask()];
      this.taskFlowEvents = [];
      this.reports = [];
      this.inspectionRecords = [];
      this.correctionRecords = [];
      this.knowledgeContributions = fallbackKnowledgeContributions;
      this.imageAnalysis = null;
      this.visionAnalysis = null;
      this.diagnosis = null;
      this.taskArchives = {};
      this.reportPage = { page: 1, size: 10, total: 0 };
    },
    localDashboard() {
      const riskDistribution = { 正常: 0, 关注: 0, 预警: 0, 严重: 0 };
      this.devices.forEach((device) => {
        riskDistribution[device.riskLevel] = (riskDistribution[device.riskLevel] || 0) + 1;
      });
      return {
        deviceCount: this.devices.length,
        warningCount: this.devices.filter((device) => device.riskLevel !== '正常').length,
        openTaskCount: this.tasks.length || 1,
        knowledgeCount: this.cases.length,
        riskDistribution,
        recentTasks: this.tasks
      };
    },
    toggleFeature(feature) {
      const features = this.inspection.imageFeatures;
      const index = features.indexOf(feature);
      if (index >= 0) {
        features.splice(index, 1);
      } else {
        features.push(feature);
      }
    },
    async createDevice() {
      const payload = {
        name: this.deviceForm.name,
        type: this.deviceForm.type,
        location: this.deviceForm.location,
        status: '运行',
        riskLevel: this.deviceForm.riskLevel,
        sensors: this.parseListInput(this.deviceForm.sensorsText),
        lastInspectionTime: this.formatTime(new Date().toISOString())
      };
      try {
        const device = await this.fetchJson('/maintenance/devices', {
          method: 'POST',
          body: JSON.stringify(payload)
        });
        this.devices = [device, ...this.devices.filter((item) => item.id !== device.id)];
        this.taskForm.deviceId = device.id;
        this.inspection.deviceId = device.id;
        this.inspection.deviceType = device.type;
        this.backendOnline = true;
      } catch (error) {
        this.backendOnline = false;
        const device = {
          id: `DEV-DEMO-${Date.now().toString().slice(-4)}`,
          ...payload
        };
        this.devices = [device, ...this.devices];
        this.taskForm.deviceId = device.id;
      }
    },
    async createTask() {
      const payload = {
        deviceId: this.taskForm.deviceId,
        title: this.taskForm.title,
        priority: this.taskForm.priority,
        status: this.taskForm.status,
        steps: this.parseListInput(this.taskForm.stepsText),
        spareParts: this.parseListInput(this.taskForm.sparePartsText),
        acceptanceCriteria: this.parseListInput(this.taskForm.acceptanceText)
      };
      try {
        const task = await this.fetchJson('/maintenance/tasks', {
          method: 'POST',
          body: JSON.stringify(payload)
        });
        this.tasks = [task, ...this.tasks.filter((item) => item.id !== task.id)];
        this.backendOnline = true;
      } catch (error) {
        this.backendOnline = false;
        const task = {
          id: `TASK-DEMO-${Date.now().toString().slice(-4)}`,
          ...payload,
          createdAt: new Date().toISOString()
        };
        this.tasks = [task, ...this.tasks];
      }
    },
    parseListInput(value) {
      return String(value || '')
        .split(/[\n,，、]/)
        .map((item) => item.trim())
        .filter(Boolean);
    },
    async submitKnowledge() {
      const payload = {
        title: this.knowledgeForm.title,
        deviceType: this.knowledgeForm.deviceType,
        faultName: this.knowledgeForm.faultName,
        submitter: this.knowledgeForm.submitter,
        symptoms: this.parseListInput(this.knowledgeForm.symptomsText),
        imageFeatures: this.parseListInput(this.knowledgeForm.imageFeaturesText),
        cause: this.knowledgeForm.cause,
        solution: this.knowledgeForm.solution,
        content: this.knowledgeForm.content
      };
      try {
        const contribution = await this.fetchJson('/maintenance/knowledge/contributions', {
          method: 'POST',
          body: JSON.stringify(payload)
        });
        this.knowledgeContributions = [contribution, ...this.knowledgeContributions.filter((item) => item.id !== contribution.id)];
        this.backendOnline = true;
      } catch (error) {
        this.backendOnline = false;
        const contribution = {
          id: `KC-DEMO-${Date.now().toString().slice(-4)}`,
          ...payload,
          status: '待审核',
          reviewNote: '等待专家审核',
          createdAt: new Date().toISOString(),
          reviewedAt: null,
          markdownDownloadUrl: '',
          pdfDownloadUrl: ''
        };
        this.knowledgeContributions = [contribution, ...this.knowledgeContributions];
      }
    },
    async reviewKnowledge(item, status) {
      const payload = {
        status,
        reviewNote: status === '已通过' ? '专家审核通过，已纳入案例库' : '专家驳回，需补充现场证据'
      };
      try {
        const reviewed = await this.fetchJson(`/maintenance/knowledge/contributions/${item.id}/review`, {
          method: 'POST',
          body: JSON.stringify(payload)
        });
        this.knowledgeContributions = this.knowledgeContributions.map((contribution) => (
          contribution.id === reviewed.id ? reviewed : contribution
        ));
        this.cases = await this.fetchJson('/maintenance/cases');
        this.knowledgeDocs = await this.fetchJson('/maintenance/knowledge');
        this.knowledgeSearch.result = null;
        await this.runKnowledgeSearch();
        this.backendOnline = true;
      } catch (error) {
        this.backendOnline = false;
        const reviewed = {
          ...item,
          status,
          reviewNote: payload.reviewNote,
          reviewedAt: new Date().toISOString()
        };
        this.knowledgeContributions = this.knowledgeContributions.map((contribution) => (
          contribution.id === item.id ? reviewed : contribution
        ));
        if (status === '已通过' && !this.cases.some((faultCase) => faultCase.faultName === item.faultName)) {
          this.cases = [
            {
              id: `CASE-DEMO-${Date.now().toString().slice(-4)}`,
              deviceType: item.deviceType,
              faultName: item.faultName,
              symptoms: item.symptoms,
              imageFeatures: item.imageFeatures,
              cause: item.cause,
              solution: item.solution,
              severity: 3
            },
            ...this.cases
          ];
          this.knowledgeDocs = [`${item.id} - ${item.title}`, ...this.knowledgeDocs];
        }
        this.knowledgeSearch.result = this.localKnowledgeSearch();
      }
    },
    async runKnowledgeSearch() {
      const query = this.knowledgeSearch.query.trim();
      if (!query) return;
      this.knowledgeSearch.loading = true;
      try {
        this.knowledgeSearch.result = await this.fetchJson('/maintenance/knowledge/search', {
          method: 'POST',
          body: JSON.stringify({ query })
        });
        this.backendOnline = true;
      } catch (error) {
        this.backendOnline = false;
        this.knowledgeSearch.result = this.localKnowledgeSearch();
      } finally {
        this.knowledgeSearch.loading = false;
      }
    },
    localKnowledgeSearch() {
      const query = this.knowledgeSearch.query;
      const domainKeywords = ['风机', '泵', '电机', '轴承', '齿轮箱', '振动', '温度', '电流', '压力', '过热', '磨损', '裂纹', '漏油', '锈蚀', '焦痕', '变色', '绝缘', '作业单', '验收'];
      const keywords = Array.from(new Set([
        ...domainKeywords.filter((keyword) => query.includes(keyword)),
        ...this.parseListInput(query.replace(/\s+/g, '、'))
      ]));
      const sources = [
        ...this.cases.map((item) => ({
          sourceType: '故障案例',
          sourceName: item.id,
          title: item.faultName,
          snippet: `设备类型：${item.deviceType}。症状：${item.symptoms.join('、')}。图片特征：${item.imageFeatures.join('、')}。原因：${item.cause}。处理方案：${item.solution}`
        })),
        ...this.knowledgeContributions
          .filter((item) => item.status === '已通过')
          .map((item) => ({
            sourceType: '已审核经验',
            sourceName: item.id,
            title: item.title,
            snippet: `故障名称：${item.faultName}。症状：${item.symptoms.join('、')}。图片特征：${item.imageFeatures.join('、')}。原因：${item.cause}。处理方案：${item.solution}。现场经验：${item.content}`
          })),
        ...this.knowledgeDocs.map((doc) => ({
          sourceType: 'Markdown',
          sourceName: doc.split(' - ')[0],
          title: doc.split(' - ')[1] || doc,
          snippet: '本地 Markdown 检修资料，可在 AI 知识问答中作为引用来源。'
        }))
      ];
      const matches = sources
        .map((source) => {
          const text = `${source.title} ${source.snippet}`;
          const score = keywords.reduce((value, keyword) => value + (text.includes(keyword) ? 1 : 0), 0);
          return { ...source, score };
        })
        .filter((item) => item.score > 0)
        .sort((left, right) => right.score - left.score)
        .slice(0, 6);
      return { query, keywords, matches };
    },
    async runDiagnosis() {
      try {
        this.diagnosis = await this.fetchJson('/maintenance/diagnose', {
          method: 'POST',
          body: JSON.stringify(this.inspection)
        });
        this.tasks = await this.fetchJson('/maintenance/tasks');
        await this.loadPersistenceViews();
        this.backendOnline = true;
      } catch (error) {
        this.backendOnline = false;
        this.diagnosis = this.localDiagnosis();
        this.tasks.unshift(this.diagnosis.generatedTask);
        this.inspectionRecords = [this.localInspectionRecord(this.diagnosis), ...this.inspectionRecords].slice(0, 10);
      }
      this.activeTab = 'diagnose';
    },
    async runImageAnalysis() {
      try {
        this.imageAnalysis = await this.fetchJson('/maintenance/image/analyze', {
          method: 'POST',
          body: JSON.stringify(this.imageForm)
        });
        this.backendOnline = true;
      } catch (error) {
        this.backendOnline = false;
        this.imageAnalysis = this.localImageAnalysis();
      }
    },
    onVisionFileChange(event) {
      const file = event.target.files?.[0];
      this.visionUploadFile = file || null;
      if (file) {
        this.imageForm.fileName = file.name;
      }
    },
    async runUploadedImageAnalysis() {
      if (!this.visionUploadFile) return;
      const formData = new FormData();
      formData.append('file', this.visionUploadFile);
      formData.append('deviceType', this.imageForm.deviceType);
      formData.append('visualDescription', this.imageForm.visualDescription);
      try {
        const response = await fetch(apiUrl('/maintenance/vision/upload'), {
          method: 'POST',
          body: formData
        });
        if (!response.ok) {
          throw new Error(await response.text());
        }
        this.imageAnalysis = await response.json();
        this.backendOnline = true;
      } catch (error) {
        this.backendOnline = false;
        this.imageAnalysis = this.localImageAnalysis();
      }
    },
    async runVisionAnalysis() {
      try {
        this.visionAnalysis = await this.fetchJson('/maintenance/vision/analyze', {
          method: 'POST',
          body: JSON.stringify({
            ...this.visionForm,
            deviceType: this.imageForm.deviceType,
            visualDescription: this.imageForm.visualDescription
          })
        });
        this.backendOnline = true;
      } catch (error) {
        this.backendOnline = false;
        this.visionAnalysis = {
          provider: 'local-rule',
          model: 'feature-keyword-matcher',
          detectedFeatures: this.localImageAnalysis().detectedFeatures,
          riskLevel: this.estimateRiskFromFeatures(this.localImageAnalysis().detectedFeatures),
          conclusion: '视觉模型暂不可用，已使用本地图片特征规则兜底。',
          similarCases: this.localImageAnalysis().similarCases,
          recommendedActions: this.localImageAnalysis().inspectionTips
        };
      }
    },
    applyVisionToInspection() {
      if (!this.visionAnalysis) return;
      const profile = this.riskProfile(this.visionAnalysis.riskLevel);
      this.inspection = {
        ...this.inspection,
        deviceType: this.imageForm.deviceType,
        description: [
          this.imageForm.visualDescription,
          this.visionAnalysis.conclusion
        ].filter(Boolean).join('\n'),
        temperature: profile.temperature,
        vibration: profile.vibration,
        current: profile.current,
        imageFeatures: [...new Set(this.visionAnalysis.detectedFeatures || [])]
      };
    },
    async diagnoseFromVision() {
      this.applyVisionToInspection();
      await this.runDiagnosis();
    },
    riskProfile(riskLevel) {
      return {
        严重: { temperature: 92, vibration: 7.4, current: 132 },
        预警: { temperature: 82, vibration: 5.8, current: 118 },
        关注: { temperature: 72, vibration: 4.8, current: 96 },
        正常: { temperature: 55, vibration: 2.4, current: 72 }
      }[riskLevel] || { temperature: 72, vibration: 4.8, current: 96 };
    },
    estimateRiskFromFeatures(features) {
      const text = (features || []).join('、');
      if (/(焦痕|裂纹|绝缘破损)/.test(text)) return '严重';
      if (/(漏液|油污|变色|金属屑|磨损)/.test(text)) return '预警';
      return '关注';
    },
    localImageAnalysis() {
      const description = this.imageForm.visualDescription;
      const features = this.featureOptions.filter((feature) => description.includes(feature));
      if (!features.length && description.includes('焦')) features.push('焦痕');
      if (!features.length && description.includes('锈')) features.push('锈蚀');
      if (!features.length) features.push('待人工复核');
      const similarCases = this.cases.filter((item) => item.deviceType === this.imageForm.deviceType).slice(0, 2);
      return {
        fileName: this.imageForm.fileName,
        detectedFeatures: features,
        similarCases,
        inspectionTips: [
          '补拍设备铭牌、故障部位近景和周边环境远景',
          '将图片特征同步写入诊断输入，结合温度、振动、电流复判',
          similarCases[0]?.solution || '未匹配到高置信案例，建议专家复核'
        ]
      };
    },
    async updateTaskStatus(taskId, status) {
      try {
        const updatedTask = await this.fetchJson(`/maintenance/tasks/${taskId}/status`, {
          method: 'POST',
          body: JSON.stringify({ status })
        });
        this.tasks = this.tasks.map((task) => task.id === taskId ? updatedTask : task);
        this.backendOnline = true;
      } catch (error) {
        this.backendOnline = false;
        this.tasks = this.tasks.map((task) => task.id === taskId ? { ...task, status } : task);
      }
    },
    async updateTaskFlow(taskId, flow) {
      const currentTask = this.tasks.find((task) => task.id === taskId);
      try {
        const updatedTask = await this.fetchJson(`/maintenance/tasks/${taskId}/flow`, {
          method: 'POST',
          body: JSON.stringify(flow)
        });
        this.tasks = this.tasks.map((task) => task.id === taskId ? updatedTask : task);
        this.taskFlowEvents = await this.fetchJson('/maintenance/tasks/flow');
        this.backendOnline = true;
      } catch (error) {
        this.backendOnline = false;
        this.tasks = this.tasks.map((task) => task.id === taskId ? { ...task, status: flow.status } : task);
        this.taskFlowEvents = [
          {
            taskId,
            fromStatus: currentTask?.status || '待派工',
            toStatus: flow.status,
            operatorRole: flow.operatorRole,
            note: `${flow.note}（本地演示）`,
            operatedAt: new Date().toISOString()
          },
          ...this.taskFlowEvents
        ];
      }
    },
    async archiveTask(task) {
      try {
        const archive = await this.fetchJson(`/maintenance/tasks/${task.id}/archive`, {
          method: 'POST'
        });
        this.taskArchives = { ...this.taskArchives, [task.id]: archive };
        this.backendOnline = true;
      } catch (error) {
        this.backendOnline = false;
        const markdown = this.buildTaskArchiveMarkdown(task);
        this.saveMarkdownBlob(markdown, `${task.id}.md`);
        this.taskArchives = {
          ...this.taskArchives,
          [task.id]: {
            taskId: task.id,
            title: task.title,
            markdownDownloadUrl: '',
            pdfDownloadUrl: '',
            archivedAt: new Date().toISOString()
          }
        };
      }
    },
    buildTaskArchiveMarkdown(task) {
      return [
        '# 检修作业单归档',
        '',
        '## 基本信息',
        `- 作业单编号：${task.id}`,
        `- 标题：${task.title}`,
        `- 设备编号：${task.deviceId}`,
        `- 优先级：${task.priority}`,
        `- 当前状态：${task.status}`,
        `- 创建时间：${task.createdAt || '刚刚生成'}`,
        '',
        '## 作业步骤',
        ...task.steps.map((item) => `- ${item}`),
        '',
        '## 备件与工器具',
        ...task.spareParts.map((item) => `- ${item}`),
        '',
        '## 验收标准',
        ...task.acceptanceCriteria.map((item) => `- ${item}`)
      ].join('\n');
    },
    localDiagnosis() {
      const score = Math.min(100,
        (this.inspection.temperature >= 85 ? 35 : 18)
        + (this.inspection.vibration >= 7.1 ? 35 : 18)
        + (this.inspection.current >= 120 ? 20 : 0)
        + this.inspection.imageFeatures.length * 8
      );
      const riskLevel = score >= 75 ? '严重' : score >= 50 ? '预警' : score >= 25 ? '关注' : '正常';
      const matchedCases = this.cases.filter((item) => item.deviceType === this.inspection.deviceType).slice(0, 2);
      return {
        riskLevel,
        score,
        evidence: [
          `温度 ${this.inspection.temperature}℃，振动 ${this.inspection.vibration}mm/s，电流 ${this.inspection.current}A`,
          `图片特征：${this.inspection.imageFeatures.join('、') || '暂无'}`,
          `现场描述：${this.inspection.description}`
        ],
        possibleCauses: matchedCases.map((item) => `${item.faultName}：${item.cause}`),
        recommendedActions: [
          '执行断电、挂牌上锁和个人防护',
          '复测温度、振动、电流并保存趋势',
          matchedCases[0]?.solution || '补充检测信息后复判',
          '生成作业单并归档图片证据'
        ],
        similarCases: matchedCases,
        generatedTask: this.mockTask(riskLevel)
      };
    },
    mockTask(riskLevel = '严重') {
      return {
        id: `TASK-DEMO-${Date.now().toString().slice(-4)}`,
        deviceId: this.inspection.deviceId,
        title: `${riskLevel}风险检修作业单`,
        priority: riskLevel === '严重' ? 'P1' : 'P2',
        status: '待派工',
        steps: ['断电挂牌上锁', '复测异常参数', '检查外观图片特征对应部位', '处理故障点', '试运行验收'],
        spareParts: ['红外测温仪', '振动采集仪', '绝缘手套', '挂牌锁具'],
        acceptanceCriteria: ['异常参数回落', '无异常噪声和焦味', '图片和报告归档'],
        createdAt: new Date().toISOString()
      };
    },
    async downloadReport() {
      if (!this.diagnosis) return;
      let markdown = '';
      try {
        const report = await this.fetchJson('/maintenance/reports', {
          method: 'POST',
          body: JSON.stringify(this.diagnosis)
        });
        markdown = report.markdown;
        this.reports = [report, ...this.reports.filter((item) => item.reportId !== report.reportId)];
        this.reportPage.total += 1;
        this.correctionForm.reportId = report.reportId;
        this.correctionForm.correctedRiskLevel = report.riskLevel;
        this.backendOnline = true;
      } catch (error) {
        this.backendOnline = false;
      }
      const lines = [
        '# 设备检修诊断报告',
        '',
        `风险等级：${this.diagnosis.riskLevel}`,
        `评分：${this.diagnosis.score}`,
        '',
        '## 证据链',
        ...this.diagnosis.evidence.map((item) => `- ${item}`),
        '',
        '## 可能原因',
        ...this.diagnosis.possibleCauses.map((item) => `- ${item}`),
        '',
        '## 作业建议',
        ...this.diagnosis.recommendedActions.map((item) => `- ${item}`)
      ];
      const localMarkdown = lines.join('\n');
      if (!markdown) {
        const report = {
          reportId: `RPT-DEMO-${Date.now().toString().slice(-5)}`,
          title: '设备检修诊断报告',
          riskLevel: this.diagnosis.riskLevel,
          sections: ['风险结论', '证据链', '可能原因', '检修作业建议'],
          markdown: localMarkdown,
          generatedAt: new Date().toISOString(),
          markdownDownloadUrl: '',
          pdfDownloadUrl: ''
        };
        this.reports = [report, ...this.reports.filter((item) => item.reportId !== report.reportId)];
        this.reportPage.total = Math.max(this.reportPage.total, this.reports.length);
        this.correctionForm.reportId = report.reportId;
        this.correctionForm.correctedRiskLevel = report.riskLevel;
      }
      const latestReport = this.reports[0];
      if (latestReport?.markdownDownloadUrl) {
        window.open(this.apiDownloadUrl(latestReport.markdownDownloadUrl), '_blank', 'noopener');
      } else {
        this.saveMarkdownBlob(markdown || localMarkdown, 'maintenance-report.md');
      }
    },
    async submitReportCorrection() {
      const payload = {
        reportId: this.correctionForm.reportId,
        correctedRiskLevel: this.correctionForm.correctedRiskLevel,
        reviewer: this.correctionForm.reviewer,
        correctedEvidence: this.parseListInput(this.correctionForm.evidenceText),
        correctedCauses: this.parseListInput(this.correctionForm.causesText),
        correctedActions: this.parseListInput(this.correctionForm.actionsText),
        reviewNote: this.correctionForm.reviewNote
      };
      try {
        const report = await this.fetchJson('/maintenance/reports/corrections', {
          method: 'POST',
          body: JSON.stringify(payload)
        });
        this.reports = [report, ...this.reports.filter((item) => item.reportId !== report.reportId)];
        await this.loadPersistenceViews();
        this.backendOnline = true;
      } catch (error) {
        this.backendOnline = false;
        const markdown = [
          '# AI 诊断结果专家修正记录',
          '',
          `- 原报告编号：${payload.reportId || '未填写'}`,
          `- 复核人：${payload.reviewer}`,
          `- 修正后风险等级：${payload.correctedRiskLevel}`,
          '',
          '## 修正证据',
          ...payload.correctedEvidence.map((item) => `- ${item}`),
          '',
          '## 修正原因',
          ...payload.correctedCauses.map((item) => `- ${item}`),
          '',
          '## 修正措施',
          ...payload.correctedActions.map((item) => `- ${item}`),
          '',
          '## 复核说明',
          payload.reviewNote
        ].join('\n');
        this.reports = [
          {
            reportId: `CORR-DEMO-${Date.now().toString().slice(-5)}`,
            title: 'AI 诊断结果专家修正记录',
            riskLevel: payload.correctedRiskLevel,
            sections: ['专家修正结论', '修正证据', '修正原因', '修正措施'],
            markdown,
            generatedAt: new Date().toISOString(),
            markdownDownloadUrl: '',
            pdfDownloadUrl: ''
          },
          ...this.reports
        ];
        this.correctionRecords = [
          {
            id: `RC-DEMO-${Date.now().toString().slice(-5)}`,
            sourceReportId: payload.reportId,
            correctedReportId: this.reports[0].reportId,
            reviewer: payload.reviewer,
            correctedRiskLevel: payload.correctedRiskLevel,
            reviewNote: payload.reviewNote,
            createdAt: new Date().toISOString()
          },
          ...this.correctionRecords
        ];
      }
    },
    async loadPersistenceViews() {
      try {
        const [reportPage, inspections, corrections] = await Promise.all([
          this.fetchJson(`/maintenance/reports/page?page=${this.reportPage.page}&size=${this.reportPage.size}`),
          this.fetchJson('/maintenance/inspections?page=1&size=10'),
          this.fetchJson('/maintenance/reports/corrections?page=1&size=10')
        ]);
        this.reportPage = {
          page: reportPage.page,
          size: reportPage.size,
          total: reportPage.total
        };
        this.reports = reportPage.records;
        this.inspectionRecords = inspections;
        this.correctionRecords = corrections;
      } catch (error) {
        this.reportPage.total = this.reports.length;
      }
    },
    async changeReportPage(delta) {
      const nextPage = Math.max(1, this.reportPage.page + delta);
      if (nextPage === this.reportPage.page) return;
      this.reportPage.page = nextPage;
      await this.loadPersistenceViews();
    },
    localInspectionRecord(diagnosis) {
      return {
        id: `INSP-DEMO-${Date.now().toString().slice(-5)}`,
        deviceId: this.inspection.deviceId,
        deviceType: this.inspection.deviceType,
        description: this.inspection.description,
        temperature: this.inspection.temperature,
        vibration: this.inspection.vibration,
        current: this.inspection.current,
        imageFeatures: this.inspection.imageFeatures,
        riskLevel: diagnosis.riskLevel,
        score: diagnosis.score,
        evidence: diagnosis.evidence,
        createdAt: new Date().toISOString()
      };
    },
    downloadMarkdown(report) {
      this.saveMarkdownBlob(report.markdown, `${report.reportId}.md`);
    },
    saveMarkdownBlob(markdown, fileName) {
      const blob = new Blob([markdown], { type: 'text/markdown;charset=utf-8' });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = fileName;
      link.click();
      URL.revokeObjectURL(url);
    },
    apiDownloadUrl(path) {
      return apiUrl(path);
    },
    apiInlineUrl(path) {
      const separator = path.includes('?') ? '&' : '?';
      return apiUrl(`${path}${separator}inline=true`);
    },
    formatTime(value) {
      if (!value) return '刚刚生成';
      return String(value).replace('T', ' ').slice(0, 19);
    },
    riskClass(risk) {
      return {
        正常: 'risk-ok',
        关注: 'risk-watch',
        预警: 'risk-warn',
        严重: 'risk-danger'
      }[risk] || 'risk-watch';
    },
    sendMessage() {
      if (!this.inputMessage.trim() || this.isStreaming) return;
      const message = this.inputMessage.trim();
      this.messages.push({ role: 'user', content: message });
      this.inputMessage = '';
      this.connectSSE(message);
    },
    connectSSE(message) {
      if (this.eventSource) {
        this.eventSource.close();
      }
      const url = apiUrl(`/ai/app/chat/sse?message=${encodeURIComponent(message)}&chatId=${this.chatId}`);
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
        }
      };

      this.eventSource.onerror = () => {
        this.messages[aiMessageIndex].content += '后端 AI 服务未启动，当前可继续使用诊断、案例和作业单演示模块。';
        this.eventSource.close();
        this.isStreaming = false;
      };
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
.workspace {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  background: #eef3ef;
  color: #15241e;
}

.sidebar {
  background: #10231d;
  color: #f6fbf8;
  padding: 28px;
}

.back {
  color: #8dd8af;
  text-decoration: none;
  font-weight: 800;
}

.sidebar h1 {
  margin: 28px 0 12px;
  font-size: 34px;
}

.sidebar p {
  color: #c9d8d0;
  line-height: 1.7;
}

nav {
  display: grid;
  gap: 10px;
  margin-top: 28px;
}

nav button {
  min-height: 48px;
  border: 1px solid rgba(246, 251, 248, 0.14);
  border-radius: 6px;
  background: rgba(246, 251, 248, 0.06);
  color: #f6fbf8;
  text-align: left;
  padding: 0 14px;
  cursor: pointer;
}

nav button span {
  color: #e6b85c;
  margin-right: 10px;
  font-weight: 800;
}

nav button.active {
  background: #8dd8af;
  color: #10231d;
}

.content {
  padding: 24px;
  overflow: auto;
}

.topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
}

.topbar-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.topbar small {
  color: #2b7c5a;
  font-weight: 800;
}

.topbar h2 {
  margin: 6px 0 0;
  font-size: 30px;
}

.ghost-button,
.ghost-link,
.danger-button,
.primary-button,
.chat-input button {
  min-height: 42px;
  border-radius: 6px;
  font-weight: 800;
  cursor: pointer;
}

.ghost-button {
  border: 1px solid #c7d4ce;
  background: #ffffff;
  color: #15241e;
  padding: 0 16px;
}

.danger-button {
  border: 1px solid #e0b7b7;
  background: #fff4f4;
  color: #9f1d1d;
  padding: 0 16px;
}

.ghost-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #c7d4ce;
  background: #ffffff;
  color: #15241e;
  padding: 0 16px;
  text-decoration: none;
}

.primary-button {
  border: 0;
  background: #15241e;
  color: #ffffff;
  padding: 0 18px;
}

.metric-grid,
.cards-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.delivery-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 14px;
}

.delivery-strip article,
.completion-card,
.report-card,
.empty-state {
  border: 1px solid #d4dfd9;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 12px 34px rgba(16, 35, 29, 0.08);
  padding: 18px;
}

.delivery-strip span,
.completion-card span,
.report-card span {
  color: #2b7c5a;
  font-size: 12px;
  font-weight: 900;
}

.delivery-strip strong {
  display: block;
  margin: 8px 0 10px;
}

.delivery-strip small,
.completion-card small {
  display: block;
  color: #66766e;
  line-height: 1.5;
}

.progress {
  height: 8px;
  border-radius: 99px;
  background: #e5ece8;
  overflow: hidden;
  margin: 10px 0;
}

.progress i {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, #2b7c5a, #e6b85c);
}

.metric-card,
.panel,
.device-card,
.case-card,
.knowledge-card,
.diagnosis-result,
.task-card,
.quick-form,
.diagnose-form,
.chat-layout {
  border: 1px solid #d4dfd9;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 12px 34px rgba(16, 35, 29, 0.08);
}

.metric-card {
  padding: 20px;
}

.metric-card span {
  color: #66766e;
}

.metric-card strong {
  display: block;
  margin: 12px 0 6px;
  font-size: 34px;
}

.metric-card p,
.device-card p,
.case-card p,
.task-card p {
  color: #66766e;
  line-height: 1.6;
}

.two-column {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  margin-top: 14px;
}

.panel {
  padding: 20px;
}

.risk-row {
  display: grid;
  grid-template-columns: 60px 1fr 32px;
  align-items: center;
  gap: 12px;
  margin: 13px 0;
}

.bar {
  height: 9px;
  border-radius: 99px;
  background: #e5ece8;
  overflow: hidden;
}

.bar i {
  display: block;
  height: 100%;
  background: #2b7c5a;
}

.task-mini {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  border-top: 1px solid #e4ece7;
  padding: 13px 0;
}

.cards-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.device-card,
.case-card,
.task-card {
  padding: 20px;
}

.quick-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  padding: 20px;
}

.quick-form h3 {
  grid-column: 1 / -1;
  margin: 0;
}

.quick-form.wide-card {
  grid-column: 1 / -1;
}

.card-head,
.result-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 16px 0;
}

.chips i,
.risk,
.case-card span {
  border-radius: 99px;
  padding: 5px 9px;
  font-style: normal;
  font-size: 12px;
  font-weight: 800;
}

.chips i {
  background: #edf3ef;
}

.risk-ok { background: #dff6e9; color: #146a3d; }
.risk-watch { background: #fff3c7; color: #7a5610; }
.risk-warn { background: #ffe0c2; color: #8a3d00; }
.risk-danger { background: #ffd8d8; color: #9f1d1d; }

.cases {
  display: grid;
  gap: 14px;
}

.case-card {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

.case-card span {
  background: #edf3ef;
}

.case-detail {
  border-left: 1px solid #e4ece7;
  padding-left: 20px;
}

.diagnose {
  display: grid;
  grid-template-columns: minmax(0, 0.9fr) minmax(320px, 0.7fr);
  gap: 14px;
}

.diagnose-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  padding: 20px;
}

label {
  display: grid;
  gap: 7px;
  font-weight: 800;
}

input,
select,
textarea {
  width: 100%;
  border: 1px solid #cbd8d1;
  border-radius: 6px;
  padding: 11px;
  font: inherit;
}

textarea {
  min-height: 104px;
  resize: vertical;
}

.wide {
  grid-column: 1 / -1;
}

.feature-picker {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.feature-picker button {
  border: 1px solid #cbd8d1;
  border-radius: 99px;
  background: #ffffff;
  padding: 8px 11px;
  cursor: pointer;
}

.feature-picker button.selected {
  border-color: #2b7c5a;
  background: #dff6e9;
}

.diagnosis-result {
  padding: 20px;
}

.result-head {
  justify-content: flex-start;
}

.result-head em {
  margin-left: auto;
  font-style: normal;
  font-size: 24px;
  font-weight: 900;
}

li {
  margin: 7px 0;
  line-height: 1.6;
}

.tasks {
  display: grid;
  gap: 14px;
}

.management-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.management-card {
  border: 1px solid #d4dfd9;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 12px 34px rgba(16, 35, 29, 0.08);
  padding: 20px;
}

.management-card h3 {
  margin: 12px 0 8px;
}

.management-card p {
  color: #66766e;
  line-height: 1.7;
}

.workflow {
  display: grid;
  gap: 14px;
}

.flow-line {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 8px;
}

.flow-line span {
  min-height: 42px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  background: #edf3ef;
  color: #15241e;
  font-weight: 800;
}

.completion-list,
.reports {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.completion-card h3,
.report-card h3,
.empty-state h3 {
  margin: 12px 0 10px;
}

.completion-card p,
.report-card p,
.empty-state p {
  color: #66766e;
  line-height: 1.7;
}

.report-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
}

.empty-state {
  grid-column: 1 / -1;
}

.empty-line {
  color: #66766e;
  padding: 12px 0;
}

.record-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 14px;
}

.record-strip span {
  border-radius: 99px;
  background: #edf3ef;
  color: #15241e;
  padding: 7px 11px;
  font-weight: 800;
}

.record-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  align-items: center;
  gap: 12px;
  border-top: 1px solid #e4ece7;
  padding: 13px 0;
}

.record-row p {
  margin: 4px 0 0;
}

.record-row small {
  color: #66766e;
  white-space: nowrap;
}

.pager-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.knowledge-grid,
.knowledge-workbench {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.knowledge-workbench .quick-form,
.knowledge-search,
.knowledge-results,
.knowledge-review-list {
  grid-column: span 3;
}

.knowledge-search {
  align-items: end;
}

.search-summary {
  align-self: center;
  color: #66766e;
  font-weight: 800;
}

.knowledge-results {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.knowledge-review-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.knowledge-card {
  padding: 20px;
}

.knowledge-card span {
  color: #2b7c5a;
  font-size: 12px;
  font-weight: 900;
}

.knowledge-card p {
  color: #66766e;
  line-height: 1.7;
}

.task-columns {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 22px;
  border-top: 1px solid #e4ece7;
  margin-top: 16px;
  padding-top: 16px;
}

.task-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  border-top: 1px solid #e4ece7;
  margin-top: 16px;
  padding-top: 14px;
}

.task-actions button {
  min-height: 34px;
  border: 1px solid #cbd8d1;
  border-radius: 6px;
  background: #f8fbf9;
  cursor: pointer;
}

.task-archive {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #e4ece7;
}

.task-archive span {
  color: #2b7c5a;
  font-weight: 900;
}

.task-archive a {
  min-height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #c7d4ce;
  border-radius: 6px;
  background: #ffffff;
  color: #15241e;
  padding: 0 12px;
  font-weight: 800;
  text-decoration: none;
}

.chat-layout {
  height: calc(100vh - 132px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-history {
  flex: 1;
  overflow: auto;
  padding: 20px;
}

.message {
  display: flex;
  margin: 10px 0;
}

.message.user {
  justify-content: flex-end;
}

.bubble {
  max-width: 760px;
  border: 1px solid #d9e3dd;
  border-radius: 8px;
  background: #ffffff;
  padding: 12px 14px;
  line-height: 1.7;
  white-space: normal;
}

.message.user .bubble {
  background: #15241e;
  color: #ffffff;
  white-space: pre-wrap;
}

.chat-input {
  display: grid;
  grid-template-columns: 1fr 96px;
  gap: 10px;
  border-top: 1px solid #d4dfd9;
  padding: 14px;
}

.chat-input button {
  border: 0;
  background: #2b7c5a;
  color: #ffffff;
}

@media (max-width: 1100px) {
  .workspace,
  .diagnose,
  .two-column {
    grid-template-columns: 1fr;
  }

  .metric-grid,
  .cards-grid,
    .management-grid,
    .delivery-strip,
    .completion-list,
    .reports,
    .knowledge-results,
    .knowledge-review-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .metric-grid,
  .cards-grid,
  .management-grid,
  .delivery-strip,
  .completion-list,
  .reports,
  .knowledge-grid,
  .knowledge-workbench,
  .knowledge-results,
  .knowledge-review-list,
  .case-card,
  .quick-form,
  .diagnose-form,
  .task-columns {
    grid-template-columns: 1fr;
  }

  .flow-line {
    grid-template-columns: 1fr;
  }
}
</style>
