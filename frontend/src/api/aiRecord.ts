import http from './http'
import type { Result } from '../types/result'
import type { PageResult } from '../types/timeline'
import type { AiAnalysisRecordVO, AiRecordPageParams } from '../types/aiRecord'

export function getProjectAiRecords(projectId: number, params: AiRecordPageParams = {}) {
  const query: Record<string, any> = {
    current: params.current ?? 1,
    size: params.size ?? 10
  }
  if (params.analysisType) {
    query.analysisType = params.analysisType
  }
  return http.get<Result<PageResult<AiAnalysisRecordVO>>>(`/ai/records/project/${projectId}`, { params: query })
}

export function getAiRecordDetail(id: number) {
  return http.get<Result<AiAnalysisRecordVO>>(`/ai/records/${id}`)
}

export function getLatestAiRecords(projectId: number, limit: number = 5) {
  return http.get<Result<AiAnalysisRecordVO[]>>(`/ai/records/project/${projectId}/latest`, { params: { limit } })
}
