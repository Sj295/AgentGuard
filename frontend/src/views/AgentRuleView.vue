<template>
  <div>
    <div class="page-intro">
      <div class="panel-header" style="margin-bottom: 8px;">Agent 规则生成</div>
      <p class="page-intro-desc">为 Codex / Claude Code / Cursor 自动生成项目上下文与安全规则文件。</p>
    </div>

    <div class="agent-grid">
      <!-- Left: Config -->
      <div class="panel">
        <div class="panel-header">生成配置</div>
        <el-form label-position="top" @submit.prevent>
          <el-form-item label="当前项目">
            <div class="project-display">{{ globalProjectName || ('#' + form.projectId) }}</div>
          </el-form-item>
          <el-form-item label="Agent 类型">
            <div class="agent-type-cards">
              <div
                v-for="at in agentTypes"
                :key="at.value"
                class="agent-type-card"
                :class="{ active: form.agentType === at.value }"
                @click="form.agentType = at.value"
              >
                <AgentIcon :type="at.value" :size="40" />
                <div class="agent-type-name">{{ at.label }}</div>
                <div class="agent-type-file">{{ at.file }}</div>
              </div>
            </div>
          </el-form-item>
          <el-button type="primary" :icon="Plus" :loading="generating" size="large" style="width: 100%" @click="handleGenerate">
            生成 Agent 规则
          </el-button>
        </el-form>
      </div>

      <!-- Right: Preview / List -->
      <div>
        <div v-if="selectedRule" class="panel shared-slide-up">
          <div class="panel-header">
            <span>规则预览</span>
            <div style="display: flex; gap: 8px;">
              <el-checkbox v-model="writeOpts.backup" label="备份" size="small" />
              <el-checkbox v-model="writeOpts.overwrite" label="覆盖已有文件" size="small" />
              <el-button type="primary" size="small" :loading="writing" @click="handleWrite(selectedRule!)">
                写入规则文件
              </el-button>
            </div>
          </div>
          <div class="rule-meta">
            <span class="rule-meta-item"><strong>文件名：</strong>{{ selectedRule.fileName }}</span>
            <span class="rule-meta-item"><strong>路径：</strong>{{ selectedRule.suggestedPath || '请重新生成以查看建议路径' }}</span>
          </div>
          <CodeBlock :code="selectedRule.content" :title="selectedRule.fileName" />
        </div>

        <div class="panel" style="margin-top: 16px;">
          <div class="panel-header">
            <span>已生成规则</span>
            <el-button text size="small" @click="loadRules">刷新</el-button>
          </div>
          <div v-if="rules.length" class="rule-list">
            <div
              v-for="rule in rules"
              :key="rule.id"
              class="rule-item"
              :class="{ active: selectedRule?.id === rule.id }"
              @click="selectedRule = rule"
            >
              <div class="rule-item-icon"><AgentIcon :type="rule.agentType" :size="28" /></div>
              <div class="rule-item-body">
                <div class="rule-item-name">{{ rule.fileName }}</div>
                <div class="rule-item-path">{{ rule.suggestedPath }}</div>
              </div>
              <div class="rule-item-time">{{ formatTime(rule.createdTime) }}</div>
            </div>
          </div>
          <EmptyState v-else title="暂无规则" desc="请先生成 Agent 规则" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, inject, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import CodeBlock from '../components/CodeBlock.vue'
import EmptyState from '../components/EmptyState.vue'
import AgentIcon from '../components/AgentIcon.vue'
import { generateAgentRule, getLatestAgentRules, writeAgentRule } from '../api/agentRule'
import type { AgentRuleVO } from '../types/agentRule'

const globalProjectId = inject<any>('globalProjectId', ref(null))
const globalProjectName = inject<any>('globalProjectName', ref(''))

const agentTypes = [
  { value: 'CODEX', label: 'Codex', file: 'AGENTS.md' },
  { value: 'CLAUDE', label: 'Claude Code', file: 'CLAUDE.md' },
  { value: 'CURSOR', label: 'Cursor', file: '.cursor/rules' }
]

const form = reactive({ projectId: null as number | null, agentType: 'CODEX' })
const rules = ref<AgentRuleVO[]>([])
const selectedRule = ref<AgentRuleVO | null>(null)
const generating = ref(false)
const writing = ref(false)
const writeOpts = reactive({ overwrite: false, backup: true })

const formatTime = (t: string) => new Date(t).toLocaleString('zh-CN')

const loadRules = async () => {
  if (!form.projectId) {
    rules.value = []
    selectedRule.value = null
    return
  }
  try {
    const res = await getLatestAgentRules(form.projectId)
    if (res.data.code === 0) {
      rules.value = res.data.data || []
      if (rules.value.length && !selectedRule.value) selectedRule.value = rules.value[0]
    }
  } catch { /* silent */ }
}

const handleGenerate = async () => {
  if (!form.projectId) {
    ElMessage.warning('请先扫描项目')
    return
  }
  generating.value = true
  try {
    const res = await generateAgentRule({ projectId: form.projectId, agentType: form.agentType as any })
    if (res.data.code === 0) {
      ElMessage.success('规则生成成功')
      await loadRules()
      selectedRule.value = res.data.data
    } else {
      ElMessage.error(res.data.message)
    }
  } catch {
    ElMessage.error('生成失败')
  } finally {
    generating.value = false
  }
}

const handleWrite = async (rule: AgentRuleVO) => {
  writing.value = true
  try {
    const res = await writeAgentRule(rule.id, { ...writeOpts })
    if (res.data.code === 0) {
      ElMessage.success(res.data.data.message || '写入成功')
    } else {
      ElMessage.error(res.data.message)
    }
  } catch {
    ElMessage.error('写入失败')
  } finally {
    writing.value = false
  }
}

onMounted(() => {
  if (globalProjectId.value) {
    form.projectId = globalProjectId.value
    loadRules()
  }
})

watch(globalProjectId, (val) => {
  if (val && val !== form.projectId) {
    form.projectId = val
    selectedRule.value = null
    loadRules()
  }
})
</script>

<style scoped>
.page-intro { margin-bottom: 20px; }
.page-intro-desc { font-size: 13px; color: #64748B; }

.project-display {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
  background: #F1F5F9;
  padding: 8px 12px;
  border-radius: 8px;
  width: 100%;
}

.agent-grid {
  display: grid;
  grid-template-columns: 380px 1fr;
  gap: 20px;
  align-items: start;
}

.agent-type-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.agent-type-card {
  text-align: center;
  padding: 14px 8px;
  border: 2px solid #E2E8F0;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.15s;
}

.agent-type-card:hover { border-color: #CBD5E1; }

.agent-type-card.active {
  border-color: #4F46E5;
  background: rgba(79,70,229,0.04);
}

.agent-type-icon { font-size: 24px; margin-bottom: 6px; }
.agent-type-name { font-size: 13px; font-weight: 600; color: #111827; }
.agent-type-file { font-size: 11px; color: #94A3B8; margin-top: 2px; }

.rule-meta {
  display: flex;
  gap: 20px;
  margin-bottom: 16px;
  font-size: 13px;
  color: #475569;
}

.rule-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.rule-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;
}

.rule-item:hover { background: #F8FAFC; }

.rule-item.active {
  background: rgba(79,70,229,0.06);
}

.rule-item-icon { font-size: 20px; flex-shrink: 0; }
.rule-item-body { flex: 1; min-width: 0; }
.rule-item-name { font-size: 13px; font-weight: 600; color: #111827; }
.rule-item-path { font-size: 11px; color: #94A3B8; margin-top: 1px; }
.rule-item-time { font-size: 11px; color: #94A3B8; flex-shrink: 0; }

@media (max-width: 900px) {
  .agent-grid { grid-template-columns: 1fr; }
}
</style>
