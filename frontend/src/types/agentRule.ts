import type { AgentType } from './enums'

export interface AgentRuleVO {
  id: number
  projectId: number
  agentType: AgentType
  fileName: string
  suggestedPath?: string
  content: string
  createdTime: string
  updatedTime?: string
}

export interface AgentRuleWriteVO {
  ruleId: number
  projectId: number
  agentType: AgentType
  fileName: string
  targetPath: string
  backupPath?: string
  written: boolean
  overwritten: boolean
  message: string
}
