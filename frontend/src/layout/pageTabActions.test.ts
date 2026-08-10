import { describe, expect, it, vi } from 'vitest'
import type { NavigateFunction } from 'react-router'
import { createPageTabActions } from './pageTabActions'
import type { PageTabItem } from './pageTabModel'

const tabs: PageTabItem[] = [
  { path: '/dashboard', label: 'Dashboard', closable: false },
  { path: '/customers', label: 'Customers', closable: true },
  { path: '/papers', label: 'Papers', closable: true },
]

function createActions(activePath: string) {
  const navigate = vi.fn() as unknown as NavigateFunction
  const closeTab = vi.fn()
  const closeLeftTabs = vi.fn()
  const closeRightTabs = vi.fn()
  const closeOtherTabs = vi.fn()
  const closeAllTabs = vi.fn()
  const actions = createPageTabActions({
    activePath,
    closeAllTabs,
    closeLeftTabs,
    closeOtherTabs,
    closeRightTabs,
    closeTab,
    navigate,
    tabs,
  })
  return { actions, closeAllTabs, closeLeftTabs, closeTab, navigate }
}

describe('pageTabActions', () => {
  it('closes the active tab and navigates to the previous tab', () => {
    const { actions, closeTab, navigate } = createActions('/papers')

    actions.closeCurrent()

    expect(closeTab).toHaveBeenCalledWith('/papers')
    expect(navigate).toHaveBeenCalledWith('/customers')
  })

  it('navigates to the target when closing the active tab from its left', () => {
    const { actions, closeLeftTabs, navigate } = createActions('/customers')

    actions.handleMenuAction('close-left', '/papers')

    expect(closeLeftTabs).toHaveBeenCalledWith('/papers')
    expect(navigate).toHaveBeenCalledWith('/papers')
  })

  it('clears closable tabs and returns to the dashboard for close-all', () => {
    const { actions, closeAllTabs, navigate } = createActions('/papers')

    actions.handleMenuAction('close-all')

    expect(closeAllTabs).toHaveBeenCalledOnce()
    expect(navigate).toHaveBeenCalledWith('/dashboard')
  })
})
