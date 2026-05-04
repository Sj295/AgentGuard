export interface MarkdownReportVO {
  projectId: number
  projectName: string
  fileName: string
  targetPath?: string
  written: boolean
  markdown: string
  createdTime: string
}
