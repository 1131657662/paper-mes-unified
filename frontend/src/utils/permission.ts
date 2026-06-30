import { PERMISSIONS } from '../constants/permissions'

export function hasAnyPermission(owned: string[] | undefined, permissions: string[]) {
  if (!owned?.length) return false
  if (owned.includes(PERMISSIONS.all)) return true
  return permissions.some((permission) => owned.includes(permission))
}
