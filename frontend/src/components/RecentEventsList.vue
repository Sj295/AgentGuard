<template>
  <div class="events-panel">
    <div class="events-header">
      <span class="events-title">最近安全事件</span>
      <el-button text type="primary" size="small" @click="$router.push('/timeline')">
        查看全部<el-icon :size="14" style="margin-left:4px"><ArrowRight /></el-icon>
      </el-button>
    </div>

    <div v-if="events.length" class="events-table-wrap">
      <table class="events-table">
        <thead>
          <tr>
            <th>时间</th>
            <th>级别</th>
            <th>类型</th>
            <th>描述</th>
            <th>状态</th>
            <th class="col-action">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="event in events" :key="event.sourceId + event.sourceType + event.createdTime">
            <td class="col-time">{{ formatTime(event.createdTime) }}</td>
            <td><RiskBadge :level="event.riskLevel" size="small" /></td>
            <td class="col-type">{{ eventTypeLabel(event.eventType) }}</td>
            <td class="col-desc">
              <span class="desc-text">{{ event.summary }}</span>
            </td>
            <td>
              <el-tag :type="statusType(event.riskLevel)" effect="plain" size="small">
                {{ statusLabel(event.riskLevel) }}
              </el-tag>
            </td>
            <td class="col-action">
              <el-button text type="primary" size="small" @click="$router.push('/timeline')">详情</el-button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <EmptyState v-else title="暂无安全事件" desc="扫描项目后将自动生成安全事件" />
  </div>
</template>

<script setup lang="ts">
import { ArrowRight } from '@element-plus/icons-vue'
import RiskBadge from './RiskBadge.vue'
import EmptyState from './EmptyState.vue'
import type { TimelineEventVO } from '../types/timeline'

interface Props {
  events: TimelineEventVO[]
}

defineProps<Props>()

const formatTime = (time: string) => new Date(time).toLocaleString('zh-CN')

const eventTypeLabels: Record<string, string> = {
  RISK_REPORT: '风险报告',
  SCAN_TASK: '扫描任务',
  RULE_GENERATION: '规则生成',
  GIT_AUDIT: 'Git 审计',
  PERMISSION_ASSESS: '权限评估',
  COMMAND_AUDIT: '命令审计',
  PREFLIGHT_CHECK: '预执行检查'
}

const eventTypeLabel = (type: string) => eventTypeLabels[type] || type

const statusLabel = (level: string) => {
  if (level === 'CRITICAL' || level === 'HIGH') return '已拦截'
  if (level === 'MEDIUM') return '已警告'
  return '已完成'
}

const statusType = (level: string): 'danger' | 'warning' | 'success' | 'info' => {
  if (level === 'CRITICAL' || level === 'HIGH') return 'danger'
  if (level === 'MEDIUM') return 'warning'
  return 'success'
}
</script>

<style scoped>
.events-panel {
  background: #fff;
  border: 1px solid #E5E7EB;
  border-radius: 14px;
  padding: 20px 24px;
}

.events-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.events-title {
  font-size: 15px;
  font-weight: 700;
  color: #111827;
}

.events-table-wrap {
  overflow-x: auto;
}

.events-table {
  width: 100%;
  border-collapse: collapse;
}

.events-table th {
  text-align: left;
  font-size: 11px;
  font-weight: 600;
  color: #94A3B8;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  padding: 8px 12px;
  border-bottom: 1px solid #F1F5F9;
}

.events-table td {
  padding: 12px;
  font-size: 13px;
  color: #334155;
  border-bottom: 1px solid #F8FAFC;
  vertical-align: middle;
}

.events-table tbody tr:hover {
  background: #F8FAFC;
}

.events-table tbody tr:last-child td {
  border-bottom: none;
}

.col-time {
  font-size: 12px;
  color: #94A3B8;
  font-family: 'SF Mono', Menlo, monospace;
  white-space: nowrap;
}

.col-type {
  font-size: 12px;
  color: #64748B;
}

.col-desc {
  max-width: 320px;
}

.desc-text {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.col-action {
  text-align: right;
}
</style>
