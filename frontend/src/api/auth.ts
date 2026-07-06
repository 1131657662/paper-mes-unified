import request from './request'
import type { AuthUser, ChangePasswordDTO, LoginDTO } from '../types/auth'

export function login(dto: LoginDTO) {
  return request<AuthUser>({ url: '/api/auth/login', method: 'post', data: dto })
}

export function getCurrentUser() {
  return request<AuthUser>({ url: '/api/auth/me', method: 'get' })
}

export function changePassword(dto: ChangePasswordDTO) {
  return request<void>({ url: '/api/auth/password', method: 'post', data: dto })
}

export function logout() {
  return request<void>({ url: '/api/auth/logout', method: 'post' })
}
