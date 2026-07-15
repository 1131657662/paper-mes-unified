import { Modal } from 'antd'
import { useEffect, useRef, useState } from 'react'
import type { Dispatch, MutableRefObject, SetStateAction } from 'react'
import { useBlocker } from 'react-router-dom'
import type { Location } from 'react-router-dom'

interface GuardOptions {
  content?: string
  title?: string
}

interface NavigationBlockerOptions {
  content: string
  dirtyRef: MutableRefObject<boolean>
  setDirty: Dispatch<SetStateAction<boolean>>
  title: string
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
  })
  useBeforeUnload(isDirty)

  function updateDirty(nextDirty: boolean) {
    dirtyRef.current = nextDirty
    setDirty(nextDirty)
  }

  return {
    clearDirty: () => updateDirty(false),
    isDirty,
    markDirty: () => updateDirty(true),
  }
}

function useNavigationBlocker(options: NavigationBlockerOptions) {
  const { content, dirtyRef, setDirty, title } = options
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
        dirtyRef.current = false
        setDirty(false)
        blocker.proceed()
      },
    })
    return () => dialog.destroy()
  }, [blocker, content, dirtyRef, setDirty, title])
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
