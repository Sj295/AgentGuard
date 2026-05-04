import http from './http'
import type { Result } from '../types/result'
import type { TimelineEventVO, ProjectSecurityOverviewVO, PageResult } from '../types/timeline'

export function getTimelineEvents(projectId: number, current: number = 1, size: number = 20, riskLevel?: string) {
  const params: Record<string, any> = { current, size }
  if (riskLevel) params.riskLevel = riskLevel
  return http.get<Result<PageResult<TimelineEventVO>>>(`/timeline/project/${projectId}`, { params })
}

export function getSecurityOverview(projectId: number) {
  return http.get<Result<ProjectSecurityOverviewVO>>(`/timeline/project/${projectId}/overview`)
}

export function getHighRiskEvents(projectId: number) {
  return http.get<Result<TimelineEventVO[]>>(`/timeline/project/${projectId}/high-risk`)
}
