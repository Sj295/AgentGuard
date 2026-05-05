import http from './http'
import type { Result } from '../types/result'
import type {
  AiGitDiffAnalysisRequest,
  AiGitDiffAnalysisVO,
  AiReportSummaryRequest,
  AiReportSummaryVO,
  AiRiskExplainRequest,
  AiRiskExplainVO,
  AiRuntimeStatusVO
} from '../types/ai'

export function getAiStatus() {
  return http.get<Result<AiRuntimeStatusVO>>('/ai/status')
}

export function analyzeGitDiffWithAi(data: AiGitDiffAnalysisRequest) {
  return http.post<Result<AiGitDiffAnalysisVO>>('/ai/git-diff/analyze', data)
}

export function explainRiskWithAi(data: AiRiskExplainRequest) {
  return http.post<Result<AiRiskExplainVO>>('/ai/risk/explain', data)
}

export function summarizeReportWithAi(data: AiReportSummaryRequest) {
  return http.post<Result<AiReportSummaryVO>>('/ai/report/summary', data)
}
