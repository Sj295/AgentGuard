<template>
  <div class="empty-state">
    <div class="empty-icon">
      <el-icon :size="40" color="#CBD5E1"><DataLine /></el-icon>
    </div>
    <div class="empty-title">{{ title || '暂无数据' }}</div>
    <div v-if="desc" class="empty-desc">{{ desc }}</div>
  </div>
</template>

<script setup lang="ts">
import { DataLine } from '@element-plus/icons-vue'

interface Props {
  title?: string
  desc?: string
}

defineProps<Props>()
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
}

.empty-icon {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: #F1F5F9;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
}

.empty-title {
  font-size: 14px;
  font-weight: 500;
  color: #64748B;
}

.empty-desc {
  font-size: 12px;
  color: #94A3B8;
  margin-top: 6px;
  text-align: center;
  max-width: 280px;
}
</style>
