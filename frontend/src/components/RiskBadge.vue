<template>
  <span
    class="risk-badge"
    :class="[`risk-badge--${level}`, `risk-badge--${size}`, { 'risk-badge--pulse': level === 'CRITICAL' }]"
  >
    <span class="risk-dot"></span>
    {{ label }}
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  level: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL' | string
  size?: 'small' | 'default' | 'large'
}

const props = withDefaults(defineProps<Props>(), {
  size: 'default'
})

const labelMap: Record<string, string> = {
  LOW: '低风险',
  MEDIUM: '中风险',
  HIGH: '高风险',
  CRITICAL: '严重'
}

const label = computed(() => labelMap[props.level] || props.level)
</script>

<style scoped>
.risk-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  border-radius: 6px;
  white-space: nowrap;
}

.risk-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

/* Sizes */
.risk-badge--small { font-size: 11px; padding: 2px 8px; }
.risk-badge--default { font-size: 12px; padding: 4px 10px; }
.risk-badge--large { font-size: 14px; padding: 6px 14px; }

/* Levels */
.risk-badge--LOW { background: rgba(16,185,129,0.08); color: #059669; }
.risk-badge--LOW .risk-dot { background: #10B981; }

.risk-badge--MEDIUM { background: rgba(245,158,11,0.08); color: #D97706; }
.risk-badge--MEDIUM .risk-dot { background: #F59E0B; }

.risk-badge--HIGH { background: rgba(245,158,11,0.1); color: #B45309; }
.risk-badge--HIGH .risk-dot { background: #F59E0B; }

.risk-badge--CRITICAL { background: rgba(239,68,68,0.08); color: #DC2626; }
.risk-badge--CRITICAL .risk-dot { background: #EF4444; }

.risk-badge--pulse {
  animation: riskPulse 2s ease-in-out infinite;
}

@keyframes riskPulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(239,68,68,0.2); }
  50% { box-shadow: 0 0 0 6px rgba(239,68,68,0); }
}
</style>
