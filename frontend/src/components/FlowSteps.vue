<template>
  <div class="flow-steps">
    <div
      v-for="(step, i) in steps"
      :key="i"
      class="flow-step"
      :class="{ 'flow-step--active': i === active, 'flow-step--done': i < active }"
    >
      <div class="flow-step-num">
        <el-icon v-if="i < active" :size="12"><CircleCheck /></el-icon>
        <span v-else>{{ i + 1 }}</span>
      </div>
      <span class="flow-step-label">{{ step }}</span>
      <div v-if="i < steps.length - 1" class="flow-step-line"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { CircleCheck } from '@element-plus/icons-vue'

interface Props {
  steps: string[]
  active?: number
}

withDefaults(defineProps<Props>(), {
  active: 0
})
</script>

<style scoped>
.flow-steps {
  display: flex;
  align-items: center;
  gap: 0;
  overflow-x: auto;
  padding: 4px 0;
}

.flow-step {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.flow-step-num {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 600;
  background: #E2E8F0;
  color: #94A3B8;
  flex-shrink: 0;
}

.flow-step-label {
  font-size: 12px;
  color: #94A3B8;
  white-space: nowrap;
}

.flow-step-line {
  width: 24px;
  height: 1px;
  background: #E2E8F0;
  margin: 0 4px;
  flex-shrink: 0;
}

.flow-step--active .flow-step-num {
  background: #4F46E5;
  color: #ffffff;
}

.flow-step--active .flow-step-label {
  color: #4F46E5;
  font-weight: 600;
}

.flow-step--done .flow-step-num {
  background: #10B981;
  color: #ffffff;
}

.flow-step--done .flow-step-label {
  color: #64748B;
}

.flow-step--done .flow-step-line {
  background: #10B981;
}
</style>
