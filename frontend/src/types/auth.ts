export interface AuthUser {
  accessToken: string
  permissions: string[]
  realName?: string
  roleCode?: string
  username: string
  uuid?: string
}

export interface LoginDTO {
  password: string
  username: string
}

export interface ChangePasswordDTO {
  newPassword: string
  oldPassword: string
}
