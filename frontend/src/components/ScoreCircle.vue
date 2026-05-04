<template>
  <div class="score-circle">
    <el-progress
      :percentage="percentage"
      :color="color"
      :stroke-width="10"
      type="circle"
      :width="size"
      :format="formatText"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  score: number
  size?: number
}

const props = withDefaults(defineProps<Props>(), {
  size: 120
})

const percentage = computed(() => Math.min(Math.max(props.score, 0), 100))

const color = computed(() => {
  const s = props.score
  if (s < 40) return '#67c23a'
  if (s < 60) return '#f59e0b'
  if (s < 80) return '#e6a23c'
  return '#f56c6c'
})

const formatText = (p: number) => `${p}`
</script>

<style scoped>
.score-circle {
  display: inline-flex;
  justify-content: center;
  align-items: center;
}
</style>
