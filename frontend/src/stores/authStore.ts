import { create } from 'zustand'
import { createJSONStorage, persist } from 'zustand/middleware'
import { PERMISSIONS } from '../constants/permissions'
import { hasAnyPermission } from '../utils/permission'
import type { AuthUser } from '../types/auth'

interface AuthState {
  user: AuthUser | null
  permissions: string[]
  actions: {
    signIn: (user: AuthUser, permissions?: string[]) => void
    signOut: () => void
    syncCurrentUser: (user: AuthUser) => void
    setPermissions: (permissions: string[]) => void
  }
}

const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      permissions: [],
      actions: {
        signIn: (user, permissions = []) => set({ user, permissions }),
        signOut: () => set({ user: null, permissions: [] }),
        syncCurrentUser: (user) => set({ user, permissions: user.permissions }),
        setPermissions: (permissions) => set({ permissions }),
      },
    }),
    {
      name: 'paper-mes-auth',
      storage: createJSONStorage(() => localStorage),
      partialize: (state) => ({ user: state.user, permissions: state.permissions }),
    },
  ),
)

export function useAuthUser() {
  return useAuthStore((state) => state.user)
}

export function useAuthActions() {
  return useAuthStore((state) => state.actions)
}

export function useHasPermission(permission: string) {
  return useAuthStore((state) => {
    if (state.user?.roleCode === 'admin') return true
    return state.permissions.includes(PERMISSIONS.all) || state.permissions.includes(permission)
  })
}

export function useHasAnyPermission(permissions: string[]) {
  return useAuthStore((state) => {
    if (state.user?.roleCode === 'admin') return true
    return hasAnyPermission(state.permissions, permissions)
  })
}

export function getAuthSnapshot() {
  return useAuthStore.getState()
}
