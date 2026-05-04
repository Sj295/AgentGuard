import http from './http'
import type { Result } from '../types/result'
import type { MarkdownReportVO } from '../types/report'

export function generateMarkdownReport(data: {
  projectId: number
  includeScanResult: boolean
  includeAgentRules: boolean
  includeRiskReports: boolean
  includeGitAudit: boolean
  includePreflight: boolean
}) {
  return http.post<Result<MarkdownReportVO>>('/reports/markdown/generate', data)
}

export function exportMarkdownReport(data: { projectId: number; overwrite: boolean }) {
  return http.post<Result<MarkdownReportVO>>('/reports/markdown/export', data)
}
