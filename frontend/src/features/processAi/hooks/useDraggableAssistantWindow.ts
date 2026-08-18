import { useState, type PointerEvent as ReactPointerEvent } from 'react'

interface Point { x: number; y: number }
interface Size { width: number; height: number }

export function useDraggableAssistantWindow() {
  const [position, setPosition] = useState<Point>(() => ({
    x: Math.max(12, window.innerWidth - 620),
    y: Math.max(12, window.innerHeight - 700),
  }))
  const [drag, setDrag] = useState<DragState>()

  const start = (event: ReactPointerEvent<HTMLElement>) => {
    if ((event.target as HTMLElement).closest('button')) return
    event.currentTarget.setPointerCapture(event.pointerId)
    setDrag({ pointerId: event.pointerId, origin: position,
      pointer: { x: event.clientX, y: event.clientY } })
  }
  const move = (event: ReactPointerEvent<HTMLElement>) => {
    if (!drag || drag.pointerId !== event.pointerId) return
    const dialog = event.currentTarget.parentElement?.getBoundingClientRect()
    const dialogSize = { width: dialog?.width ?? 580, height: dialog?.height ?? 680 }
    setPosition(clampAssistantPosition({
      x: drag.origin.x + event.clientX - drag.pointer.x,
      y: drag.origin.y + event.clientY - drag.pointer.y,
    }, { width: window.innerWidth, height: window.innerHeight }, dialogSize))
  }
  const stop = (event: ReactPointerEvent<HTMLElement>) => {
    if (drag?.pointerId === event.pointerId) setDrag(undefined)
  }

  const maxSize = {
    width: Math.max(0, window.innerWidth - position.x - 8),
    height: Math.max(0, window.innerHeight - position.y - 8),
  }
  return { dragging: Boolean(drag), maxSize, move, position, start, stop }
}

export function clampAssistantPosition(point: Point, viewport: Size, windowSize: Size): Point {
  return {
    x: Math.min(Math.max(8, point.x), Math.max(8, viewport.width - windowSize.width - 8)),
    y: Math.min(Math.max(8, point.y), Math.max(8, viewport.height - windowSize.height - 8)),
  }
}

interface DragState {
  pointerId: number
  origin: Point
  pointer: Point
}
