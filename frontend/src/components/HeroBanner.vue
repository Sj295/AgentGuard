<template>
  <div class="hero-banner">
    <div class="hero-left">
      <div class="hero-label">AGENTGUARD SAFETY CONSOLE</div>
      <h1 class="hero-title">AI 代理代码安全堡垒</h1>
      <p class="hero-desc">全面的预检与审计，确保您的代码在 AI 介入前安全无虞。</p>
      <div class="hero-actions">
        <el-button type="primary" size="large" @click="$router.push('/scan')">立即扫描项目</el-button>
        <el-button size="large" class="hero-btn-secondary" @click="$router.push('/preflight')">执行 Preflight 检查</el-button>
      </div>
    </div>

    <div class="hero-right">
      <div class="flow-card">
        <div class="flow-card-header">
          <el-icon :size="18" color="#4F46E5"><Connection /></el-icon>
          <span>Agent 安全执行流程</span>
        </div>
        <FlowSteps :steps="flowSteps" :active="0" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Connection } from '@element-plus/icons-vue'
import FlowSteps from './FlowSteps.vue'

const flowSteps = ['扫描项目', '规则生成', '权限评估', '命令审计', '预执行检查', 'Git 审计', '安全报告']
</script>

<style scoped>
.hero-banner {
  display: flex;
  gap: 24px;
  margin-bottom: 24px;
  align-items: stretch;
}

.hero-left {
  flex: 6;
  background: linear-gradient(135deg, #081225 0%, #141a3a 40%, #1e1b4b 100%);
  border-radius: 16px;
  padding: 40px 44px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.hero-left::before {
  content: '';
  position: absolute;
  top: -80px;
  right: -60px;
  width: 320px;
  height: 320px;
  border: 1px solid rgba(99, 102, 241, 0.08);
  border-radius: 50%;
  pointer-events: none;
}

.hero-left::after {
  content: '';
  position: absolute;
  bottom: -120px;
  right: 40px;
  width: 240px;
  height: 240px;
  border: 1px solid rgba(99, 102, 241, 0.05);
  border-radius: 50%;
  pointer-events: none;
}

.hero-label {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 2px;
  color: #818CF8;
  margin-bottom: 16px;
}

.hero-title {
  font-size: 32px;
  font-weight: 800;
  color: #F1F5F9;
  line-height: 1.25;
  margin: 0 0 12px;
  max-width: 480px;
}

.hero-desc {
  font-size: 15px;
  color: #94A3B8;
  line-height: 1.6;
  margin: 0 0 28px;
  max-width: 460px;
}

.hero-actions {
  display: flex;
  gap: 12px;
}

.hero-btn-secondary {
  background: rgba(255,255,255,0.06);
  border: 1px solid rgba(255,255,255,0.12);
  color: #CBD5E1;
}

.hero-btn-secondary:hover {
  background: rgba(255,255,255,0.1);
  border-color: rgba(255,255,255,0.2);
  color: #F1F5F9;
}

.hero-right {
  flex: 4;
  min-width: 0;
}

.flow-card {
  background: #fff;
  border: 1px solid #E5E7EB;
  border-radius: 16px;
  padding: 24px 28px;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.flow-card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 24px;
}

@media (max-width: 900px) {
  .hero-banner {
    flex-direction: column;
  }
  .hero-left {
    padding: 32px;
  }
  .hero-title {
    font-size: 24px;
  }
}
</style>
