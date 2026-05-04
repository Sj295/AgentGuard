<template>
  <div class="report-panel">
    <div v-if="title" class="report-panel-header">
      <el-icon :size="16" :color="typeColor"><Warning v-if="type==='warn'" /><CircleCheck v-else /></el-icon>
      <span>{{ title }}</span>
      <el-tag v-if="count !== undefined" size="small" type="info" effect="plain">{{ count }}</el-tag>
    </div>
    <div class="report-panel-body">
      <div v-for="(item, i) in items" :key="i" class="report-item">
        <span class="report-item-dot" :style="{ background: typeColor }"></span>
        <span class="report-item-text">{{ item }}</span>
      </div>
      <div v-if="!items?.length" class="report-empty">暂无数据</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Warning, CircleCheck } from '@element-plus/icons-vue'

interface Props {
  title?: string
  items?: string[]
  type?: 'warn' | 'info' | 'success'
  count?: number
}

const props = withDefaults(defineProps<Props>(), {
  type: 'warn',
  items: () => []
})

const typeColor = computed(() => {
  switch (props.type) {
    case 'warn': return '#F59E0B'
    case 'success': return '#10B981'
    default: return '#4F46E5'
  }
})
</script>

<style scoped>
.report-panel {
  background: #ffffff;
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  overflow: hidden;
}

.report-panel-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: #F8FAFC;
  border-bottom: 1px solid #E2E8F0;
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}

.report-panel-body {
  padding: 12px 16px;
}

.report-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 6px 0;
}

.report-item-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  margin-top: 7px;
  flex-shrink: 0;
}

.report-item-text {
  font-size: 13px;
  color: #475569;
  line-height: 1.5;
}

.report-empty {
  padding: 8px 0;
  font-size: 13px;
  color: #94A3B8;
}
</style>
