import { Modal } from 'antd'
import { useCallback, useEffect, useRef, useState } from 'react'
import type { Dispatch, MutableRefObject, SetStateAction } from 'react'
import { useBlocker } from 'react-router-dom'
import type { Location } from 'react-router-dom'

interface GuardOptions {
  content?: string
  onDiscard?: () => void
  title?: string
}

interface NavigationBlockerOptions {
  content: string
  dirtyRef: MutableRefObject<boolean>
  setDirty: Dispatch<SetStateAction<boolean>>
  title: string
  onDiscard?: () => void
}

type RouteLocation = Pick<Location, 'hash' | 'pathname' | 'search'>

const defaultTitle = '存在未保存修改'
const defaultContent = '当前表单有未保存的修改，确定要离开吗？'

export function useUnsavedChangesGuard(options: GuardOptions = {}) {
  const dirtyRef = useRef(false)
  const [isDirty, setDirty] = useState(false)

  useNavigationBlocker({
    content: options.content ?? defaultContent,
    dirtyRef,
    setDirty,
    title: options.title ?? defaultTitle,
    onDiscard: options.onDiscard,
  })
  useBeforeUnload(isDirty)

  const updateDirty = useCallback((nextDirty: boolean) => {
    dirtyRef.current = nextDirty
    setDirty(nextDirty)
  }, [])
  const clearDirty = useCallback(() => updateDirty(false), [updateDirty])
  const markDirty = useCallback(() => updateDirty(true), [updateDirty])
  const runIfClean = useCallback((action: () => void) => runGuardedAction(
    dirtyRef,
    setDirty,
    action,
    {
      content: options.content ?? defaultContent,
      onDiscard: options.onDiscard,
      title: options.title ?? defaultTitle,
    },
  ), [options.content, options.onDiscard, options.title])

  return {
    clearDirty,
    runIfClean,
    isDirty,
    markDirty,
  }
}

function runGuardedAction(
  dirtyRef: MutableRefObject<boolean>,
  setDirty: Dispatch<SetStateAction<boolean>>,
  action: () => void,
  options: GuardOptions & Required<Pick<GuardOptions, 'content' | 'title'>>,
) {
  if (!dirtyRef.current) {
    action()
    return
  }
  Modal.confirm({
    cancelText: '继续编辑',
    content: options.content,
    okText: '放弃修改并离开',
    title: options.title,
    onOk: () => {
      options.onDiscard?.()
      dirtyRef.current = false
      setDirty(false)
      action()
    },
  })
}

function useNavigationBlocker(options: NavigationBlockerOptions) {
  const { content, dirtyRef, onDiscard, setDirty, title } = options
  const blocker = useBlocker(({ currentLocation, nextLocation }) => (
    shouldBlockUnsavedNavigation(dirtyRef.current, currentLocation, nextLocation)
  ))

  useEffect(() => {
    if (blocker.state !== 'blocked') return
    const dialog = Modal.confirm({
      cancelText: '继续编辑',
      content,
      okText: '离开页面',
      title,
      onCancel: () => blocker.reset(),
      onOk: () => {
        onDiscard?.()
        dirtyRef.current = false
        setDirty(false)
        blocker.proceed()
      },
    })
    return () => dialog.destroy()
  }, [blocker, content, dirtyRef, onDiscard, setDirty, title])
}

function useBeforeUnload(isDirty: boolean) {
  useEffect(() => {
    if (!isDirty) return
    window.addEventListener('beforeunload', preventUnsavedUnload)
    return () => window.removeEventListener('beforeunload', preventUnsavedUnload)
  }, [isDirty])
}

export function shouldBlockUnsavedNavigation(
  isDirty: boolean,
  currentLocation: RouteLocation,
  nextLocation: RouteLocation,
) {
  return isDirty && routeKey(currentLocation) !== routeKey(nextLocation)
}

export function preventUnsavedUnload(event: BeforeUnloadEvent) {
  event.preventDefault()
  event.returnValue = ''
}

function routeKey(location: RouteLocation) {
  return `${location.pathname}${location.search}${location.hash}`
}
