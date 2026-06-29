import request from './request'
import type { PageResult } from '../types/common'
import type { Paper, PaperQuery, PaperSaveDTO } from '../types/paper'

export function pagePapers(query: PaperQuery) {
  return request<PageResult<Paper>>({
    url: '/api/papers',
    method: 'get',
    params: query,
  })
}

export function getPaper(uuid: string) {
  return request<Paper>({ url: `/api/papers/${uuid}`, method: 'get' })
}

export function createPaper(dto: PaperSaveDTO) {
  return request<string>({ url: '/api/papers', method: 'post', data: dto })
}

export function updatePaper(uuid: string, dto: PaperSaveDTO) {
  return request<void>({ url: `/api/papers/${uuid}`, method: 'put', data: dto })
}

export function deletePaper(uuid: string) {
  return request<void>({ url: `/api/papers/${uuid}`, method: 'delete' })
}
