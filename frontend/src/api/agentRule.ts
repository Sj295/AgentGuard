import http from './http'
import type { Result } from '../types/result'
import type { AgentRuleVO, AgentRuleWriteVO } from '../types/agentRule'
import type { AgentType } from '../types/enums'

export function generateAgentRule(data: { projectId: number; agentType: AgentType }) {
  return http.post<Result<AgentRuleVO>>('/agent-rules/generate', data)
}

export function getAgentRuleById(id: number) {
  return http.get<Result<AgentRuleVO>>(`/agent-rules/${id}`)
}

export function getLatestAgentRules(projectId: number) {
  return http.get<Result<AgentRuleVO[]>>(`/agent-rules/project/${projectId}`)
}

export function getLatestAgentRule(projectId: number, agentType: AgentType) {
  return http.get<Result<AgentRuleVO>>(`/agent-rules/project/${projectId}/latest`, { params: { agentType } })
}

export function writeAgentRule(id: number, data: { overwrite: boolean; backup: boolean }) {
  return http.post<Result<AgentRuleWriteVO>>(`/agent-rules/${id}/write`, data)
}
