import request from './request'
import { getPaper as getGeneratedPaper, listPapers } from './generated/paperReadOnly'
import type { PaperQuery, PaperSaveDTO } from '../types/paper'

export function pagePapers(query: PaperQuery) {
  return listPapers(query)
}

export function getPaper(uuid: string) {
  return getGeneratedPaper(uuid)
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
