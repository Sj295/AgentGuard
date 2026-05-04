import http from './http'
import type {Result} from '../types/result'

export function getHealth() {
  return http.get<Result<string>>('/health')
}
