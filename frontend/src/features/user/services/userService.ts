import {
  createUser,
  getUser,
  pageUsers,
  resetUserPassword,
  updateUser,
  updateUserStatus,
} from '../../../api/user'
import type {
  UserPasswordDTO,
  UserQuery,
  UserSaveDTO,
  UserStatusDTO,
} from '../../../types/user'

export const userService = {
  create: (data: UserSaveDTO) => createUser(data),
  detail: (uuid: string) => getUser(uuid),
  list: (query: UserQuery) => pageUsers(query),
  resetPassword: (params: { uuid: string; data: UserPasswordDTO }) =>
    resetUserPassword(params.uuid, params.data),
  update: (params: { uuid: string; data: UserSaveDTO }) => updateUser(params.uuid, params.data),
  updateStatus: (params: { uuid: string; data: UserStatusDTO }) =>
    updateUserStatus(params.uuid, params.data),
}
