import request from './request'
import type { PageResult } from '../types/common'
import type { OperationLog, OperationLogQuery } from '../types/operationLog'

export function getOperationLogs(query: OperationLogQuery) {
  return request<PageResult<OperationLog>>({
    url: '/api/operation-logs',
    method: 'get',
    params: query,
  })
}
