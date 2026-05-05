export interface AiGitDiffAnalysisRequest {
  projectId: number
  gitAuditReportId: number
}

export interface AiRiskExplainRequest {
  projectId: number
  reportId: number
}

export interface AiReportSummaryRequest {
  projectId: number
  markdown: string
}

export interface AiGitDiffAnalysisVO {
  projectId: number
  gitAuditReportId: number
  summary: string
  impactAreas: string[]
  testSuggestions: string[]
  rollbackSuggestions: string[]
  confidenceNote: string
  mocked: boolean
}

export interface AiRiskExplainVO {
  projectId: number
  reportId: number
  riskSummary: string
  whyItMatters: string[]
  fixPlan: string[]
  safeNextSteps: string[]
  confidenceNote: string
  mocked: boolean
}

export interface AiReportSummaryVO {
  projectId: number
  executiveSummary: string
  keyFindings: string[]
  priorityActions: string[]
  confidenceNote: string
  mocked: boolean
}

export interface AiInsightSection {
  title: string
  items: string[]
}
