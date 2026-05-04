import type { RiskLevel } from './enums'

export interface TimelineEventVO {
  eventName: string
  eventType: string
  riskLevel: RiskLevel
  summary: string
  sourceType: string
  sourceId: number
  createdTime: string
}

export interface ProjectSecurityOverviewVO {
  projectId: number
  projectName: string
  totalEvents: number
  criticalCount: number
  highCount: number
  mediumCount: number
  lowCount: number
  latestRiskLevel: RiskLevel
  latestScanTime: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
}
