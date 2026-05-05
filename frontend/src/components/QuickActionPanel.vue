<template>
  <div class="qa-panel">
    <div class="qa-grid">
      <div
        v-for="action in actions"
        :key="action.path"
        class="qa-item"
        @click="$router.push(action.path)"
      >
        <div class="qa-icon" :style="{ background: action.bg, color: action.color }">
          <el-icon :size="18"><component :is="action.icon" /></el-icon>
        </div>
        <div class="qa-text">
          <div class="qa-name">{{ action.label }}</div>
          <div class="qa-desc">{{ action.desc }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Search, Document, Lock, Warning, Share, DataAnalysis } from '@element-plus/icons-vue'

const actions = [
  { path: '/scan',          label: '扫描项目',   desc: '分析项目结构与风险',     icon: Search,       bg: 'rgba(79,70,229,0.08)',  color: '#4F46E5' },
  { path: '/agent-rules',    label: 'Agent 规则', desc: '管理 Agent 安全规则',   icon: Document,     bg: 'rgba(6,182,212,0.08)',  color: '#06B6D4' },
  { path: '/permission',     label: '权限评估',   desc: '评估 Agent 权限配置',   icon: Lock,         bg: 'rgba(245,158,11,0.08)', color: '#F59E0B' },
  { path: '/command-audit',  label: '命令审计',   desc: '检测危险命令执行',      icon: Warning,      bg: 'rgba(239,68,68,0.08)',  color: '#EF4444' },
  { path: '/git-audit',      label: 'Git 审计',   desc: '审计未提交代码变更',    icon: Share,        bg: 'rgba(16,185,129,0.08)', color: '#10B981' },
  { path: '/reports',        label: '安全报告',   desc: '生成项目安全报告',      icon: DataAnalysis, bg: 'rgba(99,102,241,0.08)', color: '#6366F1' },
]
</script>

<style scoped>
.qa-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.qa-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px;
  border-radius: 10px;
  background: #F8FAFC;
  border: 1px solid transparent;
  cursor: pointer;
  transition: all 0.15s;
}

.qa-item:hover {
  background: #fff;
  border-color: #C7D2FE;
  box-shadow: 0 2px 12px rgba(79,70,229,0.06);
  transform: translateY(-1px);
}

.qa-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.qa-name {
  font-size: 13px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 2px;
}

.qa-desc {
  font-size: 11px;
  color: #94A3B8;
  line-height: 1.4;
}
</style>
