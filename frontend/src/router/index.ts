import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '../layout/MainLayout.vue'
import DashboardView from '../views/Dashboard.vue'
import ScanView from '../views/ScanView.vue'
import AgentRuleView from '../views/AgentRuleView.vue'
import PermissionView from '../views/PermissionView.vue'
import CommandAuditView from '../views/CommandAuditView.vue'
import PreflightView from '../views/PreflightView.vue'
import GitAuditView from '../views/GitAuditView.vue'
import ReportView from '../views/ReportView.vue'
import TimelineView from '../views/TimelineView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: MainLayout,
      children: [
        { path: '', name: 'Dashboard', component: DashboardView },
        { path: 'scan', name: 'Scan', component: ScanView },
        { path: 'agent-rules', name: 'AgentRules', component: AgentRuleView },
        { path: 'permission', name: 'Permission', component: PermissionView },
        { path: 'command-audit', name: 'CommandAudit', component: CommandAuditView },
        { path: 'preflight', name: 'Preflight', component: PreflightView },
        { path: 'git-audit', name: 'GitAudit', component: GitAuditView },
        { path: 'reports', name: 'Reports', component: ReportView },
        { path: 'timeline', name: 'Timeline', component: TimelineView }
      ]
    }
  ]
})

export default router
