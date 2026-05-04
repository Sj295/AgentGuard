import http from './http'
import type { Result } from '../types/result'
import type { PageResult } from '../types/timeline'

export interface ProjectInfoVO {
  id: number
  projectName: string
  projectPath: string
  projectType: string
  techStack: string
  hasGit: boolean
  hasAgentsMd: boolean
  description: string | null
  createdTime: string
  updatedTime: string
}

export function getAllProjects(current: number = 1, size: number = 100) {
  return http.get<Result<PageResult<ProjectInfoVO>>>('/projects', { params: { current, size } })
}

export function getProjectById(id: number) {
  return http.get<Result<ProjectInfoVO>>(`/projects/${id}`)
}
