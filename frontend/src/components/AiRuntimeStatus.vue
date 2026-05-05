<template>
  <div class="ai-runtime-status" :class="{ compact }">
    <div class="ai-runtime-main">
      <el-icon :size="compact ? 16 : 18" :color="iconColor">
        <MagicStick />
      </el-icon>
      <div class="ai-runtime-copy">
        <div class="ai-runtime-title">AI 调用模式</div>
        <div class="ai-runtime-text">{{ statusText }}</div>
        <div v-if="status?.willCallRemoteModel && !compact" class="ai-runtime-meta">
          {{ status.provider }} · {{ status.model }}
        </div>
      </div>
    </div>
    <el-tag :type="tagType" effect="plain" size="small">{{ modeLabel }}</el-tag>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { MagicStick } from '@element-plus/icons-vue'
import { getAiStatus } from '../api/ai'
import type { AiRuntimeStatusVO } from '../types/ai'

withDefaults(defineProps<{ compact?: boolean }>(), {
  compact: false
})

const status = ref<AiRuntimeStatusVO | null>(null)
const loadFailed = ref(false)

const loadStatus = async () => {
  try {
    const res = await getAiStatus()
    if (res.data.code === 0) {
      status.value = res.data.data
      loadFailed.value = false
      return
    }
  } catch {
    // The status hint should never block the core audit workflow.
  }
  loadFailed.value = true
}

const statusText = computed(() => {
  if (status.value?.statusText) return status.value.statusText
  return loadFailed.value ? '暂时无法读取 AI 运行状态。' : '正在读取 AI 运行状态...'
})

const modeLabel = computed(() => {
  switch (status.value?.executionMode) {
    case 'REAL_MODEL':
      return '真实模型'
    case 'MOCK_EMPTY_KEY':
      return 'Mock：无 Key'
    case 'MISCONFIGURED_EMPTY_KEY':
      return '配置不完整'
    case 'MOCK_DISABLED':
      return 'Mock：AI 已关闭'
    default:
      return loadFailed.value ? '未知' : '读取中'
  }
})

const tagType = computed(() => {
  switch (status.value?.executionMode) {
    case 'REAL_MODEL':
      return 'success'
    case 'MOCK_EMPTY_KEY':
      return 'warning'
    case 'MISCONFIGURED_EMPTY_KEY':
      return 'danger'
    case 'MOCK_DISABLED':
      return 'info'
    default:
      return 'info'
  }
})

const iconColor = computed(() => {
  if (status.value?.executionMode === 'REAL_MODEL') return '#10B981'
  if (status.value?.executionMode === 'MISCONFIGURED_EMPTY_KEY') return '#EF4444'
  if (status.value?.executionMode === 'MOCK_EMPTY_KEY') return '#F59E0B'
  return '#64748B'
})

onMounted(loadStatus)
</script>

<style scoped>
.ai-runtime-status {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  background: #F8FAFC;
}

.ai-runtime-status.compact {
  align-items: center;
  padding: 9px 12px;
}

.ai-runtime-main {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  min-width: 0;
}

.compact .ai-runtime-main {
  align-items: center;
}

.ai-runtime-copy {
  min-width: 0;
}

.ai-runtime-title {
  font-size: 12px;
  font-weight: 600;
  color: #334155;
}

.ai-runtime-text {
  margin-top: 2px;
  font-size: 12px;
  line-height: 1.5;
  color: #64748B;
}

.compact .ai-runtime-title {
  display: none;
}

.compact .ai-runtime-text {
  margin-top: 0;
}

.ai-runtime-meta {
  margin-top: 3px;
  font-size: 11px;
  color: #94A3B8;
}

@media (max-width: 640px) {
  .ai-runtime-status,
  .ai-runtime-status.compact {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
