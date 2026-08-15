import type { ProjectMemoryPatchOperation, ProjectMemoryPatchOperationType } from './types'

const allowedPath = /^\/(?:rules|terms|examples|disabled)\/[A-Za-z0-9._-]{1,128}(?:\/.+)?$/

export function parseProjectMemoryOperations(text: string): ProjectMemoryPatchOperation[] {
  let value: unknown
  try {
    value = JSON.parse(text)
  } catch {
    throw new Error('补丁内容不是有效 JSON')
  }
  if (!Array.isArray(value) || value.length === 0 || value.length > 20) {
    throw new Error('补丁必须包含 1 到 20 个操作')
  }
  return value.map(parseOperation)
}

function parseOperation(value: unknown, index: number): ProjectMemoryPatchOperation {
  if (!isRecord(value)) throw operationError(index, '必须是对象')
  const op = operationType(value.op)
  if (!op) throw operationError(index, 'op 仅支持 add、replace、remove')
  if (typeof value.path !== 'string' || value.path.endsWith('/') || !allowedPath.test(value.path)) {
    throw operationError(index, 'path 不在允许范围内')
  }
  if (op === 'remove') {
    if (Object.hasOwn(value, 'value')) throw operationError(index, 'remove 不能提供 value')
    return { op, path: value.path }
  }
  if (!Object.hasOwn(value, 'value') || value.value === null) {
    throw operationError(index, 'add/replace 必须提供非空 value')
  }
  return { op, path: value.path, value: value.value }
}

function operationType(value: unknown): ProjectMemoryPatchOperationType | undefined {
  if (value === 'add' || value === 'replace' || value === 'remove') return value
  return undefined
}

function operationError(index: number, reason: string): Error {
  return new Error(`第 ${index + 1} 个补丁操作${reason}`)
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}
