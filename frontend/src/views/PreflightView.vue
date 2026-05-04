<template>
  <div>
    <!-- Flow bar -->
    <div class="panel" style="margin-bottom: 20px; padding: 16px 24px;">
      <FlowSteps :steps="flowSteps" :active="4" />
    </div>

    <div class="pf-grid">
      <!-- Left: Config -->
      <div class="panel">
        <div class="panel-header">预执行检查配置</div>
        <el-form label-position="top" @submit.prevent>
          <el-form-item label="当前项目">
            <div class="project-display">{{ globalProjectName || ('#' + form.projectId) }}</div>
          </el-form-item>
          <div class="pf-row">
            <el-form-item label="Agent 类型" style="flex:1">
              <el-select v-model="form.agentType" style="width: 100%">
                <el-option label="Codex" value="CODEX" />
                <el-option label="Claude Code" value="CLAUDE" />
                <el-option label="Cursor" value="CURSOR" />
              </el-select>
            </el-form-item>
            <el-form-item label="任务类型" style="flex:1">
              <el-select v-model="form.taskType" style="width: 100%">
                <el-option label="前端重构" value="FRONTEND_REFACTOR" />
                <el-option label="Bug 修复" value="BUG_FIX" />
                <el-option label="大规模重构" value="LARGE_REFACTOR" />
                <el-option label="新功能" value="NEW_FEATURE" />
                <el-option label="测试编写" value="TEST_WRITING" />
                <el-option label="文档" value="DOCUMENTATION" />
              </el-select>
            </el-form-item>
          </div>
          <div class="pf-row">
            <el-form-item label="沙箱模式" style="flex:1">
              <el-select v-model="form.sandboxMode" style="width: 100%">
                <el-option label="仅限工作区写入" value="WORKSPACE_WRITE" />
                <el-option label="只读" value="READ_ONLY" />
                <el-option label="完全访问 (有风险)" value="DANGER_FULL_ACCESS" />
              </el-select>
            </el-form-item>
            <el-form-item label="审批方式" style="flex:1">
              <el-select v-model="form.approvalPolicy" style="width: 100%">
                <el-option label="按需审批" value="ON_REQUEST" />
                <el-option label="自动批准" value="AUTO_APPROVE" />
                <el-option label="无需审批" value="NEVER" />
              </el-select>
            </el-form-item>
          </div>
          <div class="switch-row">
            <span>网络访问</span>
            <el-switch v-model="form.networkAccess" />
          </div>
          <div class="switch-row">
            <span>允许删除</span>
            <el-switch v-model="form.allowDelete" />
          </div>
          <el-form-item label="计划命令" style="margin-top: 8px">
            <el-input v-model="plannedText" type="textarea" :rows="4" placeholder="每行一个命令（可选）" />
          </el-form-item>
          <el-button type="primary" :icon="CircleCheck" :loading="checking" size="large" style="width: 100%" @click="handleCheck">
            执行预执行检查
          </el-button>
        </el-form>
      </div>

      <!-- Right: Result -->
      <div>
        <div v-if="result" class="panel shared-slide-up">
          <!-- Decision banner -->
          <div class="decision-banner" :class="result.allowedToProceed ? 'decision-ok' : 'decision-block'">
            <div class="decision-icon">
              <el-icon :size="32"><CircleCheckFilled v-if="result.allowedToProceed" /><CircleCloseFilled v-else /></el-icon>
            </div>
            <div>
              <div class="decision-title">{{ result.allowedToProceed ? '可以继续执行' : '不建议执行' }}</div>
              <div class="decision-sub">
                风险等级 <RiskBadge :level="result.overallRiskLevel" size="small" /> · 分数 {{ result.score }}
              </div>
            </div>
          </div>

          <!-- Check items -->
          <div class="shared-section-title">检查项</div>
          <div class="check-list">
            <div v-for="(item, i) in result.checkItems" :key="i" class="check-item">
              <div class="check-status" :class="`check-${item.status}`">
                <el-icon :size="16"><CircleCheck v-if="item.status==='PASS'" /><WarningFilled v-else-if="item.status==='WARN'" /><CircleClose v-else /></el-icon>
              </div>
              <div class="check-body">
                <div class="check-name">{{ item.name }}</div>
                <div class="check-msg">{{ item.message }}</div>
              </div>
            </div>
          </div>

          <ReportPanel v-if="result.riskItems?.length" title="风险项" :items="result.riskItems" type="warn" style="margin-top: 16px; margin-bottom: 12px;" />
          <ReportPanel v-if="result.suggestions?.length" title="建议" :items="result.suggestions" type="info" style="margin-bottom: 12px;" />

          <div v-if="result.recommendedActions?.length" class="shared-section-title">推荐操作</div>
          <div v-if="result.recommendedActions?.length" class="action-checklist">
            <div v-for="(a, i) in result.recommendedActions" :key="i" class="action-check-item">
              <el-icon :size="14" color="#4F46E5"><CircleCheck /></el-icon>
              <span>{{ a }}</span>
            </div>
          </div>
        </div>

        <div v-else class="panel">
          <EmptyState title="执行预执行检查" desc="配置左侧参数后，点击执行全面安全检查" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, inject, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  CircleCheck, CircleCheckFilled, CircleClose, CircleCloseFilled,
  WarningFilled
} from '@element-plus/icons-vue'
import RiskBadge from '../components/RiskBadge.vue'
import EmptyState from '../components/EmptyState.vue'
import FlowSteps from '../components/FlowSteps.vue'
import ReportPanel from '../components/ReportPanel.vue'
import { runPreflightCheck } from '../api/preflight'
import type { PreflightCheckVO } from '../types/preflight'

const globalProjectId = inject<any>('globalProjectId', ref(null))
const globalProjectName = inject<any>('globalProjectName', ref(''))

const flowSteps = ['项目扫描', '规则生成', '权限评估', '命令审计', '预执行检查', 'Git 变更审计', '安全报告']

const form = reactive({
  projectId: null as number | null,
  agentType: 'CLAUDE',
  taskType: 'BUG_FIX',
  sandboxMode: 'WORKSPACE_WRITE',
  approvalPolicy: 'ON_REQUEST',
  networkAccess: false,
  allowDelete: false
})

const plannedText = ref('')
const checking = ref(false)
const result = ref<PreflightCheckVO | null>(null)

onMounted(() => {
  if (globalProjectId.value) form.projectId = globalProjectId.value
})

const handleCheck = async () => {
  if (!form.projectId) {
    ElMessage.warning('请先设置项目 ID')
    return
  }
  checking.value = true
  try {
    const cmds = plannedText.value.split('\n').map(s => s.trim()).filter(s => s.length > 0)
    const res = await runPreflightCheck({ ...form, plannedCommands: cmds } as any)
    if (res.data.code === 0) {
      result.value = res.data.data
      ElMessage.success('检查完成')
    } else {
      ElMessage.error(res.data.message)
    }
  } catch {
    ElMessage.error('检查失败')
  } finally {
    checking.value = false
  }
}

watch(globalProjectId, (val) => {
  if (val && val !== form.projectId) {
    form.projectId = val
    result.value = null
  }
})
</script>

<style scoped>
.pf-grid {
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

.pf-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.switch-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
  font-size: 14px;
  color: #334155;
}

/* Decision banner */
.decision-banner {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  border-radius: 12px;
  margin-bottom: 20px;
}

.decision-ok {
  background: rgba(16,185,129,0.06);
  border: 1px solid rgba(16,185,129,0.15);
  color: #059669;
}

.decision-block {
  background: rgba(239,68,68,0.06);
  border: 1px solid rgba(239,68,68,0.15);
  color: #DC2626;
}

.decision-title {
  font-size: 20px;
  font-weight: 700;
}

.decision-sub {
  margin-top: 4px;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 8px;
}

/* Check items */
.check-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.check-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  background: #F8FAFC;
  border-radius: 8px;
  border: 1px solid #F1F5F9;
}

.check-status {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.check-PASS { background: rgba(16,185,129,0.1); color: #10B981; }
.check-WARN { background: rgba(245,158,11,0.1); color: #F59E0B; }
.check-FAIL { background: rgba(239,68,68,0.1); color: #EF4444; }

.check-name { font-size: 13px; font-weight: 600; color: #111827; }
.check-msg { font-size: 12px; color: #64748B; margin-top: 2px; }

.action-checklist {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.action-check-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #475569;
}

@media (max-width: 900px) {
  .pf-grid { grid-template-columns: 1fr; }
}
</style>
