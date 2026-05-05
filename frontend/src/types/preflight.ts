import type { RiskLevel, PreflightCheckStatus, AgentType, TaskType } from './enums'

export interface PreflightCheckVO {
  reportId: number
  projectId: number
  agentType: AgentType
  taskType: TaskType
  overallRiskLevel: RiskLevel
  score: number
  allowedToProceed: boolean
  checkItems: CheckItemVO[]
  riskItems: string[]
  suggestions: string[]
  recommendedActions: string[]
  createdTime: string
}

export interface CheckItemVO {
  name: string
  status: PreflightCheckStatus
  message: string
}
