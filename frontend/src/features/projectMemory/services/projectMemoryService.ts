import { request } from '../../../api/request'
import type {
  ProjectMemoryCandidate,
  ProjectMemoryCandidateDetail,
  ProjectMemoryCandidateApprovePayload,
  ProjectMemoryCandidateRejectPayload,
  ProjectMemoryCandidateStatus,
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
  candidates: (status?: ProjectMemoryCandidateStatus) => request<ProjectMemoryCandidate[]>({
    url: '/api/ai/project-memory/candidates',
    method: 'get',
    params: status ? { status } : undefined,
  }),
  candidate: (uuid: string) => request<ProjectMemoryCandidateDetail>({
    url: `/api/ai/project-memory/candidates/${uuid}`,
    method: 'get',
  }),
  approveCandidate: ({ uuid, ...data }: ProjectMemoryCandidateApprovePayload) =>
    request<ProjectMemorySnapshot>({
      url: `/api/ai/project-memory/candidates/${uuid}/approve`,
      method: 'post',
      data,
    }),
  rejectCandidate: ({ uuid, ...data }: ProjectMemoryCandidateRejectPayload) =>
    request<ProjectMemoryCandidate>({
      url: `/api/ai/project-memory/candidates/${uuid}/reject`,
      method: 'post',
      data,
    }),
}
