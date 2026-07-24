import request from './request'
import type { ProcessCatalog } from '../types/processCatalog'

export function getActiveProcessCatalog(): Promise<ProcessCatalog[]> {
  return request<ProcessCatalog[]>({
    url: '/api/process-catalog',
    method: 'get',
  })
}
