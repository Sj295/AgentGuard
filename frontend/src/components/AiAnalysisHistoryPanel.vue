<template>
  <div class="ai-history-wrap">
    <div class="panel">
      <div class="panel-header">
        <span>最近 AI 增强分析</span>
        <el-button text size="small" @click="loadLatest">刷新</el-button>
      </div>
      <div v-if="latestRecords.length" class="latest-list">
        <div v-for="record in latestRecords" :key="`latest-${record.id}`" class="latest-item" @click="openDetail(record.id)">
          <div class="latest-main">
            <span class="latest-type">{{ analysisTypeLabel(record.analysisType) }}</span>
            <span class="latest-provider">{{ record.provider }} / {{ record.model }}</span>
          </div>
          <div class="latest-meta">
            <el-tag v-if="record.mocked" type="warning" effect="plain" size="small">Mock</el-tag>
            <el-tag :type="record.success ? 'success' : 'danger'" effect="plain" size="small">
              {{ record.success ? 'SUCCESS' : 'FAILED' }}
            </el-tag>
            <span class="latest-time">{{ formatTime(record.createdTime) }}</span>
          </div>
        </div>
      </div>
      <EmptyState v-else title="暂无 AI 分析记录" desc="调用 AI 增强接口后将在此显示最近记录" />
    </div>

    <div class="panel">
      <div class="panel-header">
        <span>AI 分析历史</span>
        <div class="history-actions">
          <el-select v-model="filters.analysisType" clearable placeholder="分析类型" size="small" style="width: 180px" @change="handleSearch">
            <el-option label="全部" value="" />
            <el-option label="Git Diff 影响分析" value="GIT_DIFF_ANALYSIS" />
            <el-option label="风险解释" value="RISK_EXPLAIN" />
            <el-option label="报告摘要" value="REPORT_SUMMARY" />
          </el-select>
          <el-button text size="small" @click="handleSearch">查询</el-button>
        </div>
      </div>

      <el-table v-if="records.length" :data="records" size="small" class="history-table">
        <el-table-column prop="analysisType" label="分析类型" min-width="140">
          <template #default="{ row }">
            {{ analysisTypeLabel(row.analysisType) }}
          </template>
        </el-table-column>
        <el-table-column prop="provider" label="Provider" min-width="130" />
        <el-table-column prop="model" label="Model" min-width="120" />
        <el-table-column label="Mock" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.mocked" type="warning" effect="plain" size="small">Mock</el-tag>
            <span v-else>--</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.success ? 'success' : 'danger'" effect="plain" size="small">
              {{ row.success ? 'SUCCESS' : 'FAILED' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="latencyMs" label="耗时(ms)" width="100" />
        <el-table-column label="时间" min-width="170">
          <template #default="{ row }">
            {{ formatTime(row.createdTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="openDetail(row.id)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <EmptyState v-else title="暂无历史记录" desc="可按分析类型筛选后查看详情" />

      <el-pagination
        v-if="pagination.total > 0"
        v-model:current-page="pagination.current"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        layout="total, prev, pager, next"
        style="margin-top: 16px; justify-content: flex-end;"
        @current-change="loadRecords"
      />
    </div>

    <el-dialog v-model="detailVisible" title="AI 分析记录详情" width="860px">
      <div v-if="detail" class="detail-wrap">
        <div class="detail-meta-grid">
          <div><strong>分析类型：</strong>{{ analysisTypeLabel(detail.analysisType) }}</div>
          <div><strong>Provider：</strong>{{ detail.provider }}</div>
          <div><strong>Model：</strong>{{ detail.model }}</div>
          <div><strong>耗时：</strong>{{ detail.latencyMs }} ms</div>
          <div><strong>Mock：</strong>{{ detail.mocked ? '是' : '否' }}</div>
          <div><strong>状态：</strong>{{ detail.success ? 'SUCCESS' : 'FAILED' }}</div>
          <div><strong>Source Report：</strong>{{ detail.sourceReportId ?? '--' }}</div>
          <div><strong>时间：</strong>{{ formatTime(detail.createdTime) }}</div>
        </div>

        <div class="detail-section">
          <div class="detail-title">输入摘要</div>
          <div class="detail-text">{{ detail.inputSummary || '无' }}</div>
        </div>

        <div class="detail-section">
          <div class="detail-title">输出内容</div>
          <CodeBlock :code="prettyOutput(detail.outputContent)" title="ai-output.json" />
        </div>

        <div class="detail-section" v-if="detail.errorMessage">
          <div class="detail-title">错误信息</div>
          <div class="detail-error">{{ detail.errorMessage }}</div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { inject, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getAiRecordDetail, getLatestAiRecords, getProjectAiRecords } from '../api/aiRecord'
import type { AiAnalysisRecordVO, AiAnalysisType } from '../types/aiRecord'
import EmptyState from './EmptyState.vue'
import CodeBlock from './CodeBlock.vue'

const props = defineProps<{ projectId?: number | null }>()
const injectedProjectId = inject<any>('globalProjectId', ref(null))

const currentProjectId = ref<number | null>(null)
const latestRecords = ref<AiAnalysisRecordVO[]>([])
const records = ref<AiAnalysisRecordVO[]>([])
const detail = ref<AiAnalysisRecordVO | null>(null)
const detailVisible = ref(false)

const filters = reactive<{ analysisType: AiAnalysisType | '' }>({ analysisType: '' })
const pagination = reactive({ current: 1, size: 10, total: 0 })

const analysisTypeLabel = (type: AiAnalysisType) => {
  if (type === 'GIT_DIFF_ANALYSIS') return 'Git Diff 影响分析'
  if (type === 'RISK_EXPLAIN') return '风险解释'
  return '报告摘要'
}

const formatTime = (time: string) => new Date(time).toLocaleString('zh-CN')

const prettyOutput = (raw?: string) => {
  if (!raw) return '{}'
  try {
    return JSON.stringify(JSON.parse(raw), null, 2)
  } catch {
    return raw
  }
}

const syncProjectId = () => {
  const incoming = props.projectId ?? injectedProjectId.value
  currentProjectId.value = incoming ? Number(incoming) : null
}

const loadLatest = async () => {
  if (!currentProjectId.value) return
  try {
    const res = await getLatestAiRecords(currentProjectId.value, 5)
    if (res.data.code === 0) {
      latestRecords.value = res.data.data || []
    }
  } catch {
    ElMessage.error('加载最近 AI 记录失败')
  }
}

const loadRecords = async () => {
  if (!currentProjectId.value) return
  try {
    const res = await getProjectAiRecords(currentProjectId.value, {
      current: pagination.current,
      size: pagination.size,
      analysisType: filters.analysisType
    })
    if (res.data.code === 0) {
      records.value = res.data.data.records || []
      pagination.total = res.data.data.total || 0
    } else {
      ElMessage.error(res.data.message)
    }
  } catch {
    ElMessage.error('加载 AI 历史记录失败')
  }
}

const handleSearch = () => {
  pagination.current = 1
  loadRecords()
}

const openDetail = async (id: number) => {
  try {
    const res = await getAiRecordDetail(id)
    if (res.data.code === 0) {
      detail.value = res.data.data
      detailVisible.value = true
    } else {
      ElMessage.error(res.data.message)
    }
  } catch {
    ElMessage.error('加载记录详情失败')
  }
}

const loadAll = async () => {
  if (!currentProjectId.value) return
  await Promise.all([loadLatest(), loadRecords()])
}

watch(() => props.projectId, () => {
  syncProjectId()
  pagination.current = 1
  loadAll()
})

watch(injectedProjectId, () => {
  if (props.projectId !== undefined) return
  syncProjectId()
  pagination.current = 1
  loadAll()
})

onMounted(() => {
  syncProjectId()
  loadAll()
})
</script>

<style scoped>
.ai-history-wrap {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.latest-list {
  display: flex;
  flex-direction: column;
}

.latest-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid #F1F5F9;
  cursor: pointer;
}

.latest-item:last-child {
  border-bottom: none;
}

.latest-main {
  display: flex;
  align-items: center;
  gap: 12px;
}

.latest-type {
  font-size: 13px;
  font-weight: 600;
  color: #111827;
}

.latest-provider {
  font-size: 12px;
  color: #64748B;
}

.latest-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.latest-time {
  font-size: 12px;
  color: #94A3B8;
}

.history-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.detail-wrap {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.detail-meta-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
  font-size: 13px;
  color: #334155;
}

.detail-section {
  padding-top: 4px;
}

.detail-title {
  font-size: 13px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 8px;
}

.detail-text {
  font-size: 13px;
  color: #475569;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.detail-error {
  font-size: 13px;
  color: #DC2626;
  line-height: 1.5;
  white-space: pre-wrap;
}

@media (max-width: 900px) {
  .detail-meta-grid {
    grid-template-columns: 1fr;
  }
}
</style>
