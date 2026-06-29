import request from './request'
import type { AuthUser, LoginDTO } from '../types/auth'

export function login(dto: LoginDTO) {
  return request<AuthUser>({ url: '/api/auth/login', method: 'post', data: dto })
}

export function getCurrentUser() {
  return request<AuthUser>({ url: '/api/auth/me', method: 'get' })
}

export function logout() {
  return request<void>({ url: '/api/auth/logout', method: 'post' })
}
