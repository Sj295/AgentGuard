<template>
  <div>
    <!-- Overview cards -->
    <div class="tl-overview">
      <div class="tl-ov-card">
        <div class="tl-ov-value">{{ total }}</div>
        <div class="tl-ov-label">总事件数</div>
      </div>
      <div class="tl-ov-card tl-ov-critical">
        <div class="tl-ov-value">{{ criticalCount }}</div>
        <div class="tl-ov-label">严重</div>
      </div>
      <div class="tl-ov-card tl-ov-high">
        <div class="tl-ov-value">{{ highCount }}</div>
        <div class="tl-ov-label">高风险</div>
      </div>
      <div class="tl-ov-card tl-ov-medium">
        <div class="tl-ov-value">{{ mediumCount }}</div>
        <div class="tl-ov-label">中风险</div>
      </div>
      <div class="tl-ov-card tl-ov-low">
        <div class="tl-ov-value">{{ lowCount }}</div>
        <div class="tl-ov-label">低风险</div>
      </div>
    </div>

    <!-- Filter + Timeline -->
    <div class="panel">
      <div class="panel-header">
        <span>安全事件时间线</span>
        <div style="display: flex; gap: 8px; align-items: center;">
          <el-select v-model="filters.riskLevel" placeholder="全部等级" clearable size="small" style="width: 120px" @change="handleSearch">
            <el-option label="全部" value="" />
            <el-option label="严重" value="CRITICAL" />
            <el-option label="高风险" value="HIGH" />
            <el-option label="中风险" value="MEDIUM" />
            <el-option label="低风险" value="LOW" />
          </el-select>
          <span class="tl-project-name">{{ globalProjectName || ('#' + filters.projectId) }}</span>
          <el-button type="primary" size="small" :icon="Search" @click="handleSearch">查询</el-button>
        </div>
      </div>

      <div v-if="events.length" class="tl-list">
        <div
          v-for="event in events"
          :key="`${event.sourceType}-${event.sourceId}-${event.createdTime}`"
          class="tl-item"
          :class="{ 'tl-item--critical': event.riskLevel === 'CRITICAL' }"
        >
          <div class="tl-item-dot" :class="`dot-${event.riskLevel}`"></div>
          <div class="tl-item-body">
            <div class="tl-item-header">
              <span class="tl-item-name">{{ event.eventName }}</span>
              <RiskBadge :level="event.riskLevel" size="small" />
            </div>
            <div class="tl-item-summary">{{ event.summary }}</div>
            <div class="tl-item-footer">
              <el-tag size="small" effect="plain">{{ event.sourceType }}</el-tag>
              <span class="tl-item-time">{{ formatTime(event.createdTime) }}</span>
            </div>
          </div>
        </div>
      </div>
      <EmptyState v-else title="暂无安全事件" desc="扫描项目后将自动生成安全事件" />

      <el-pagination
        v-if="total > 0"
        v-model:current-page="pagination.current"
        v-model:page-size="pagination.size"
        :total="total"
        layout="total, prev, pager, next"
        style="margin-top: 20px; justify-content: flex-end;"
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, inject, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import RiskBadge from '../components/RiskBadge.vue'
import EmptyState from '../components/EmptyState.vue'
import { getTimelineEvents, getSecurityOverview } from '../api/timeline'
import type { TimelineEventVO, ProjectSecurityOverviewVO } from '../types/timeline'

const globalProjectId = inject<any>('globalProjectId', ref(null))
const globalProjectName = inject<any>('globalProjectName', ref(''))

const filters = reactive({ projectId: null as number | null, riskLevel: '' })
const events = ref<TimelineEventVO[]>([])
const total = ref(0)
const pagination = reactive({ current: 1, size: 20 })

const criticalCount = ref(0)
const highCount = ref(0)
const mediumCount = ref(0)
const lowCount = ref(0)

const formatTime = (time: string) => new Date(time).toLocaleString('zh-CN')

const loadOverview = async () => {
  if (!filters.projectId) return
  try {
    const res = await getSecurityOverview(filters.projectId)
    if (res.data.code === 0) {
      const ov = res.data.data
      total.value = ov.totalEvents
      criticalCount.value = ov.criticalCount
      highCount.value = ov.highCount
      mediumCount.value = ov.mediumCount
      lowCount.value = ov.lowCount
    }
  } catch { /* silent */ }
}

const loadEvents = async () => {
  if (!filters.projectId) return
  try {
    const res = await getTimelineEvents(filters.projectId, pagination.current, pagination.size, filters.riskLevel || undefined)
    if (res.data.code === 0) {
      events.value = res.data.data.records
      total.value = res.data.data.total
    } else {
      ElMessage.error(res.data.message)
    }
  } catch {
    ElMessage.error('加载时间线失败')
  }
}

const handleSearch = () => {
  pagination.current = 1
  loadEvents()
  loadOverview()
}

const handlePageChange = () => loadEvents()

onMounted(() => {
  if (globalProjectId.value) {
    filters.projectId = globalProjectId.value
    loadOverview()
    loadEvents()
  }
})

watch(globalProjectId, (val) => {
  if (val && val !== filters.projectId) {
    filters.projectId = val
    pagination.current = 1
    loadOverview()
    loadEvents()
  }
})
</script>

<style scoped>
.tl-overview {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
  margin-bottom: 20px;
}

.tl-ov-card {
  background: #ffffff;
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  padding: 16px;
  text-align: center;
  border-top: 3px solid #E2E8F0;
}

.tl-ov-critical { border-top-color: #EF4444; }
.tl-ov-high { border-top-color: #F59E0B; }
.tl-ov-medium { border-top-color: #FBBF24; }
.tl-ov-low { border-top-color: #10B981; }

.tl-ov-value { font-size: 28px; font-weight: 700; color: #111827; }
.tl-ov-label { font-size: 12px; color: #64748B; margin-top: 4px; }

/* Timeline list */
.tl-list {
  display: flex;
  flex-direction: column;
}

.tl-item {
  display: flex;
  gap: 14px;
  padding: 16px 0;
  border-bottom: 1px solid #F1F5F9;
}

.tl-item:last-child { border-bottom: none; }

.tl-item--critical {
  background: rgba(239,68,68,0.02);
  margin: 0 -12px;
  padding-left: 12px;
  padding-right: 12px;
  border-radius: 8px;
  border-bottom: none;
  margin-bottom: 4px;
}

.tl-item-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-top: 6px;
  flex-shrink: 0;
}

.dot-CRITICAL { background: #EF4444; }
.dot-HIGH { background: #F59E0B; }
.dot-MEDIUM { background: #FBBF24; }
.dot-LOW { background: #10B981; }

.tl-item-body { flex: 1; min-width: 0; }

.tl-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.tl-item-name { font-size: 14px; font-weight: 600; color: #111827; }

.tl-item-summary { font-size: 13px; color: #64748B; line-height: 1.5; margin-bottom: 8px; }

.tl-item-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.tl-item-time { font-size: 11px; color: #94A3B8; }

.tl-project-name {
  font-size: 13px;
  font-weight: 600;
  color: #111827;
  background: #F1F5F9;
  padding: 4px 10px;
  border-radius: 6px;
}

@media (max-width: 900px) {
  .tl-overview { grid-template-columns: repeat(3, 1fr); }
}
</style>
