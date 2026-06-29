import request from './request'
import type { PageResult } from '../types/common'
import type { Machine, MachineQuery, MachineSaveDTO } from '../types/machine'

export function pageMachines(query: MachineQuery) {
  return request<PageResult<Machine>>({
    url: '/api/machines',
    method: 'get',
    params: query,
  })
}

export function getMachine(uuid: string) {
  return request<Machine>({ url: `/api/machines/${uuid}`, method: 'get' })
}

export function createMachine(dto: MachineSaveDTO) {
  return request<string>({ url: '/api/machines', method: 'post', data: dto })
}

export function updateMachine(uuid: string, dto: MachineSaveDTO) {
  return request<void>({ url: `/api/machines/${uuid}`, method: 'put', data: dto })
}

export function deleteMachine(uuid: string) {
  return request<void>({ url: `/api/machines/${uuid}`, method: 'delete' })
}
