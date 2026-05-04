export interface ScanResultVO {
  projectId: number
  taskId: number
  projectName: string
  projectPath: string
  projectType: string
  techStack: string[]
  fileCount: number
  directoryCount: number
  hasGit: boolean
  hasAgentsMd: boolean
  detectedFiles: string[]
  sensitiveFiles: string[]
  riskLevel: string
  suggestions: string[]
}
