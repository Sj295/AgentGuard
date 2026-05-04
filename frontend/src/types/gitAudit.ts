import type { RiskLevel } from './enums'

export interface GitDiffAuditVO {
  reportId: number
  projectId: number
  changedFileCount: number
  riskLevel: RiskLevel
  addedFiles: string[]
  modifiedFiles: string[]
  deletedFiles: string[]
  riskItems: string[]
  rollbackCommands: string[]
  suggestions: string[]
  createdTime: string
}
