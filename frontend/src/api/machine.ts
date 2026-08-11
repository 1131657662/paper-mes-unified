import request from './request'
import { getMachine as getGeneratedMachine, listMachines } from './generated/machineReadOnly'
import type { MachineQuery, MachineSaveDTO } from '../types/machine'

export function pageMachines(query: MachineQuery) {
  return listMachines(query)
}

export function getMachine(uuid: string) {
  return getGeneratedMachine(uuid)
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
