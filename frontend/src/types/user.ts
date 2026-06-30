import type { PageQuery } from './common'

export type UserRoleCode = 'admin' | 'operator' | 'finance' | 'warehouse'
export type UserStatus = 0 | 1

export interface SystemUser {
  uuid: string
  username: string
  realName: string
  roleCode: UserRoleCode
  status: UserStatus
  lastLoginTime?: string
  remark?: string
  createBy?: string
  updateBy?: string
  createTime?: string
  updateTime?: string
}

export interface UserQuery extends PageQuery {
  keyword?: string
  roleCode?: UserRoleCode
  status?: UserStatus
}

export interface UserSaveDTO {
  username: string
  password?: string
  realName: string
  roleCode: UserRoleCode
  status?: UserStatus
  remark?: string
}

export interface UserStatusDTO {
  status: UserStatus
}

export interface UserPasswordDTO {
  password: string
}
