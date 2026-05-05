<template>
  <div class="scan-page">
    <!-- Left: Config -->
    <div class="scan-left">
      <div class="panel">
        <div class="panel-header">扫描本地项目</div>
        <p class="scan-desc">识别技术栈、敏感文件、Git 状态和 Agent 上下文风险。</p>
        <el-form :model="form" label-position="top" @submit.prevent>
          <el-form-item label="项目名称">
            <el-input v-model="form.projectName" placeholder="例如：my-web-app" clearable size="large" />
          </el-form-item>
          <el-form-item label="项目路径">
            <el-input v-model="form.projectPath" placeholder="D:/project/my-app" clearable size="large" />
          </el-form-item>
          <el-button type="primary" :icon="Search" :loading="scanning" size="large" style="width: 100%" @click="handleScan">
            开始扫描
          </el-button>
        </el-form>
        <div class="scan-tips">
          <div class="scan-tip">
            <el-icon :size="14"><CircleCheck /></el-icon>
            默认跳过 node_modules、target、dist、.git
          </div>
          <div class="scan-tip">
            <el-icon :size="14"><CircleCheck /></el-icon>
            不会上传代码，只分析本地路径
          </div>
        </div>
      </div>
    </div>

    <!-- Right: Result -->
    <div class="scan-right">
      <div v-if="result" class="panel shared-slide-up">
        <div class="panel-header">
          <span>扫描结果</span>
          <RiskBadge :level="result.riskLevel" size="large" />
        </div>

        <div class="scan-meta-grid">
          <div class="scan-meta-item">
            <span class="scan-meta-label">项目类型</span>
            <span class="scan-meta-value">{{ projectTypeMap[result.projectType] || result.projectType || '—' }}</span>
          </div>
          <div class="scan-meta-item">
            <span class="scan-meta-label">文件数</span>
            <span class="scan-meta-value">{{ result.fileCount }}</span>
          </div>
          <div class="scan-meta-item">
            <span class="scan-meta-label">目录数</span>
            <span class="scan-meta-value">{{ result.directoryCount }}</span>
          </div>
          <div class="scan-meta-item">
            <span class="scan-meta-label">Git 仓库</span>
            <el-tag :type="result.hasGit ? 'success' : 'info'" size="small" effect="plain">{{ result.hasGit ? '已初始化' : '未初始化' }}</el-tag>
          </div>
          <div class="scan-meta-item">
            <span class="scan-meta-label">AGENTS.md</span>
            <el-tag :type="result.hasAgentsMd ? 'success' : 'danger'" size="small" effect="plain">{{ result.hasAgentsMd ? '已存在' : '缺失' }}</el-tag>
          </div>
        </div>

        <div class="shared-section-title">技术栈</div>
        <div class="tag-cloud">
          <el-tag v-for="tech in result.techStack" :key="tech" effect="plain" size="small">{{ tech }}</el-tag>
          <span v-if="!result.techStack?.length" class="empty-hint">未检测到</span>
        </div>

        <div class="shared-section-title">检测到的文件</div>
        <div class="tag-cloud">
          <el-tag v-for="f in result.detectedFiles" :key="f" effect="plain" size="small">{{ f }}</el-tag>
          <span v-if="!result.detectedFiles?.length" class="empty-hint">未检测到文件</span>
        </div>

        <div v-if="result.sensitiveFiles?.length" class="scan-alert">
          <div class="scan-alert-header">
            <el-icon :size="16" color="#EF4444"><Warning /></el-icon>
            <span>敏感文件 ({{ result.sensitiveFiles.length }})</span>
          </div>
          <div class="tag-cloud">
            <el-tag v-for="f in result.sensitiveFiles" :key="f" type="danger" effect="dark" size="small">{{ f }}</el-tag>
          </div>
        </div>

        <div v-if="result.suggestions?.length" class="shared-section-title">建议</div>
        <div v-if="result.suggestions?.length" class="suggestion-list">
          <div v-for="(s, i) in result.suggestions" :key="i" class="suggestion-item">
            <el-icon :size="14" color="#4F46E5"><CircleCheck /></el-icon>
            <span>{{ s }}</span>
          </div>
        </div>
      </div>

      <div v-else class="panel">
        <div class="scan-empty">
          <div class="scan-empty-icon">
            <el-icon :size="36" color="#CBD5E1"><Search /></el-icon>
          </div>
          <div class="scan-empty-title">输入项目信息开始扫描</div>
          <div class="scan-empty-desc">扫描后将获得以下信息：</div>
          <div class="scan-empty-features">
            <span>技术栈识别</span>
            <span>敏感文件检测</span>
            <span>Git 状态分析</span>
            <span>风险等级评估</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, inject, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, CircleCheck, Warning } from '@element-plus/icons-vue'
import RiskBadge from '../components/RiskBadge.vue'
import { scanProject } from '../api/scan'
import type { ScanResultVO } from '../types/scan'

const globalProjectId = inject<any>('globalProjectId', ref(null))
const globalProjectName = inject<any>('globalProjectName', ref(''))
const hasScannedProject = inject<any>('hasScannedProject', ref(false))

const projectTypeMap: Record<string, string> = {
  FULL_STACK: '全栈项目',
  BACKEND: '后端项目',
  FRONTEND: '前端项目',
  UNKNOWN: '未知类型'
}

const form = reactive({
  projectName: '',
  projectPath: ''
})

const scanning = ref(false)
const result = ref<ScanResultVO | null>(null)

watch(() => form.projectPath, (val) => {
  if (!val) return
  const normalized = val.replace(/\\/g, '/').replace(/\/+$/, '')
  const name = normalized.split('/').pop() || ''
  if (name) form.projectName = name
})

const handleScan = async () => {
  if (!form.projectName || !form.projectPath) {
    ElMessage.warning('请填写项目名称和路径')
    return
  }
  scanning.value = true
  try {
    const res = await scanProject({ projectName: form.projectName, projectPath: form.projectPath })
    if (res.data.code === 0) {
      result.value = res.data.data
      if (result.value.projectId) {
        globalProjectId.value = result.value.projectId
        globalProjectName.value = result.value.projectName
        hasScannedProject.value = true
        localStorage.setItem('agentguard_projectId', String(result.value.projectId))
        localStorage.setItem('agentguard_projectName', result.value.projectName)
        localStorage.setItem('agentguard_hasScanned', 'true')
      }
      ElMessage.success('扫描完成')
    } else {
      ElMessage.error(res.data.message)
    }
  } catch {
    ElMessage.error('扫描失败')
  } finally {
    scanning.value = false
  }
}
</script>

<style scoped>
.scan-page {
  display: grid;
  grid-template-columns: 380px 1fr;
  gap: 20px;
  align-items: start;
}

.scan-desc {
  font-size: 13px;
  color: #64748B;
  margin-bottom: 20px;
  line-height: 1.6;
}

.scan-tips {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #F1F5F9;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.scan-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #94A3B8;
}

.scan-meta-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  padding-bottom: 16px;
  border-bottom: 1px solid #F1F5F9;
  margin-bottom: 8px;
}

.scan-meta-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.scan-meta-label {
  font-size: 11px;
  color: #94A3B8;
}

.scan-meta-value {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
}

.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.empty-hint {
  font-size: 12px;
  color: #94A3B8;
}

.scan-alert {
  margin-top: 16px;
  background: rgba(239,68,68,0.04);
  border: 1px solid rgba(239,68,68,0.12);
  border-radius: 10px;
  padding: 14px;
}

.scan-alert-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #DC2626;
  margin-bottom: 10px;
}

.suggestion-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.suggestion-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 13px;
  color: #475569;
  line-height: 1.5;
}

.scan-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
}

.scan-empty-icon {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: #F1F5F9;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
}

.scan-empty-title {
  font-size: 16px;
  font-weight: 600;
  color: #334155;
  margin-bottom: 8px;
}

.scan-empty-desc {
  font-size: 13px;
  color: #94A3B8;
  margin-bottom: 16px;
}

.scan-empty-features {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}

.scan-empty-features span {
  font-size: 12px;
  color: #64748B;
  background: #F1F5F9;
  padding: 4px 12px;
  border-radius: 20px;
}

@media (max-width: 900px) {
  .scan-page { grid-template-columns: 1fr; }
}
</style>
