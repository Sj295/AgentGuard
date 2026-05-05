<template>
  <el-container class="layout-root">
    <el-aside :width="isCollapse ? '64px' : '240px'" class="layout-aside">
      <div class="logo-area">
        <AppLogo :size="32" />
        <div v-if="!isCollapse" class="logo-text-wrap">
          <div class="logo-text">AgentGuard</div>
          <div class="logo-sub">Agent Safety Console</div>
        </div>
      </div>

      <div class="nav-scroll">
        <div v-for="group in menuGroups" :key="group.label" class="nav-group">
          <div v-if="!isCollapse" class="nav-group-label">{{ group.label }}</div>
          <div
            v-for="item in group.items"
            :key="item.path"
            class="nav-item"
            :class="{ active: activeMenu === item.path }"
            @click="$router.push(item.path)"
          >
            <el-icon :size="18"><component :is="item.icon" /></el-icon>
            <span v-if="!isCollapse" class="nav-label">{{ item.label }}</span>
          </div>
        </div>
      </div>

      <div v-if="!isCollapse" class="sidebar-footer">
        <div class="sidebar-version">v0.1.0</div>
      </div>
    </el-aside>

    <el-container>
      <el-header class="layout-header" height="56px">
        <div class="header-left">
          <el-button text class="collapse-btn" @click="toggleCollapse">
            <el-icon :size="18"><Fold v-if="!isCollapse" /><Expand v-else /></el-icon>
          </el-button>
          <div class="header-title-area">
            <div class="header-title">{{ pageTitle }}</div>
            <div class="header-desc">{{ pageDesc }}</div>
          </div>
        </div>
        <div class="header-right">
          <div v-if="hasScannedProject && globalProjectId" class="header-project">
            <span class="header-project-label">当前项目</span>
            <span class="header-project-id">{{ globalProjectName || ('#' + globalProjectId) }}</span>
          </div>
          <div v-else class="header-project-empty">
            <span>请先扫描项目</span>
          </div>
          <div class="header-badge">
            <el-icon :size="14"><Warning /></el-icon>
            AI Agent 安全防护
          </div>
        </div>
      </el-header>

      <el-main class="layout-main">
        <div class="main-content">
          <router-view/>
        </div>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, provide } from 'vue'
import { useRoute } from 'vue-router'
import {
  Odometer, Search, Document, Lock, Warning,
  CircleCheck, Share, DataAnalysis, Timer,
  Fold, Expand
} from '@element-plus/icons-vue'
import AppLogo from '../components/AppLogo.vue'
import { getProjectById } from '../api/project'

const route = useRoute()
const isCollapse = ref(false)
const savedProjectId = Number(localStorage.getItem('agentguard_projectId') || 0)
const savedHasScanned = localStorage.getItem('agentguard_hasScanned') === 'true'
const globalProjectId = ref<number | null>(savedHasScanned && savedProjectId > 0 ? savedProjectId : null)
const globalProjectName = ref('')
const hasScannedProject = ref(savedHasScanned && savedProjectId > 0)

const loadProjectName = async (id: number) => {
  try {
    const res = await getProjectById(id)
    if (res.data.code === 0) {
      globalProjectName.value = res.data.data.projectName
    }
  } catch { /* silent */ }
}

onMounted(() => {
  if (globalProjectId.value) {
    loadProjectName(globalProjectId.value)
  }
})

provide('globalProjectId', globalProjectId)
provide('globalProjectName', globalProjectName)
provide('hasScannedProject', hasScannedProject)

const activeMenu = computed(() => route.path)

const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
}

const menuGroups = [
  {
    label: '概览',
    items: [
      { path: '/', label: '安全总览', icon: Odometer }
    ]
  },
  {
    label: '项目',
    items: [
      { path: '/scan', label: '项目扫描', icon: Search },
      { path: '/agent-rules', label: 'Agent 规则', icon: Document }
    ]
  },
  {
    label: 'Agent 控制',
    items: [
      { path: '/permission', label: '权限评估', icon: Lock },
      { path: '/preflight', label: '预执行检查', icon: CircleCheck }
    ]
  },
  {
    label: '审计',
    items: [
      { path: '/command-audit', label: '命令审计', icon: Warning },
      { path: '/git-audit', label: 'Git 变更审计', icon: Share }
    ]
  },
  {
    label: '报告',
    items: [
      { path: '/reports', label: '安全报告', icon: DataAnalysis },
      { path: '/timeline', label: '安全时间线', icon: Timer }
    ]
  }
]

const pageTitles: Record<string, string> = {
  '/': '安全总览',
  '/scan': '项目扫描',
  '/agent-rules': 'Agent 规则',
  '/permission': '权限评估',
  '/command-audit': '命令审计',
  '/preflight': '预执行检查',
  '/git-audit': 'Git 变更审计',
  '/reports': '安全报告',
  '/timeline': '安全时间线'
}

const pageDescs: Record<string, string> = {
  '/': '项目安全状态概览与快速操作入口',
  '/scan': '识别技术栈、敏感文件、Git 状态与上下文风险',
  '/agent-rules': '为 Codex / Claude Code / Cursor 生成项目安全规则',
  '/permission': '评估 Agent 任务执行时的权限配置风险',
  '/command-audit': '检测待执行命令中的安全风险',
  '/preflight': 'Agent 执行前的全面安全检查',
  '/git-audit': '审计未提交的 Git 变更并检测风险修改',
  '/reports': '生成和导出项目安全报告',
  '/timeline': '按时间顺序查看项目安全事件'
}

const pageTitle = computed(() => pageTitles[route.path] || 'AgentGuard')
const pageDesc = computed(() => pageDescs[route.path] || '')
</script>

<style scoped>
.layout-root {
  height: 100vh;
}

/* Sidebar */
.layout-aside {
  background: #081225;
  transition: width 0.25s ease;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-right: 1px solid #162240;
}

.logo-area {
  height: 64px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  gap: 12px;
  border-bottom: 1px solid #162240;
  flex-shrink: 0;
}

.logo-text-wrap {
  overflow: hidden;
  white-space: nowrap;
}

.logo-text {
  font-size: 16px;
  font-weight: 700;
  color: #F1F5F9;
  letter-spacing: 0.3px;
  line-height: 1.2;
}

.logo-sub {
  font-size: 10px;
  color: #64748B;
  letter-spacing: 0.5px;
  margin-top: 1px;
}

.nav-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 12px 0;
}

.nav-group {
  margin-bottom: 4px;
}

.nav-group-label {
  padding: 8px 24px 4px;
  font-size: 10px;
  font-weight: 600;
  color: #475569;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 9px 20px;
  margin: 2px 8px;
  border-radius: 8px;
  cursor: pointer;
  color: #94A3B8;
  font-size: 13px;
  transition: all 0.15s;
  position: relative;
}

.nav-item:hover {
  background: rgba(255,255,255,0.05);
  color: #E2E8F0;
}

.nav-item.active {
  background: rgba(79, 70, 229, 0.15);
  color: #A5B4FC;
}

.nav-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 20px;
  background: #818CF8;
  border-radius: 0 3px 3px 0;
}

.nav-label {
  white-space: nowrap;
}

.sidebar-footer {
  padding: 12px 20px;
  border-top: 1px solid #162240;
}

.sidebar-version {
  font-size: 11px;
  color: #475569;
  text-align: center;
}

/* Header */
.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  background: #ffffff;
  border-bottom: 1px solid #E2E8F0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  color: #64748B;
}

.header-title {
  font-size: 16px;
  font-weight: 700;
  color: #111827;
  line-height: 1.2;
}

.header-desc {
  font-size: 12px;
  color: #94A3B8;
  margin-top: 2px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.header-project {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-project-label {
  font-size: 12px;
  color: #64748B;
}

.header-project-id {
  font-size: 13px;
  font-weight: 600;
  color: #111827;
  background: #F1F5F9;
  padding: 2px 10px;
  border-radius: 6px;
}

.header-project-empty {
  font-size: 12px;
  color: #94A3B8;
  background: #F1F5F9;
  padding: 4px 12px;
  border-radius: 6px;
}

.header-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  font-weight: 600;
  color: #4F46E5;
  background: rgba(79, 70, 229, 0.06);
  padding: 5px 12px;
  border-radius: 20px;
  border: 1px solid rgba(79, 70, 229, 0.12);
}

/* Main */
.layout-main {
  background: #F6F8FC;
  padding: 0;
  overflow-y: auto;
}

.main-content {
  max-width: 1440px;
  margin: 0 auto;
  padding: 28px;
}
</style>

<style>
.layout-aside .el-menu {
  border-right: none;
}
</style>
