import http from './http'
import type { Result } from '../types/result'
import type { PreflightCheckVO } from '../types/preflight'
import type { RiskReportVO } from '../types/risk'
import type { AgentType, TaskType, SandboxMode, ApprovalPolicy } from '../types/enums'

export function runPreflightCheck(data: {
  projectId: number
  agentType: AgentType
  taskType: TaskType
  sandboxMode: SandboxMode
  approvalPolicy: ApprovalPolicy
  networkAccess: boolean
  allowDelete: boolean
  plannedCommands: string[]
}) {
  return http.post<Result<PreflightCheckVO>>('/preflight/check', data)
}

export function getPreflightReports(projectId: number) {
  return http.get<Result<RiskReportVO[]>>(`/preflight/reports/project/${projectId}`)
}
