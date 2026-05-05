<template>
  <div class="perm-page">
    <!-- Left: Config -->
    <div class="panel">
      <div class="panel-header">权限配置</div>
      <el-form label-position="top" @submit.prevent>
        <el-form-item label="当前项目">
          <div class="project-display">{{ globalProjectName || ('#' + form.projectId) }}</div>
        </el-form-item>
        <el-form-item label="Agent 类型">
          <el-select v-model="form.agentType" style="width: 100%">
            <el-option label="Codex" value="CODEX" />
            <el-option label="Claude Code" value="CLAUDE" />
            <el-option label="Cursor" value="CURSOR" />
          </el-select>
        </el-form-item>
        <el-form-item label="任务类型">
          <el-select v-model="form.taskType" style="width: 100%">
            <el-option label="前端重构" value="FRONTEND_REFACTOR" />
            <el-option label="Bug 修复" value="BUG_FIX" />
            <el-option label="大规模重构" value="LARGE_REFACTOR" />
            <el-option label="新功能" value="NEW_FEATURE" />
            <el-option label="测试编写" value="TEST_WRITING" />
            <el-option label="文档" value="DOCUMENTATION" />
          </el-select>
        </el-form-item>
        <el-form-item label="沙箱模式">
          <el-select v-model="form.sandboxMode" style="width: 100%">
            <el-option label="仅限工作区写入" value="WORKSPACE_WRITE" />
            <el-option label="只读" value="READ_ONLY" />
            <el-option label="完全访问 (有风险)" value="DANGER_FULL_ACCESS" />
          </el-select>
        </el-form-item>
        <el-form-item label="审批方式">
          <el-select v-model="form.approvalPolicy" style="width: 100%">
            <el-option label="按需审批" value="ON_REQUEST" />
            <el-option label="自动批准" value="AUTO_APPROVE" />
            <el-option label="无需审批" value="NEVER" />
          </el-select>
        </el-form-item>
        <div class="switch-row">
          <span>网络访问</span>
          <el-switch v-model="form.networkAccess" />
        </div>
        <div class="switch-row">
          <span>允许删除</span>
          <el-switch v-model="form.allowDelete" />
        </div>
        <el-button type="primary" :icon="Search" :loading="assessing" size="large" style="width: 100%; margin-top: 16px" @click="handleAssess">
          评估权限
        </el-button>
      </el-form>
    </div>

    <!-- Right: Result -->
    <div>
      <div v-if="isHighRisk" class="risk-banner">
        <el-icon :size="18"><Warning /></el-icon>
        <span>当前配置不建议执行，可能导致误删、超出权限范围或不可回滚变更。</span>
      </div>

      <div v-if="result" class="panel shared-slide-in">
        <div class="panel-header">
          <span>评估结果</span>
          <RiskBadge :level="result.riskLevel" size="large" />
        </div>

        <div class="score-area">
          <div class="score-circle" :style="{ borderColor: scoreColor }">
            <span class="score-num" :style="{ color: scoreColor }">{{ result.score }}</span>
            <span class="score-label">风险分数</span>
          </div>
        </div>

        <ReportPanel v-if="result.riskItems?.length" title="风险项" :items="result.riskItems" type="warn" :count="result.riskItems.length" style="margin-bottom: 12px;" />
        <ReportPanel v-if="result.suggestions?.length" title="建议" :items="result.suggestions" type="info" :count="result.suggestions.length" style="margin-bottom: 12px;" />

        <div class="ai-action-row">
          <el-button type="primary" plain :loading="aiLoading" @click="handleAiExplain">
            AI 解释风险
          </el-button>
        </div>
        <AiInsightPanel
          v-if="aiResult"
          :mocked="aiResult.mocked"
          :confidence-note="aiResult.confidenceNote"
          summary-title="风险摘要"
          :summary="aiResult.riskSummary"
          :sections="[
            { title: '风险影响', items: aiResult.whyItMatters },
            { title: '修复计划', items: aiResult.fixPlan },
            { title: '安全下一步', items: aiResult.safeNextSteps }
          ]"
        />

        <div v-if="result.recommendedConfig" class="shared-section-title">推荐配置</div>
        <div v-if="result.recommendedConfig" class="rec-config">
          <div class="rec-item"><span class="rec-label">沙箱模式</span><el-tag size="small" effect="plain">{{ result.recommendedConfig.sandboxMode }}</el-tag></div>
          <div class="rec-item"><span class="rec-label">审批方式</span><el-tag size="small" effect="plain">{{ result.recommendedConfig.approvalPolicy }}</el-tag></div>
          <div class="rec-item"><span class="rec-label">网络访问</span><el-tag :type="result.recommendedConfig.networkAccess ? 'success' : 'danger'" size="small" effect="plain">{{ result.recommendedConfig.networkAccess ? '允许' : '禁止' }}</el-tag></div>
          <div class="rec-item"><span class="rec-label">允许删除</span><el-tag :type="result.recommendedConfig.allowDelete ? 'success' : 'danger'" size="small" effect="plain">{{ result.recommendedConfig.allowDelete ? '允许' : '禁止' }}</el-tag></div>
        </div>
      </div>

      <div v-else class="panel">
        <EmptyState title="请先评估权限" desc="配置左侧参数后点击评估" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, computed, inject, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Warning } from '@element-plus/icons-vue'
import RiskBadge from '../components/RiskBadge.vue'
import EmptyState from '../components/EmptyState.vue'
import ReportPanel from '../components/ReportPanel.vue'
import AiInsightPanel from '../components/AiInsightPanel.vue'
import { assessPermission } from '../api/risk'
import { explainRiskWithAi } from '../api/ai'
import type { PermissionAssessResultVO } from '../types/risk'
import type { AiRiskExplainVO } from '../types/ai'

const globalProjectId = inject<any>('globalProjectId', ref(null))
const globalProjectName = inject<any>('globalProjectName', ref(''))

const form = reactive({
  projectId: null as number | null,
  agentType: 'CLAUDE',
  taskType: 'BUG_FIX',
  sandboxMode: 'WORKSPACE_WRITE',
  approvalPolicy: 'ON_REQUEST',
  networkAccess: false,
  allowDelete: false
})

const assessing = ref(false)
const result = ref<PermissionAssessResultVO | null>(null)
const aiLoading = ref(false)
const aiResult = ref<AiRiskExplainVO | null>(null)

onMounted(() => {
  if (globalProjectId.value) form.projectId = globalProjectId.value
})

const isHighRisk = computed(() => {
  if (!result.value) return false
  return result.value.riskLevel === 'CRITICAL' || result.value.riskLevel === 'HIGH'
})

const scoreColor = computed(() => {
  if (!result.value) return '#94A3B8'
  const s = result.value.score
  if (s < 40) return '#10B981'
  if (s < 60) return '#F59E0B'
  if (s < 80) return '#F97316'
  return '#EF4444'
})

const handleAssess = async () => {
  if (!form.projectId) {
    ElMessage.warning('请先设置项目 ID')
    return
  }
  assessing.value = true
  try {
    const res = await assessPermission({ ...form } as any)
    if (res.data.code === 0) {
      result.value = res.data.data
      aiResult.value = null
      ElMessage.success('评估完成')
    } else {
      ElMessage.error(res.data.message)
    }
  } catch {
    ElMessage.error('评估失败')
  } finally {
    assessing.value = false
  }
}

watch(globalProjectId, (val) => {
  if (val && val !== form.projectId) {
    form.projectId = val
    result.value = null
    aiResult.value = null
  }
})

const handleAiExplain = async () => {
  if (!form.projectId || !result.value?.reportId) {
    ElMessage.warning('请先完成权限评估')
    return
  }
  aiLoading.value = true
  try {
    const res = await explainRiskWithAi({
      projectId: form.projectId,
      reportId: result.value.reportId
    })
    if (res.data.code === 0) {
      aiResult.value = res.data.data
      ElMessage.success('AI 风险解释已生成')
    } else {
      ElMessage.error(res.data.message)
    }
  } catch {
    ElMessage.error('AI 风险解释生成失败')
  } finally {
    aiLoading.value = false
  }
}
</script>

<style scoped>
.perm-page {
  display: grid;
  grid-template-columns: 380px 1fr;
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

.switch-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  font-size: 14px;
  color: #334155;
}

.risk-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 20px;
  background: rgba(239,68,68,0.06);
  border: 1px solid rgba(239,68,68,0.15);
  border-radius: 10px;
  color: #DC2626;
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 16px;
}

.score-area {
  display: flex;
  justify-content: center;
  padding: 20px 0 24px;
}

.score-circle {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  border: 4px solid;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.score-num {
  font-size: 36px;
  font-weight: 800;
  line-height: 1;
}

.score-label {
  font-size: 11px;
  color: #94A3B8;
  margin-top: 4px;
}

.rec-config {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.rec-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #F8FAFC;
  border-radius: 8px;
  font-size: 13px;
}

.rec-label { color: #64748B; }

.ai-action-row {
  margin-bottom: 12px;
}

@media (max-width: 900px) {
  .perm-page { grid-template-columns: 1fr; }
}
</style>
