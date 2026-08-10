import { useEffect, type RefObject } from 'react'

export function useRouteFocus(contentRef: RefObject<HTMLElement | null>, routeKey: string) {
  useEffect(() => {
    const content = contentRef.current
    if (!content) return

    content.scrollTo({ top: 0, left: 0 })
    if (focusRouteHeading(content)) return

    content.focus({ preventScroll: true })
    const observer = new MutationObserver(() => {
      if (!focusRouteHeading(content)) return
      observer.disconnect()
    })
    observer.observe(content, { childList: true, subtree: true })
    return () => observer.disconnect()
  }, [contentRef, routeKey])
}

function focusRouteHeading(content: HTMLElement) {
  const heading = content.querySelector<HTMLElement>('h1')
  if (!heading) return false
  heading.tabIndex = -1
  heading.focus({ preventScroll: true })
  return true
}
