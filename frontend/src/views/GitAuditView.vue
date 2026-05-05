<template>
  <div>
    <div class="git-grid">
      <!-- Left: Control -->
      <div class="panel">
        <div class="panel-header">Git 变更审计</div>
        <p class="git-desc">审计项目未提交的 Git 变更，检测风险文件修改并生成回滚命令。</p>
        <el-form label-position="top" @submit.prevent>
          <el-form-item label="当前项目">
            <div class="project-display">{{ globalProjectName || ('#' + projectId) }}</div>
          </el-form-item>
          <el-button type="primary" :icon="Search" :loading="auditing" size="large" style="width: 100%" @click="handleAudit">
            审计变更
          </el-button>
        </el-form>

        <div v-if="result" class="git-stats">
          <div class="git-stat" style="border-top-color: #10B981;">
            <div class="git-stat-count">{{ result.addedFiles?.length || 0 }}</div>
            <div class="git-stat-label">新增</div>
          </div>
          <div class="git-stat" style="border-top-color: #F59E0B;">
            <div class="git-stat-count">{{ result.modifiedFiles?.length || 0 }}</div>
            <div class="git-stat-label">修改</div>
          </div>
          <div class="git-stat" style="border-top-color: #EF4444;">
            <div class="git-stat-count">{{ result.deletedFiles?.length || 0 }}</div>
            <div class="git-stat-label">删除</div>
          </div>
        </div>
      </div>

      <!-- Right: Result -->
      <div>
        <div v-if="result" class="panel shared-slide-up">
          <div class="panel-header">
            <span>审计结果</span>
            <RiskBadge :level="result.riskLevel" size="large" />
          </div>

          <div v-if="result.addedFiles?.length" class="shared-section-title" style="color: #10B981;">新增文件</div>
          <div v-if="result.addedFiles?.length" class="file-tags">
            <el-tag v-for="f in result.addedFiles" :key="f" effect="plain" size="small" type="success">{{ f }}</el-tag>
          </div>

          <div v-if="result.modifiedFiles?.length" class="shared-section-title" style="color: #F59E0B;">修改文件</div>
          <div v-if="result.modifiedFiles?.length" class="file-tags">
            <el-tag v-for="f in result.modifiedFiles" :key="f" effect="plain" size="small" type="warning">{{ f }}</el-tag>
          </div>

          <div v-if="result.deletedFiles?.length" class="shared-section-title" style="color: #EF4444;">删除文件</div>
          <div v-if="result.deletedFiles?.length" class="file-tags">
            <el-tag v-for="f in result.deletedFiles" :key="f" effect="plain" size="small" type="danger">{{ f }}</el-tag>
          </div>

          <ReportPanel v-if="result.riskItems?.length" title="风险项" :items="result.riskItems" type="warn" style="margin-top: 16px; margin-bottom: 12px;" />

          <div v-if="result.rollbackCommands?.length" class="shared-section-title">回滚命令</div>
          <div v-if="result.rollbackCommands?.length" style="margin-bottom: 12px;">
            <CodeBlock :code="result.rollbackCommands.map(c => '$ ' + c).join('\n')" title="rollback.sh" />
          </div>

          <ReportPanel v-if="result.suggestions?.length" title="建议" :items="result.suggestions" type="info" />

          <div class="ai-action-row">
            <el-button type="primary" plain :loading="aiLoading" @click="handleAiAnalyze">
              生成 AI 影响分析
            </el-button>
          </div>
          <AiInsightPanel
            v-if="aiResult"
            :mocked="aiResult.mocked"
            :confidence-note="aiResult.confidenceNote"
            summary-title="影响摘要"
            :summary="aiResult.summary"
            :sections="[
              { title: '影响范围', items: aiResult.impactAreas },
              { title: '测试建议', items: aiResult.testSuggestions },
              { title: '回滚建议', items: aiResult.rollbackSuggestions }
            ]"
          />
        </div>

        <div v-else-if="cleanWorkspace" class="panel">
          <div class="clean-state">
            <el-icon :size="48" color="#10B981"><CircleCheckFilled /></el-icon>
            <div class="clean-title">没有未提交的变更</div>
            <div class="clean-desc">暂存区和工作区均无待提交内容</div>
          </div>
        </div>

        <div v-else class="panel">
          <EmptyState title="审计 Git 变更" desc="输入项目 ID 并点击审计" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, inject, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, CircleCheckFilled } from '@element-plus/icons-vue'
import RiskBadge from '../components/RiskBadge.vue'
import CodeBlock from '../components/CodeBlock.vue'
import EmptyState from '../components/EmptyState.vue'
import ReportPanel from '../components/ReportPanel.vue'
import AiInsightPanel from '../components/AiInsightPanel.vue'
import { auditGitDiff } from '../api/gitAudit'
import { analyzeGitDiffWithAi } from '../api/ai'
import type { GitDiffAuditVO } from '../types/gitAudit'
import type { AiGitDiffAnalysisVO } from '../types/ai'

const globalProjectId = inject<any>('globalProjectId', ref(null))
const globalProjectName = inject<any>('globalProjectName', ref(''))
const projectId = ref<number | null>(null)
const auditing = ref(false)
const result = ref<GitDiffAuditVO | null>(null)
const cleanWorkspace = ref(false)
const aiLoading = ref(false)
const aiResult = ref<AiGitDiffAnalysisVO | null>(null)

onMounted(() => {
  if (globalProjectId.value) projectId.value = globalProjectId.value
})

watch(globalProjectId, (val) => {
  if (val && val !== projectId.value) {
    projectId.value = val
    result.value = null
    cleanWorkspace.value = false
    aiResult.value = null
  }
})

const handleAudit = async () => {
  if (!projectId.value) { ElMessage.warning('请先扫描项目'); return }
  auditing.value = true
  cleanWorkspace.value = false
  result.value = null
  aiResult.value = null
  try {
    const res = await auditGitDiff({ projectId: projectId.value })
    if (res.data.code === 0) {
      result.value = res.data.data
      const total = (result.value.addedFiles?.length || 0) + (result.value.modifiedFiles?.length || 0) + (result.value.deletedFiles?.length || 0)
      if (total === 0) cleanWorkspace.value = true
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

const handleAiAnalyze = async () => {
  if (!projectId.value || !result.value?.reportId) {
    ElMessage.warning('请先完成 Git 审计')
    return
  }
  aiLoading.value = true
  try {
    const res = await analyzeGitDiffWithAi({
      projectId: projectId.value,
      gitAuditReportId: result.value.reportId
    })
    if (res.data.code === 0) {
      aiResult.value = res.data.data
      ElMessage.success('AI 分析已生成')
    } else {
      ElMessage.error(res.data.message)
    }
  } catch {
    ElMessage.error('AI 分析生成失败')
  } finally {
    aiLoading.value = false
  }
}
</script>

<style scoped>
.git-grid {
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

.git-desc { font-size: 13px; color: #64748B; margin-bottom: 16px; }

.git-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-top: 20px;
}

.git-stat {
  text-align: center;
  padding: 14px 8px;
  background: #F8FAFC;
  border-radius: 8px;
  border-top: 3px solid;
}

.git-stat-count { font-size: 24px; font-weight: 700; color: #111827; }
.git-stat-label { font-size: 12px; color: #64748B; margin-top: 2px; }

.file-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.clean-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 20px;
  text-align: center;
}

.clean-title { font-size: 18px; font-weight: 600; color: #111827; margin-top: 16px; }
.clean-desc { font-size: 13px; color: #94A3B8; margin-top: 6px; }

.ai-action-row {
  margin-top: 16px;
}

@media (max-width: 900px) {
  .git-grid { grid-template-columns: 1fr; }
}
</style>
