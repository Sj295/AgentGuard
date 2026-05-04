import http from './http'
import type { Result } from '../types/result'
import type { PermissionAssessResultVO, RiskReportVO } from '../types/risk'
import type { AgentType, TaskType, SandboxMode, ApprovalPolicy } from '../types/enums'

export function assessPermission(data: {
  projectId: number
  agentType: AgentType
  taskType: TaskType
  sandboxMode: SandboxMode
  approvalPolicy: ApprovalPolicy
  networkAccess: boolean
  allowDelete: boolean
}) {
  return http.post<Result<PermissionAssessResultVO>>('/risk/permission-assess', data)
}

export function getRiskReportById(reportId: number) {
  return http.get<Result<RiskReportVO>>(`/risk/reports/${reportId}`)
}

export function getRiskReportsByType(projectId: number, reportType: string) {
  return http.get<Result<RiskReportVO[]>>(`/risk/reports/project/${projectId}/type/${reportType}`)
}
