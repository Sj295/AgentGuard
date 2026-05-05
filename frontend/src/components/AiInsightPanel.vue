<template>
  <div class="ai-panel">
    <div class="ai-header">
      <div class="ai-title-wrap">
        <el-icon color="#4F46E5"><MagicStick /></el-icon>
        <span class="ai-title">{{ title }}</span>
      </div>
      <el-tag v-if="cached" type="info" effect="plain" size="small">缓存命中</el-tag>
      <el-tag v-else-if="mocked" type="warning" effect="plain" size="small">Mock 输出</el-tag>
      <el-tag v-else type="success" effect="plain" size="small">真实模型输出</el-tag>
    </div>

    <div v-if="summary" class="ai-summary">
      <div class="ai-section-title">{{ summaryTitle }}</div>
      <div class="ai-summary-text">{{ summary }}</div>
    </div>

    <div v-for="(section, idx) in sections" :key="idx" class="ai-section">
      <div class="ai-section-title">{{ section.title }}</div>
      <div v-if="section.items?.length" class="ai-list">
        <div v-for="(item, i) in section.items" :key="`${idx}-${i}`" class="ai-item">
          <span class="ai-dot"></span>
          <span>{{ item }}</span>
        </div>
      </div>
      <div v-else class="ai-empty">暂无建议</div>
    </div>

    <div class="ai-note">{{ confidenceNote }}</div>
  </div>
</template>

<script setup lang="ts">
import { MagicStick } from '@element-plus/icons-vue'
import type { AiInsightSection } from '../types/ai'

interface Props {
  title?: string
  mocked: boolean
  cached?: boolean
  confidenceNote: string
  summary?: string
  summaryTitle?: string
  sections?: AiInsightSection[]
}

withDefaults(defineProps<Props>(), {
  title: 'AI 增强建议',
  cached: false,
  summary: '',
  summaryTitle: '摘要',
  sections: () => []
})
</script>

<style scoped>
.ai-panel {
  margin-top: 16px;
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  background: #FFFFFF;
  overflow: hidden;
}

.ai-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid #E2E8F0;
  background: #F8FAFC;
}

.ai-title-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ai-title {
  font-size: 14px;
  font-weight: 600;
  color: #334155;
}

.ai-summary {
  padding: 14px 16px 0;
}

.ai-summary-text {
  margin-top: 8px;
  font-size: 13px;
  line-height: 1.6;
  color: #334155;
}

.ai-section {
  padding: 14px 16px 0;
}

.ai-section:last-of-type {
  padding-bottom: 12px;
}

.ai-section-title {
  font-size: 13px;
  font-weight: 600;
  color: #111827;
}

.ai-list {
  margin-top: 8px;
}

.ai-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 13px;
  line-height: 1.5;
  color: #475569;
  padding: 5px 0;
}

.ai-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #4F46E5;
  margin-top: 7px;
  flex-shrink: 0;
}

.ai-empty {
  margin-top: 8px;
  font-size: 12px;
  color: #94A3B8;
}

.ai-note {
  margin-top: 8px;
  padding: 10px 16px 14px;
  font-size: 12px;
  line-height: 1.5;
  color: #64748B;
}
</style>
