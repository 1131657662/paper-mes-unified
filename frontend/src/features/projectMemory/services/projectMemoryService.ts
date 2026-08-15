import { request } from '../../../api/request'
import type {
  ProjectMemoryPatchPayload,
  ProjectMemoryRollbackPayload,
  ProjectMemorySnapshot,
  ProjectMemoryVersion,
} from '../types'

export const projectMemoryService = {
  current: () => request<ProjectMemorySnapshot>({
    url: '/api/ai/project-memory/current',
    method: 'get',
  }),
  versions: () => request<ProjectMemoryVersion[]>({
    url: '/api/ai/project-memory/versions',
    method: 'get',
  }),
  reload: () => request<ProjectMemorySnapshot>({
    url: '/api/ai/project-memory/reload',
    method: 'post',
  }),
  patch: (payload: ProjectMemoryPatchPayload) => request<ProjectMemorySnapshot>({
    url: '/api/ai/project-memory/patch',
    method: 'post',
    data: payload,
  }),
  rollback: (payload: ProjectMemoryRollbackPayload) => request<ProjectMemorySnapshot>({
    url: '/api/ai/project-memory/rollback',
    method: 'post',
    data: payload,
  }),
}
