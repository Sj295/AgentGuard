import http from './http'
import type { Result } from '../types/result'
import type { CommandAuditVO } from '../types/command'
import type { RiskReportVO } from '../types/risk'

export function auditCommands(data: { projectId: number; commands: string[] }) {
  return http.post<Result<CommandAuditVO>>('/commands/audit', data)
}

export function getCommandAuditReports(projectId: number) {
  return http.get<Result<RiskReportVO[]>>(`/commands/reports/project/${projectId}`)
}
