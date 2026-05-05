<template>
  <div class="stat-card" :class="`stat-card--${color}`">
    <div class="stat-icon-wrap">
      <el-icon :size="20"><component :is="icon" /></el-icon>
    </div>
    <div class="stat-body">
      <div class="stat-title">{{ title }}</div>
      <div class="stat-value">{{ value }}</div>
      <div class="stat-subtitle">{{ subtitle }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Component } from 'vue'

interface Props {
  icon: Component
  title: string
  value: string | number
  subtitle: string
  color?: 'indigo' | 'red' | 'cyan' | 'emerald' | 'amber' | 'slate'
}

withDefaults(defineProps<Props>(), {
  color: 'indigo'
})
</script>

<style scoped>
.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  background: #fff;
  border: 1px solid #E5E7EB;
  border-radius: 14px;
  padding: 20px 24px;
  transition: all 0.2s;
  cursor: default;
}

.stat-card:hover {
  box-shadow: 0 4px 16px rgba(0,0,0,0.04);
  transform: translateY(-2px);
}

.stat-icon-wrap {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-card--indigo .stat-icon-wrap { background: rgba(79,70,229,0.08); color: #4F46E5; }
.stat-card--red .stat-icon-wrap    { background: rgba(239,68,68,0.08); color: #EF4444; }
.stat-card--cyan .stat-icon-wrap   { background: rgba(6,182,212,0.08); color: #06B6D4; }
.stat-card--emerald .stat-icon-wrap{ background: rgba(16,185,129,0.08); color: #10B981; }
.stat-card--amber .stat-icon-wrap  { background: rgba(245,158,11,0.08); color: #F59E0B; }
.stat-card--slate .stat-icon-wrap  { background: rgba(100,116,139,0.08); color: #64748B; }

.stat-card--red .stat-value { color: #EF4444; }
.stat-card--amber .stat-value { color: #D97706; }

.stat-body {
  flex: 1;
  min-width: 0;
}

.stat-title {
  font-size: 12px;
  color: #94A3B8;
  margin-bottom: 4px;
  font-weight: 500;
}

.stat-value {
  font-size: 26px;
  font-weight: 700;
  color: #111827;
  line-height: 1.2;
}

.stat-subtitle {
  font-size: 11px;
  color: #94A3B8;
  margin-top: 3px;
}
</style>
