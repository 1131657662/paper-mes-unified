import { useRef, useState } from 'react'
import type { MouseEvent, PointerEvent, ReactNode } from 'react'
import './ResizableWorkspace.css'

interface Props {
  leftTitle: string
  mainTitle: string
  rightTitle?: string
  left: ReactNode
  main: ReactNode
  right?: ReactNode
  leftInitial?: number
  rightInitial?: number
}

type DragTarget = 'left' | 'right'

export default function ResizableWorkspace({
  leftTitle,
  mainTitle,
  rightTitle,
  left,
  main,
  right,
  leftInitial = 28,
  rightInitial = 28,
}: Props) {
  const workspaceRef = useRef<HTMLDivElement>(null)
  const draggingRef = useRef<DragTarget>()
  const [leftWidth, setLeftWidth] = useState(leftInitial)
  const [rightWidth, setRightWidth] = useState(right ? rightInitial : 0)
  const [dragging, setDragging] = useState<DragTarget>()
  const mainWidth = Math.max(24, 100 - leftWidth - (right ? rightWidth : 0))

  const gridTemplateColumns = right
    ? `${leftWidth}fr 10px minmax(0, ${mainWidth}fr) 10px ${rightWidth}fr`
    : `${leftWidth}fr 10px minmax(0, ${mainWidth}fr)`

  return (
    <div
      ref={workspaceRef}
      className="process-workspace"
      style={{ gridTemplateColumns }}
      onPointerMove={dragWorkspace}
      onPointerUp={stopDragging}
      onPointerCancel={stopDragging}
      onMouseMove={dragWorkspace}
      onMouseUp={stopDragging}
      onMouseLeave={stopDragging}
    >
      <WorkspacePane title={leftTitle}>{left}</WorkspacePane>
      <Divider target="left" dragging={dragging} onDragStart={startDragging} onDrag={dragPane} />
      <WorkspacePane title={mainTitle}>{main}</WorkspacePane>
      {right && <Divider target="right" dragging={dragging} onDragStart={startDragging} onDrag={dragPane} />}
      {right && <WorkspacePane title={rightTitle ?? ''}>{right}</WorkspacePane>}
    </div>
  )

  function startDragging(target?: DragTarget) {
    draggingRef.current = target
    setDragging(target)
  }

  function stopDragging() {
    startDragging(undefined)
  }

  function dragWorkspace(event: PointerEvent<HTMLDivElement> | MouseEvent<HTMLDivElement>) {
    if (!draggingRef.current) return
    dragPane(draggingRef.current, event.clientX)
  }

  function dragPane(target: DragTarget, clientX: number) {
    const rect = workspaceRef.current?.getBoundingClientRect()
    if (!rect || rect.width <= 0) return
    const percent = ((clientX - rect.left) / rect.width) * 100
    if (target === 'left') setLeftWidth(clamp(percent, 18, 100 - 24 - (right ? rightWidth : 0)))
    if (target === 'right') setRightWidth(clamp(100 - percent, 20, 100 - 24 - leftWidth))
  }
}

function WorkspacePane({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="process-workspace__pane">
      <div className="process-workspace__pane-header">{title}</div>
      <div className="process-workspace__pane-body">{children}</div>
    </section>
  )
}

function Divider({ target, dragging, onDragStart, onDrag }: DividerProps) {
  return (
    <div
      className={`process-workspace__divider ${dragging === target ? 'process-workspace__divider--dragging' : ''}`}
      onPointerDown={(event) => {
        event.preventDefault()
        event.currentTarget.setPointerCapture(event.pointerId)
        onDragStart(target)
      }}
      onPointerMove={(event) => {
        if (dragging === target || event.currentTarget.hasPointerCapture(event.pointerId)) {
          onDrag(target, event.clientX)
        }
      }}
      onPointerUp={(event) => {
        if (event.currentTarget.hasPointerCapture(event.pointerId)) {
          event.currentTarget.releasePointerCapture(event.pointerId)
        }
        onDragStart(undefined)
      }}
      onPointerCancel={() => onDragStart(undefined)}
      onLostPointerCapture={() => {
        onDragStart(undefined)
      }}
      onMouseDown={(event) => {
        event.preventDefault()
        onDragStart(target)
      }}
    />
  )
}

interface DividerProps {
  target: DragTarget
  dragging?: DragTarget
  onDragStart: (target?: DragTarget) => void
  onDrag: (target: DragTarget, clientX: number) => void
}

function clamp(value: number, min: number, max: number) {
  return Math.min(max, Math.max(min, value))
}
