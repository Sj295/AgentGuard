<template>
  <el-tag :type="tagType" effect="light">
    {{ label }}
  </el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  status: 'PASS' | 'WARN' | 'FAIL' | string
}

const props = defineProps<Props>()

const labelMap: Record<string, string> = {
  PASS: '通过',
  WARN: '警告',
  FAIL: '失败'
}

const label = computed(() => labelMap[props.status] || props.status)

const tagType = computed(() => {
  switch (props.status) {
    case 'PASS': return 'success'
    case 'WARN': return 'warning'
    case 'FAIL': return 'danger'
    default: return 'info'
  }
})
</script>
