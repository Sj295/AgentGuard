<template>
  <div class="status-bar">
    <div class="status-left">
      <AiRuntimeStatus compact />
      <span class="status-sep"></span>

      <div class="status-item">
        <span class="status-dot" :class="redisAvailable ? 'dot-up' : 'dot-down'"></span>
        <div class="status-text">
          <span class="status-label">Redis 缓存</span>
          <span class="status-val">{{ redisAvailable ? '连接正常' : '未连接' }}</span>
        </div>
      </div>

      <span class="status-sep"></span>

      <div class="status-item">
        <span class="status-text">
          <span class="status-label">规则版本</span>
          <span class="status-val">v2026.05.04</span>
        </span>
      </div>

      <span class="status-sep"></span>

      <div class="status-item">
        <span class="status-text">
          <span class="status-label">最近扫描</span>
          <span class="status-val">{{ lastScanTime }}</span>
        </span>
      </div>
    </div>

    <div class="status-right">
      <el-button text size="small" class="status-btn" @click="$router.push('/timeline')">
        <el-icon :size="14"><Odometer /></el-icon>
        系统状态
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, inject, onMounted, ref } from 'vue'
import { Odometer } from '@element-plus/icons-vue'
import AiRuntimeStatus from './AiRuntimeStatus.vue'
import { getAiStatus } from '../api/ai'

interface Props {
  lastScanTime?: string
}

const props = withDefaults(defineProps<Props>(), {
  lastScanTime: '—'
})

const redisAvailable = ref(false)

const lastScanTime = computed(() => {
  if (!props.lastScanTime || props.lastScanTime === '—') return '—'
  return new Date(props.lastScanTime).toLocaleString('zh-CN')
})

onMounted(async () => {
  try {
    const res = await getAiStatus()
    if (res.data.code === 0) {
      redisAvailable.value = !!res.data.data.redisAvailable
    }
  } catch { /* silent */ }
})
</script>

<style scoped>
.status-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border: 1px solid #E5E7EB;
  border-radius: 12px;
  padding: 0 20px;
  height: 52px;
  margin-bottom: 24px;
}

.status-left {
  display: flex;
  align-items: center;
  gap: 0;
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

.status-sep {
  width: 1px;
  height: 24px;
  background: #E5E7EB;
  margin: 0 16px;
  flex-shrink: 0;
}

.status-item {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.dot-up {
  background: #22C55E;
  box-shadow: 0 0 6px rgba(34, 197, 94, 0.3);
}

.dot-down {
  background: #CBD5E1;
}

.status-text {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.status-label {
  font-size: 10px;
  color: #94A3B8;
  font-weight: 600;
  letter-spacing: 0.3px;
}

.status-val {
  font-size: 12px;
  color: #334155;
  font-weight: 500;
}

.status-right {
  flex-shrink: 0;
}

.status-btn {
  color: #64748B;
  font-size: 12px;
}

.status-btn:hover {
  color: #4F46E5;
}
</style>
