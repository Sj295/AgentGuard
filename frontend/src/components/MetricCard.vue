<template>
  <div class="metric-card" :class="`metric-card--${color}`">
    <div class="metric-icon">
      <el-icon :size="22"><component :is="icon" /></el-icon>
    </div>
    <div class="metric-body">
      <div class="metric-value">{{ value }}</div>
      <div class="metric-label">{{ label }}</div>
    </div>
    <div v-if="suffix" class="metric-suffix">{{ suffix }}</div>
  </div>
</template>

<script setup lang="ts">
import type { Component } from 'vue'

interface Props {
  icon: Component
  value: string | number
  label: string
  color?: 'indigo' | 'red' | 'cyan' | 'emerald' | 'amber'
  suffix?: string
}

withDefaults(defineProps<Props>(), {
  color: 'indigo',
  suffix: ''
})
</script>

<style scoped>
.metric-card {
  display: flex;
  align-items: center;
  gap: 16px;
  background: #ffffff;
  border: 1px solid #E2E8F0;
  border-radius: 12px;
  padding: 20px;
  transition: all 0.2s;
}

.metric-card:hover {
  box-shadow: 0 4px 20px rgba(0,0,0,0.04);
  transform: translateY(-1px);
}

.metric-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.metric-card--indigo .metric-icon { background: rgba(79,70,229,0.08); color: #4F46E5; }
.metric-card--red .metric-icon { background: rgba(239,68,68,0.08); color: #EF4444; }
.metric-card--cyan .metric-icon { background: rgba(6,182,212,0.08); color: #06B6D4; }
.metric-card--emerald .metric-icon { background: rgba(16,185,129,0.08); color: #10B981; }
.metric-card--amber .metric-icon { background: rgba(245,158,11,0.08); color: #F59E0B; }

.metric-body {
  flex: 1;
  min-width: 0;
}

.metric-value {
  font-size: 24px;
  font-weight: 700;
  color: #111827;
  line-height: 1.2;
}

.metric-label {
  font-size: 12px;
  color: #64748B;
  margin-top: 4px;
}

.metric-suffix {
  font-size: 12px;
  color: #94A3B8;
  flex-shrink: 0;
}
</style>
