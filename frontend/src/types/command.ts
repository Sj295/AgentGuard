import type { RiskLevel } from './enums'

export interface CommandAuditVO {
  reportId: number
  projectId: number
  riskLevel: RiskLevel
  score: number
  riskItems: string[]
  safeAlternatives: string[]
  suggestions: string[]
  createdTime: string
}
