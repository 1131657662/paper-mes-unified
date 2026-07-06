import { changePassword } from '../../../api/auth'
import type { ChangePasswordDTO } from '../../../types/auth'

export const authService = {
  changePassword: (data: ChangePasswordDTO) => changePassword(data),
}
