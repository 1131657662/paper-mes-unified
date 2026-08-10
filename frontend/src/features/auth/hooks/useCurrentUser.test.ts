import { beforeEach, describe, expect, it, vi } from 'vitest'

const { useQuery } = vi.hoisted(() => ({
  useQuery: vi.fn((options: unknown) => options),
}))

vi.mock('@tanstack/react-query', () => ({ useQuery }))

import { useCurrentUser } from './useCurrentUser'

describe('useCurrentUser', () => {
  beforeEach(() => useQuery.mockClear())

  it('refreshes the current user whenever the window regains focus', () => {
    useCurrentUser(true)

    expect(useQuery).toHaveBeenCalledWith(expect.objectContaining({
      enabled: true,
      refetchOnWindowFocus: 'always',
      retry: false,
    }))
  })

  it('keeps the current-user request disabled when auth is not required', () => {
    useCurrentUser(false)

    expect(useQuery).toHaveBeenCalledWith(expect.objectContaining({ enabled: false }))
  })
})
