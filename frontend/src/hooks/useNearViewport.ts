import { useEffect, useRef, useState } from 'react'

export function useNearViewport<T extends Element>() {
  const targetRef = useRef<T>(null)
  const [isNearViewport, setIsNearViewport] = useState(false)

  useEffect(() => {
    const target = targetRef.current
    if (!target || isNearViewport) return
    if (!('IntersectionObserver' in window)) {
      setIsNearViewport(true)
      return
    }

    const observer = new IntersectionObserver(([entry]) => {
      if (!entry?.isIntersecting) return
      setIsNearViewport(true)
      observer.disconnect()
    })
    observer.observe(target)
    return () => observer.disconnect()
  }, [isNearViewport])

  return { isNearViewport, targetRef }
}
