<template>
  <div>
    <!-- Hero -->
    <div class="hero">
      <div class="hero-left">
        <div class="hero-eyebrow">AgentGuard Agent Safety Console</div>
        <h1 class="hero-title">在让 AI Agent 修改代码前<br/>先完成安全预检</h1>
        <p class="hero-desc">
          为 Codex / Claude Code / Cursor 提供项目扫描、权限评估、命令审计、Preflight 检查和 Git Diff 审计。
        </p>
        <div class="hero-actions">
          <el-button type="primary" size="large" :icon="Search" @click="$router.push('/scan')">
            扫描项目
          </el-button>
          <el-button size="large" :icon="CircleCheck" @click="$router.push('/preflight')">
            Preflight 检查
          </el-button>
        </div>
      </div>
      <div class="hero-right">
        <div class="flow-panel">
          <div class="flow-panel-title">Agent 安全执行流程</div>
          <FlowSteps :steps="flowSteps" :active="0" />
        </div>
      </div>
    </div>

    <!-- Metrics -->
    <div class="metrics-grid">
      <MetricCard :icon="Folder" :value="String(overview?.totalEvents || 0)" label="安全事件总数" color="indigo" />
      <MetricCard :icon="Warning" :value="latestRiskLabel" label="当前最高风险" :color="latestRiskColor" />
      <MetricCard :icon="Document" :value="String(ruleCount)" label="Agent 规则数量" color="cyan" />
      <MetricCard :icon="Clock" :value="latestScanTime" label="最近扫描时间" color="emerald" />
    </div>

    <!-- Risk + Actions -->
    <div class="content-grid">
      <div class="panel">
        <div class="panel-header">
          <span>风险分布</span>
          <el-select v-model="projectId" size="small" style="width: 160px" placeholder="选择项目" @change="handleProjectChange">
            <el-option
              v-for="p in projectList"
              :key="p.id"
              :label="p.projectName"
              :value="p.id"
            />
          </el-select>
        </div>
        <div v-if="overview" class="risk-bars">
          <div class="risk-bar-row">
            <span class="risk-bar-label risk-critical">CRITICAL</span>
            <div class="risk-bar-track"><div class="risk-bar-fill" :style="{ width: calcPercent(overview.criticalCount) + '%', background: '#EF4444' }"></div></div>
            <span class="risk-bar-count">{{ overview.criticalCount }}</span>
          </div>
          <div class="risk-bar-row">
            <span class="risk-bar-label risk-high">HIGH</span>
            <div class="risk-bar-track"><div class="risk-bar-fill" :style="{ width: calcPercent(overview.highCount) + '%', background: '#F59E0B' }"></div></div>
            <span class="risk-bar-count">{{ overview.highCount }}</span>
          </div>
          <div class="risk-bar-row">
            <span class="risk-bar-label" style="color: #D97706">MEDIUM</span>
            <div class="risk-bar-track"><div class="risk-bar-fill" :style="{ width: calcPercent(overview.mediumCount) + '%', background: '#FBBF24' }"></div></div>
            <span class="risk-bar-count">{{ overview.mediumCount }}</span>
          </div>
          <div class="risk-bar-row">
            <span class="risk-bar-label risk-low">LOW</span>
            <div class="risk-bar-track"><div class="risk-bar-fill" :style="{ width: calcPercent(overview.lowCount) + '%', background: '#10B981' }"></div></div>
            <span class="risk-bar-count">{{ overview.lowCount }}</span>
          </div>
        </div>
        <EmptyState v-else title="暂无安全概览" desc="请先扫描项目以获取安全数据" />
      </div>

      <div class="panel">
        <div class="panel-header">快速操作</div>
        <div class="action-list">
          <div class="action-item" @click="$router.push('/scan')">
            <div class="action-icon" style="background: rgba(79,70,229,0.08); color: #4F46E5;"><el-icon :size="18"><Search /></el-icon></div>
            <div class="action-text"><div class="action-name">扫描项目</div><div class="action-desc">识别技术栈与风险</div></div>
          </div>
          <div class="action-item" @click="$router.push('/agent-rules')">
            <div class="action-icon" style="background: rgba(6,182,212,0.08); color: #06B6D4;"><el-icon :size="18"><Document /></el-icon></div>
            <div class="action-text"><div class="action-name">生成 Agent 规则</div><div class="action-desc">为 Agent 生成项目安全规则</div></div>
          </div>
          <div class="action-item" @click="$router.push('/permission')">
            <div class="action-icon" style="background: rgba(245,158,11,0.08); color: #F59E0B;"><el-icon :size="18"><Lock /></el-icon></div>
            <div class="action-text"><div class="action-name">权限评估</div><div class="action-desc">评估权限配置风险</div></div>
          </div>
          <div class="action-item" @click="$router.push('/command-audit')">
            <div class="action-icon" style="background: rgba(239,68,68,0.08); color: #EF4444;"><el-icon :size="18"><Warning /></el-icon></div>
            <div class="action-text"><div class="action-name">命令审计</div><div class="action-desc">检测危险命令</div></div>
          </div>
          <div class="action-item" @click="$router.push('/git-audit')">
            <div class="action-icon" style="background: rgba(16,185,129,0.08); color: #10B981;"><el-icon :size="18"><Share /></el-icon></div>
            <div class="action-text"><div class="action-name">Git 变更审计</div><div class="action-desc">审计未提交变更</div></div>
          </div>
          <div class="action-item" @click="$router.push('/reports')">
            <div class="action-icon" style="background: rgba(99,102,241,0.08); color: #6366F1;"><el-icon :size="18"><DataAnalysis /></el-icon></div>
            <div class="action-text"><div class="action-name">安全报告</div><div class="action-desc">生成 Markdown 报告</div></div>
          </div>
        </div>
      </div>
    </div>

    <!-- Recent Events -->
    <div class="panel">
      <div class="panel-header">
        <span>最近安全事件</span>
        <el-button text type="primary" size="small" @click="$router.push('/timeline')">查看全部</el-button>
      </div>
      <div v-if="recentEvents.length" class="event-list">
        <div v-for="event in recentEvents" :key="`${event.sourceType}-${event.sourceId}-${event.createdTime}`" class="event-item" @click="$router.push('/timeline')">
          <div class="event-dot" :class="`dot-${event.riskLevel}`"></div>
          <div class="event-body">
            <div class="event-name">{{ event.eventName }}</div>
            <div class="event-summary">{{ event.summary }}</div>
          </div>
          <div class="event-meta">
            <RiskBadge :level="event.riskLevel" size="small" />
            <span class="event-time">{{ formatTime(event.createdTime) }}</span>
          </div>
        </div>
      </div>
      <EmptyState v-else title="暂无安全事件" desc="扫描项目后将自动生成安全事件" />
    </div>

    <div class="ai-dashboard-block">
      <AiRuntimeStatus />
      <AiAnalysisHistoryPanel :project-id="projectId" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, inject, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Folder, Warning, Document, Clock,
  Search, CircleCheck, Lock, Share, DataAnalysis
} from '@element-plus/icons-vue'
import RiskBadge from '../components/RiskBadge.vue'
import EmptyState from '../components/EmptyState.vue'
import MetricCard from '../components/MetricCard.vue'
import FlowSteps from '../components/FlowSteps.vue'
import AiAnalysisHistoryPanel from '../components/AiAnalysisHistoryPanel.vue'
import AiRuntimeStatus from '../components/AiRuntimeStatus.vue'
import { getSecurityOverview, getTimelineEvents } from '../api/timeline'
import { getLatestAgentRules } from '../api/agentRule'
import { getAllProjects } from '../api/project'
import type { ProjectSecurityOverviewVO, TimelineEventVO } from '../types/timeline'
import type { ProjectInfoVO } from '../api/project'

const globalProjectId = inject<any>('globalProjectId', ref(null))
const hasScannedProject = inject<any>('hasScannedProject', ref(false))
const projectId = ref<number | null>(null)
const projectList = ref<ProjectInfoVO[]>([])
const overview = ref<ProjectSecurityOverviewVO | null>(null)
const recentEvents = ref<TimelineEventVO[]>([])
const ruleCount = ref(0)

const flowSteps = ['项目扫描', '规则生成', '权限评估', '命令审计', '预执行检查', 'Git 变更审计', '安全报告']

const latestRiskLabel = computed(() => {
  const map: Record<string, string> = { CRITICAL: '严重', HIGH: '高风险', MEDIUM: '中风险', LOW: '低风险' }
  return overview.value?.latestRiskLevel ? map[overview.value.latestRiskLevel] || '—' : '—'
})

const latestRiskColor = computed(() => {
  const level = overview.value?.latestRiskLevel
  if (level === 'CRITICAL' || level === 'HIGH') return 'red'
  if (level === 'MEDIUM') return 'amber'
  return 'emerald'
})

const latestScanTime = computed(() => {
  if (!overview.value?.latestScanTime) return '—'
  return new Date(overview.value.latestScanTime).toLocaleString('zh-CN')
})

const calcPercent = (count: number) => {
  if (!overview.value || overview.value.totalEvents === 0) return 0
  return Math.round((count / overview.value.totalEvents) * 100)
}

const formatTime = (time: string) => new Date(time).toLocaleString('zh-CN')

const loadOverview = async () => {
  if (!projectId.value) return
  try {
    const res = await getSecurityOverview(projectId.value)
    if (res.data.code === 0) overview.value = res.data.data
  } catch { /* silent */ }
}

const loadRecentEvents = async () => {
  if (!projectId.value) return
  try {
    const res = await getTimelineEvents(projectId.value, 1, 5)
    if (res.data.code === 0) recentEvents.value = res.data.data.records
  } catch { /* silent */ }
}

const loadRuleCount = async () => {
  if (!projectId.value) return
  try {
    const res = await getLatestAgentRules(projectId.value)
    if (res.data.code === 0) ruleCount.value = res.data.data?.length || 0
  } catch { /* silent */ }
}

const loadProjectList = async () => {
  try {
    const res = await getAllProjects(1, 100)
    if (res.data.code === 0) {
      projectList.value = res.data.data.records || []
    }
  } catch { /* silent */ }
}

const loadAll = async () => {
  if (!projectId.value) return
  await Promise.all([loadOverview(), loadRecentEvents(), loadRuleCount()])
}

const handleProjectChange = (val: number) => {
  if (val) {
    globalProjectId.value = val
    localStorage.setItem('agentguard_projectId', String(val))
    loadAll()
  }
}

watch(globalProjectId, (val) => {
  if (val && hasScannedProject.value && val !== projectId.value) {
    projectId.value = val
    loadAll()
  }
})

onMounted(async () => {
  await loadProjectList()
  if (hasScannedProject.value && globalProjectId.value) {
    projectId.value = globalProjectId.value
    loadAll()
  }
})
</script>

<style scoped>
/* Hero */
.hero {
  display: flex;
  gap: 32px;
  margin-bottom: 28px;
  align-items: stretch;
}

.hero-left {
  flex: 1;
  background: linear-gradient(135deg, #0F172A 0%, #1E293B 100%);
  border-radius: 16px;
  padding: 36px;
  color: #fff;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.hero-eyebrow {
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 1.5px;
  color: #818CF8;
  margin-bottom: 12px;
}

.hero-title {
  font-size: 26px;
  font-weight: 700;
  line-height: 1.3;
  margin-bottom: 12px;
}

.hero-desc {
  font-size: 14px;
  color: #94A3B8;
  line-height: 1.6;
  margin-bottom: 24px;
  max-width: 480px;
}

.hero-actions {
  display: flex;
  gap: 12px;
}

.hero-right {
  width: 360px;
  flex-shrink: 0;
}

.flow-panel {
  background: #ffffff;
  border: 1px solid #E2E8F0;
  border-radius: 16px;
  padding: 24px;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.flow-panel-title {
  font-size: 13px;
  font-weight: 600;
  color: #64748B;
  margin-bottom: 20px;
}

/* Metrics */
.metrics-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 28px;
}

/* Content grid */
.content-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
}

/* Risk bars */
.risk-bars {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.risk-bar-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.risk-bar-label {
  width: 70px;
  font-size: 11px;
  font-weight: 700;
  text-align: right;
  flex-shrink: 0;
  letter-spacing: 0.5px;
}

.risk-bar-track {
  flex: 1;
  height: 8px;
  background: #F1F5F9;
  border-radius: 4px;
  overflow: hidden;
}

.risk-bar-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.6s ease;
  min-width: 2px;
}

.risk-bar-count {
  width: 32px;
  font-size: 14px;
  font-weight: 700;
  color: #111827;
  text-align: right;
  flex-shrink: 0;
}

/* Action list */
.action-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.action-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;
}

.action-item:hover {
  background: #F8FAFC;
}

.action-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.action-name {
  font-size: 13px;
  font-weight: 600;
  color: #111827;
}

.action-desc {
  font-size: 11px;
  color: #94A3B8;
  margin-top: 1px;
}

/* Event list */
.event-list {
  display: flex;
  flex-direction: column;
}

.event-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 0;
  border-bottom: 1px solid #F1F5F9;
  cursor: pointer;
  transition: background 0.1s;
}

.event-item:last-child {
  border-bottom: none;
}

.event-item:hover {
  background: #F8FAFC;
  margin: 0 -12px;
  padding-left: 12px;
  padding-right: 12px;
  border-radius: 8px;
}

.event-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.dot-CRITICAL { background: #EF4444; }
.dot-HIGH { background: #F59E0B; }
.dot-MEDIUM { background: #FBBF24; }
.dot-LOW { background: #10B981; }

.event-body {
  flex: 1;
  min-width: 0;
}

.event-name {
  font-size: 13px;
  font-weight: 600;
  color: #111827;
}

.event-summary {
  font-size: 12px;
  color: #94A3B8;
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.event-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.event-time {
  font-size: 11px;
  color: #94A3B8;
}

.ai-dashboard-block {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

@media (max-width: 1200px) {
  .hero { flex-direction: column; }
  .hero-right { width: 100%; }
  .metrics-grid { grid-template-columns: repeat(2, 1fr); }
  .content-grid { grid-template-columns: 1fr; }
}
</style>
