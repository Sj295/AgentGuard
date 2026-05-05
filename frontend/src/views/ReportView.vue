<template>
  <div class="report-grid">
    <!-- Left: Config -->
    <div class="panel">
      <div class="panel-header">报告配置</div>
      <el-form label-position="top" @submit.prevent>
        <el-form-item label="当前项目">
          <div class="project-display">{{ globalProjectName || ('#' + form.projectId) }}</div>
        </el-form-item>
        <el-form-item label="包含内容">
          <div class="report-options">
            <el-checkbox v-model="form.includeScanResult">扫描结果</el-checkbox>
            <el-checkbox v-model="form.includeAgentRules">Agent 规则</el-checkbox>
            <el-checkbox v-model="form.includeRiskReports">风险报告</el-checkbox>
            <el-checkbox v-model="form.includeGitAudit">Git 审计</el-checkbox>
            <el-checkbox v-model="form.includePreflight">预执行检查</el-checkbox>
          </div>
        </el-form-item>
        <div class="report-actions">
          <el-button type="primary" :icon="Document" :loading="generating" size="large" style="flex:1" @click="handleGenerate">
            生成预览
          </el-button>
          <el-button :icon="Download" :loading="exporting" size="large" style="flex:1" @click="handleExport">
            导出到文件
          </el-button>
        </div>
      </el-form>

      <div v-if="exportResult" class="export-result">
        <el-icon :size="16" color="#10B981"><CircleCheck /></el-icon>
        <div>
          <div class="export-msg">{{ exportResult.written ? '报告已导出' : '报告已生成' }}</div>
          <div class="export-path">
            {{ exportResult.targetPath }}
            <button class="copy-link" @click="copyPath">复制路径</button>
          </div>
        </div>
      </div>
    </div>

    <!-- Right: Preview -->
    <div class="panel">
      <div class="panel-header">
        <span>报告预览</span>
        <div class="preview-actions">
          <el-button v-if="reportContent" size="small" text @click="downloadMd">下载 .md</el-button>
          <el-button v-if="reportContent" type="primary" plain size="small" :loading="aiLoading" @click="handleAiSummary">
            生成 AI 报告摘要
          </el-button>
        </div>
      </div>
      <template v-if="reportContent">
        <div class="md-preview" v-html="renderedHtml"></div>
        <AiInsightPanel
          v-if="aiResult"
          :mocked="aiResult.mocked"
          :confidence-note="aiResult.confidenceNote"
          summary-title="执行摘要"
          :summary="aiResult.executiveSummary"
          :sections="[
            { title: '关键发现', items: aiResult.keyFindings },
            { title: '优先动作', items: aiResult.priorityActions }
          ]"
        />
      </template>
      <EmptyState v-else title="生成安全报告" desc="选择包含内容后点击生成预览" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, computed, inject, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Document, Download, CircleCheck } from '@element-plus/icons-vue'
import EmptyState from '../components/EmptyState.vue'
import AiInsightPanel from '../components/AiInsightPanel.vue'
import { generateMarkdownReport, exportMarkdownReport } from '../api/report'
import { summarizeReportWithAi } from '../api/ai'
import type { MarkdownReportVO } from '../types/report'
import type { AiReportSummaryVO } from '../types/ai'

const globalProjectId = inject<any>('globalProjectId', ref(null))
const globalProjectName = inject<any>('globalProjectName', ref(''))

const form = reactive({
  projectId: null as number | null,
  includeScanResult: true,
  includeAgentRules: true,
  includeRiskReports: true,
  includeGitAudit: true,
  includePreflight: true
})

const generating = ref(false)
const exporting = ref(false)
const aiLoading = ref(false)
const reportContent = ref('')
const exportResult = ref<MarkdownReportVO | null>(null)
const aiResult = ref<AiReportSummaryVO | null>(null)

onMounted(() => {
  if (globalProjectId.value) form.projectId = globalProjectId.value
})

const renderedHtml = computed(() => {
  return reportContent.value
    .replace(/^### (.*$)/gim, '<h3>$1</h3>')
    .replace(/^## (.*$)/gim, '<h2>$1</h2>')
    .replace(/^# (.*$)/gim, '<h1>$1</h1>')
    .replace(/\*\*(.*?)\*\*/gim, '<strong>$1</strong>')
    .replace(/\*(.*?)\*/gim, '<em>$1</em>')
    .replace(/`([^`]+)`/gim, '<code>$1</code>')
    .replace(/^- (.*$)/gim, '<li>$1</li>')
    .replace(/(<li>.*<\/li>\n?)+/gim, '<ul>$&</ul>')
    .replace(/\n/gim, '<br>')
})

const handleGenerate = async () => {
  if (!form.projectId) { ElMessage.warning('请先扫描项目'); return }
  generating.value = true
  try {
    const res = await generateMarkdownReport({ ...form } as any)
    if (res.data.code === 0) {
      reportContent.value = res.data.data.markdown
      aiResult.value = null
      ElMessage.success('报告生成成功')
    } else {
      ElMessage.error(res.data.message)
    }
  } catch {
    ElMessage.error('生成失败')
  } finally {
    generating.value = false
  }
}

const handleExport = async () => {
  if (!form.projectId) { ElMessage.warning('请先扫描项目'); return }
  exporting.value = true
  try {
    const res = await exportMarkdownReport({ ...form, overwrite: false } as any)
    if (res.data.code === 0) {
      exportResult.value = res.data.data
      ElMessage.success('导出成功')
    } else {
      ElMessage.error(res.data.message)
    }
  } catch {
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}

const downloadMd = () => {
  const blob = new Blob([reportContent.value], { type: 'text/markdown' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `security-report-${form.projectId}.md`
  a.click()
  URL.revokeObjectURL(url)
}

const handleAiSummary = async () => {
  if (!form.projectId || !reportContent.value) {
    ElMessage.warning('请先生成 Markdown 报告')
    return
  }
  aiLoading.value = true
  try {
    const res = await summarizeReportWithAi({
      projectId: form.projectId,
      markdown: reportContent.value
    })
    if (res.data.code === 0) {
      aiResult.value = res.data.data
      ElMessage.success('AI 摘要已生成')
    } else {
      ElMessage.error(res.data.message)
    }
  } catch {
    ElMessage.error('AI 摘要生成失败')
  } finally {
    aiLoading.value = false
  }
}

const copyPath = async () => {
  if (!exportResult.value?.targetPath) return
  try {
    await navigator.clipboard.writeText(exportResult.value.targetPath)
    ElMessage.success('已复制')
  } catch { /* silent */ }
}

watch(globalProjectId, (val) => {
  if (val && val !== form.projectId) {
    form.projectId = val
    reportContent.value = ''
    exportResult.value = null
    aiResult.value = null
  }
})
</script>

<style scoped>
.report-grid {
  display: grid;
  grid-template-columns: 360px 1fr;
  gap: 20px;
  align-items: start;
}

.project-display {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
  background: #F1F5F9;
  padding: 8px 12px;
  border-radius: 8px;
  width: 100%;
}

.report-options {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.report-actions {
  display: flex;
  gap: 12px;
  margin-top: 8px;
}

.export-result {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-top: 16px;
  padding: 12px;
  background: rgba(16,185,129,0.04);
  border: 1px solid rgba(16,185,129,0.12);
  border-radius: 8px;
}

.export-msg { font-size: 13px; font-weight: 500; color: #059669; }
.export-path { font-size: 12px; color: #64748B; margin-top: 4px; display: flex; align-items: center; gap: 8px; }

.copy-link {
  background: none;
  border: none;
  color: #4F46E5;
  font-size: 12px;
  cursor: pointer;
  padding: 0;
  font-family: inherit;
}

.preview-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* Markdown preview */
.md-preview {
  font-size: 14px;
  line-height: 1.8;
  color: #334155;
}

.md-preview :deep(h1), .md-preview :deep(h2), .md-preview :deep(h3) {
  color: #111827;
  margin-top: 20px;
  margin-bottom: 10px;
}

.md-preview :deep(code) {
  background: #F1F5F9;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'JetBrains Mono', 'Fira Code', Consolas, monospace;
  font-size: 13px;
}

.md-preview :deep(ul) {
  padding-left: 20px;
}

@media (max-width: 900px) {
  .report-grid { grid-template-columns: 1fr; }
}
</style>
