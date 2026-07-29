import { Modal, message } from 'antd'
import { useCallback, useEffect, useRef, useState } from 'react'
import type { Dispatch, MutableRefObject, SetStateAction } from 'react'
import { useBlocker } from 'react-router'
import type { Location } from 'react-router'

interface GuardOptions {
  content?: string
  onDiscard?: () => void
  pending?: boolean
  pendingContent?: string
  title?: string
}

interface NavigationBlockerOptions {
  content: string
  dirtyRef: MutableRefObject<boolean>
  pendingContent: string
  pendingRef: MutableRefObject<boolean>
  setDirty: Dispatch<SetStateAction<boolean>>
  title: string
  onDiscard?: () => void
}

type RouteLocation = Pick<Location, 'hash' | 'pathname' | 'search'>

const defaultTitle = '存在未保存修改'
const defaultContent = '当前表单有未保存的修改，确定要离开吗？'
const defaultPendingContent = '正在保存，请稍候再离开当前页面'

export function useUnsavedChangesGuard(options: GuardOptions = {}) {
  const dirtyRef = useRef(false)
  const pendingRef = useRef(options.pending === true)
  const [isDirty, setDirty] = useState(false)
  pendingRef.current = options.pending === true

  useNavigationBlocker({
    content: options.content ?? defaultContent,
    dirtyRef,
    pendingContent: options.pendingContent ?? defaultPendingContent,
    pendingRef,
    setDirty,
    title: options.title ?? defaultTitle,
    onDiscard: options.onDiscard,
  })
  useBeforeUnload(isDirty || options.pending === true)

  const updateDirty = useCallback((nextDirty: boolean) => {
    dirtyRef.current = nextDirty
    setDirty(nextDirty)
  }, [])
  const clearDirty = useCallback(() => updateDirty(false), [updateDirty])
  const markDirty = useCallback(() => updateDirty(true), [updateDirty])
  const runIfClean = useCallback((action: () => void) => runGuardedAction(
    dirtyRef,
    pendingRef,
    setDirty,
    action,
    {
      content: options.content ?? defaultContent,
      onDiscard: options.onDiscard,
      pendingContent: options.pendingContent ?? defaultPendingContent,
      title: options.title ?? defaultTitle,
    },
  ), [options.content, options.onDiscard, options.pendingContent, options.title])

  return {
    clearDirty,
    runIfClean,
    isDirty,
    markDirty,
  }
}

function runGuardedAction(
  dirtyRef: MutableRefObject<boolean>,
  pendingRef: MutableRefObject<boolean>,
  setDirty: Dispatch<SetStateAction<boolean>>,
  action: () => void,
  options: GuardOptions & Required<Pick<GuardOptions, 'content' | 'title'>>,
) {
  if (pendingRef.current) {
    message.info(options.pendingContent)
    return
  }
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
  const { content, dirtyRef, onDiscard, pendingContent, pendingRef, setDirty, title } = options
  const blocker = useBlocker(({ currentLocation, nextLocation }) => (
    shouldBlockWorkflowNavigation(
      dirtyRef.current,
      pendingRef.current,
      currentLocation,
      nextLocation,
    )
  ))

  useEffect(() => {
    if (blocker.state !== 'blocked') return
    if (pendingRef.current) {
      const dialog = Modal.info({
        content: pendingContent,
        onOk: () => blocker.reset(),
        title: '正在保存',
      })
      return () => dialog.destroy()
    }
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
  }, [blocker, content, dirtyRef, onDiscard, pendingContent, pendingRef, setDirty, title])
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

export function shouldBlockWorkflowNavigation(
  isDirty: boolean,
  isPending: boolean,
  currentLocation: RouteLocation,
  nextLocation: RouteLocation,
) {
  return (isDirty || isPending) && routeKey(currentLocation) !== routeKey(nextLocation)
}

export function preventUnsavedUnload(event: BeforeUnloadEvent) {
  event.preventDefault()
  event.returnValue = ''
}

function routeKey(location: RouteLocation) {
  return `${location.pathname}${location.search}${location.hash}`
}
