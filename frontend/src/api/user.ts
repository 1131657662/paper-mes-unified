import request from './request'
import type { PageResult } from '../types/common'
import type {
  SystemUser,
  UserPasswordDTO,
  UserQuery,
  UserSaveDTO,
  UserStatusDTO,
} from '../types/user'

export function pageUsers(query: UserQuery) {
  return request<PageResult<SystemUser>>({
    url: '/api/users',
    method: 'get',
    params: query,
  })
}

export function getUser(uuid: string) {
  return request<SystemUser>({ url: `/api/users/${uuid}`, method: 'get' })
}

export function createUser(dto: UserSaveDTO) {
  return request<string>({ url: '/api/users', method: 'post', data: dto })
}

export function updateUser(uuid: string, dto: UserSaveDTO) {
  return request<void>({ url: `/api/users/${uuid}`, method: 'put', data: dto })
}

export function updateUserStatus(uuid: string, dto: UserStatusDTO) {
  return request<void>({ url: `/api/users/${uuid}/status`, method: 'put', data: dto })
}

export function resetUserPassword(uuid: string, dto: UserPasswordDTO) {
  return request<void>({ url: `/api/users/${uuid}/password`, method: 'put', data: dto })
}
