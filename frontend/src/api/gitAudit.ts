import http from './http'
import type { Result } from '../types/result'
import type { GitDiffAuditVO } from '../types/gitAudit'
import type { RiskReportVO } from '../types/risk'

export function auditGitDiff(data: { projectId: number }) {
  return http.post<Result<GitDiffAuditVO>>('/git-audit/diff', data)
}

export function getGitAuditReport(reportId: number) {
  return http.get<Result<RiskReportVO>>(`/git-audit/reports/${reportId}`)
}
