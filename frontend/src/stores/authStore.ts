import { create } from 'zustand'
import { createJSONStorage, persist } from 'zustand/middleware'
import { PERMISSIONS } from '../constants/permissions'
import { hasAnyPermission } from '../utils/permission'
import type { AuthUser } from '../types/auth'
import { clearCreateOrderLocalDraft } from '../features/processOrderCreate/localDraftStorage'

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

interface PersistedAuthState {
  permissions: string[]
  user: AuthUser | null
}

const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      permissions: [],
      actions: {
        signIn: (user, permissions = []) => set((state) => {
          if (state.user?.uuid && state.user.uuid !== user.uuid) clearCreateOrderLocalDraft()
          return { user: sanitizeUser(user), permissions }
        }),
        signOut: () => {
          clearCreateOrderLocalDraft()
          set({ user: null, permissions: [] })
        },
        syncCurrentUser: (user) => set({ user: sanitizeUser(user), permissions: user.permissions }),
        setPermissions: (permissions) => set({ permissions }),
      },
    }),
    {
      name: 'paper-mes-auth',
      storage: createJSONStorage(() => localStorage),
      version: 2,
      migrate: migrateAuthState,
      partialize: (state) => ({ user: sanitizeUser(state.user), permissions: state.permissions }),
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

function migrateAuthState(value: unknown): PersistedAuthState {
  if (!isRecord(value)) return { user: null, permissions: [] }
  const permissions = stringArray(value.permissions)
  return { user: parseUser(value.user, permissions), permissions }
}

function parseUser(value: unknown, permissions: string[]): AuthUser | null {
  if (!isRecord(value) || typeof value.username !== 'string') return null
  return sanitizeUser({
    permissions,
    username: value.username,
    realName: optionalString(value.realName),
    roleCode: optionalString(value.roleCode),
    uuid: optionalString(value.uuid),
  })
}

function sanitizeUser(user: AuthUser | null): AuthUser | null {
  if (!user) return null
  return {
    permissions: [...user.permissions],
    username: user.username,
    realName: user.realName,
    roleCode: user.roleCode,
    uuid: user.uuid,
  }
}

function stringArray(value: unknown): string[] {
  return Array.isArray(value) ? value.filter((item): item is string => typeof item === 'string') : []
}

function optionalString(value: unknown): string | undefined {
  return typeof value === 'string' ? value : undefined
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}
