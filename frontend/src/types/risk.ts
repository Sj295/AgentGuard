import type { RiskLevel, AgentType, TaskType, SandboxMode, ApprovalPolicy } from './enums'

export interface PermissionAssessResultVO {
  reportId: number
  projectId: number
  agentType: AgentType
  riskLevel: RiskLevel
  score: number
  riskItems: string[]
  suggestions: string[]
  recommendedConfig: RecommendedConfigVO
  createdTime: string
}

export interface RecommendedConfigVO {
  sandboxMode: SandboxMode
  approvalPolicy: ApprovalPolicy
  networkAccess: boolean
  allowDelete: boolean
}

export interface RiskReportVO {
  reportId: number
  projectId: number
  reportType: string
  riskLevel: RiskLevel
  riskScore?: number
  summary: string
  payloadJson?: string
  createdTime: string
}
