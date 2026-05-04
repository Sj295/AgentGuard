import http from './http'
import type { Result } from '../types/result'
import type { ScanResultVO } from '../types/scan'

export function scanProject(data: { projectName: string; projectPath: string }) {
  return http.post<Result<ScanResultVO>>('/projects/scan', data)
}
