<template>
  <div class="code-block">
    <div class="code-header">
      <div class="code-header-left">
        <span class="code-dot red"></span>
        <span class="code-dot yellow"></span>
        <span class="code-dot green"></span>
        <span v-if="title" class="code-title">{{ title }}</span>
      </div>
      <button class="code-copy" @click="handleCopy">
        <el-icon :size="14"><CopyDocument /></el-icon>
        {{ copied ? '已复制' : '复制' }}
      </button>
    </div>
    <pre class="code-pre"><code>{{ code }}</code></pre>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { CopyDocument } from '@element-plus/icons-vue'

interface Props {
  code: string
  title?: string
}

const props = defineProps<Props>()
const copied = ref(false)

const handleCopy = async () => {
  try {
    await navigator.clipboard.writeText(props.code)
    copied.value = true
    setTimeout(() => { copied.value = false }, 2000)
  } catch {
    // silent
  }
}
</script>

<style scoped>
.code-block {
  background: #0F172A;
  border-radius: 10px;
  overflow: hidden;
  font-family: 'JetBrains Mono', 'Fira Code', Consolas, monospace;
  border: 1px solid #1E293B;
}

.code-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 16px;
  background: #1E293B;
}

.code-header-left {
  display: flex;
  align-items: center;
  gap: 6px;
}

.code-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.code-dot.red { background: #EF4444; }
.code-dot.yellow { background: #F59E0B; }
.code-dot.green { background: #10B981; }

.code-title {
  color: #64748B;
  font-size: 12px;
  margin-left: 8px;
}

.code-copy {
  display: flex;
  align-items: center;
  gap: 4px;
  background: transparent;
  border: 1px solid #334155;
  color: #94A3B8;
  font-size: 11px;
  padding: 3px 10px;
  border-radius: 5px;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.15s;
}

.code-copy:hover {
  background: #334155;
  color: #E2E8F0;
}

.code-pre {
  margin: 0;
  padding: 16px;
  color: #E2E8F0;
  font-size: 13px;
  line-height: 1.7;
  overflow-x: auto;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
