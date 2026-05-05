export type AiAnalysisType = 'GIT_DIFF_ANALYSIS' | 'RISK_EXPLAIN' | 'REPORT_SUMMARY'

export interface AiAnalysisRecordVO {
  id: number
  projectId: number
  analysisType: AiAnalysisType
  sourceReportId?: number | null
  provider: string
  model: string
  mocked: boolean
  success: boolean
  latencyMs: number
  inputSummary?: string
  outputContent?: string
  errorMessage?: string
  createdTime: string
}

export interface AiRecordPageParams {
  current?: number
  size?: number
  analysisType?: AiAnalysisType | ''
}
