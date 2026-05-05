<template>
  <div class="dashboard">
    <HeroBanner />

    <StatusInfoBar :last-scan-time="overview?.latestScanTime" />

    <!-- Stat Cards -->
    <div class="stats-grid">
      <StatCard
        :icon="Folder"
        title="安全事件计数"
        :value="overview?.totalEvents ?? 0"
        subtitle="安全事件"
        color="indigo"
      />
      <StatCard
        :icon="Warning"
        title="风险概览"
        :value="latestRiskLabel"
        :subtitle="riskSubtitle"
        color="red"
      />
      <StatCard
        :icon="Document"
        title="规则状态"
        :value="ruleCount"
        subtitle="Agent 规则"
        color="cyan"
      />
      <StatCard
        :icon="Clock"
        title="最近扫描时间"
        :value="latestScanTime"
        subtitle="最近扫描时间"
        color="slate"
      />
    </div>

    <!-- Risk + Quick Actions -->
    <div class="content-grid">
      <div class="panel">
        <div class="panel-header">
          <span class="panel-title">风险分布图</span>
          <el-select v-model="projectId" size="small" style="width: 180px" placeholder="选择项目" @change="handleProjectChange" v-if="projectList.length">
            <el-option v-for="p in projectList" :key="p.id" :label="p.projectName" :value="p.id" />
          </el-select>
        </div>
        <div v-if="overview" class="risk-chart">
          <div class="risk-bar-row">
            <span class="risk-label-crit">CRITICAL</span>
            <div class="risk-track"><div class="risk-fill" :style="{ width: calcPercent(overview.criticalCount) + '%', background: '#EF4444' }"></div></div>
            <span class="risk-count">{{ overview.criticalCount }}</span>
          </div>
          <div class="risk-bar-row">
            <span class="risk-label-high">HIGH</span>
            <div class="risk-track"><div class="risk-fill" :style="{ width: calcPercent(overview.highCount) + '%', background: '#F59E0B' }"></div></div>
            <span class="risk-count">{{ overview.highCount }}</span>
          </div>
          <div class="risk-bar-row">
            <span class="risk-label-med">MEDIUM</span>
            <div class="risk-track"><div class="risk-fill" :style="{ width: calcPercent(overview.mediumCount) + '%', background: '#FBBF24' }"></div></div>
            <span class="risk-count">{{ overview.mediumCount }}</span>
          </div>
          <div class="risk-bar-row">
            <span class="risk-label-low">LOW</span>
            <div class="risk-track"><div class="risk-fill" :style="{ width: calcPercent(overview.lowCount) + '%', background: '#10B981' }"></div></div>
            <span class="risk-count">{{ overview.lowCount }}</span>
          </div>
        </div>
        <EmptyState v-else title="暂无安全概览" desc="请先扫描项目以获取安全数据" />
      </div>

      <div class="panel">
        <div class="panel-header">
          <span class="panel-title">快速操作</span>
        </div>
        <QuickActionPanel />
      </div>
    </div>

    <!-- Recent Events -->
    <RecentEventsList :events="recentEvents" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, inject, watch } from 'vue'
import { Folder, Warning, Document, Clock } from '@element-plus/icons-vue'
import HeroBanner from '../components/HeroBanner.vue'
import StatusInfoBar from '../components/StatusInfoBar.vue'
import StatCard from '../components/StatCard.vue'
import QuickActionPanel from '../components/QuickActionPanel.vue'
import RecentEventsList from '../components/RecentEventsList.vue'
import EmptyState from '../components/EmptyState.vue'
import { getSecurityOverview, getTimelineEvents } from '../api/timeline'
import { getLatestAgentRules } from '../api/agentRule'
import { getAllProjects } from '../api/project'
import type { ProjectSecurityOverviewVO, TimelineEventVO } from '../types/timeline'
import type { ProjectInfoVO } from '../api/project'

const globalProjectId = inject<any>('globalProjectId', ref(null))
const globalProjectName = inject<any>('globalProjectName', ref(''))
const hasScannedProject = inject<any>('hasScannedProject', ref(false))
const projectId = ref<number | null>(null)
const projectList = ref<ProjectInfoVO[]>([])
const overview = ref<ProjectSecurityOverviewVO | null>(null)
const recentEvents = ref<TimelineEventVO[]>([])
const ruleCount = ref(0)

const latestRiskLabel = computed(() => {
  const map: Record<string, string> = { CRITICAL: '严重', HIGH: 'High Risk', MEDIUM: '中风险', LOW: '低风险' }
  return overview.value?.latestRiskLevel ? map[overview.value.latestRiskLevel] || '—' : '—'
})

const riskSubtitle = computed(() => {
  if (!overview.value) return '—'
  return `${overview.value.criticalCount} Critical, ${overview.value.highCount} High`
})

const latestScanTime = computed(() => {
  if (!overview.value?.latestScanTime) return '—'
  return new Date(overview.value.latestScanTime).toLocaleString('zh-CN')
})

const calcPercent = (count: number) => {
  if (!overview.value || overview.value.totalEvents === 0) return 0
  return Math.round((count / overview.value.totalEvents) * 100)
}

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
    const selectedProject = projectList.value.find((project) => project.id === val)
    globalProjectId.value = val
    globalProjectName.value = selectedProject?.projectName || ''
    hasScannedProject.value = true
    localStorage.setItem('agentguard_projectId', String(val))
    if (selectedProject?.projectName) {
      localStorage.setItem('agentguard_projectName', selectedProject.projectName)
    }
    localStorage.setItem('agentguard_hasScanned', 'true')
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
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.content-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
}

.panel {
  background: #fff;
  border: 1px solid #E5E7EB;
  border-radius: 14px;
  padding: 20px 24px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.panel-title {
  font-size: 15px;
  font-weight: 700;
  color: #111827;
}

/* Risk chart */
.risk-chart {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 4px 0;
}

.risk-bar-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.risk-label-crit,
.risk-label-high,
.risk-label-med,
.risk-label-low {
  width: 72px;
  font-size: 11px;
  font-weight: 700;
  text-align: right;
  flex-shrink: 0;
  letter-spacing: 0.5px;
}

.risk-label-crit { color: #EF4444; }
.risk-label-high { color: #F59E0B; }
.risk-label-med  { color: #D97706; }
.risk-label-low  { color: #10B981; }

.risk-track {
  flex: 1;
  height: 10px;
  background: #F1F5F9;
  border-radius: 5px;
  overflow: hidden;
}

.risk-fill {
  height: 100%;
  border-radius: 5px;
  transition: width 0.6s ease;
  min-width: 2px;
}

.risk-count {
  width: 32px;
  font-size: 14px;
  font-weight: 700;
  color: #111827;
  text-align: right;
  flex-shrink: 0;
}

@media (max-width: 1200px) {
  .stats-grid { grid-template-columns: repeat(2, 1fr); }
  .content-grid { grid-template-columns: 1fr; }
}

@media (max-width: 640px) {
  .stats-grid { grid-template-columns: 1fr; }
}
</style>
