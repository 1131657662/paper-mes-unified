import { EyeOutlined } from '@ant-design/icons'
import { Button, Drawer } from 'antd'
import { useRef, useState } from 'react'
import type { CSSProperties, KeyboardEvent, PointerEvent, ReactNode } from 'react'
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
  leftInitial = 25,
  rightInitial = 26,
}: Props) {
  const workspaceRef = useRef<HTMLDivElement>(null)
  const draggingRef = useRef<DragTarget | undefined>(undefined)
  const [leftWidth, setLeftWidth] = useState(leftInitial)
  const [rightWidth, setRightWidth] = useState(right ? rightInitial : 0)
  const [dragging, setDragging] = useState<DragTarget>()
  const [rightOpen, setRightOpen] = useState(false)
  const mainWidth = Math.max(24, 100 - leftWidth - (right ? rightWidth : 0))
  const workspaceStyle = {
    '--process-workspace-left': `${leftWidth}fr`,
    '--process-workspace-left-percent': `${leftWidth}%`,
    '--process-workspace-main': `${mainWidth}fr`,
    '--process-workspace-right': `${rightWidth}fr`,
    '--process-workspace-right-percent': `${rightWidth}%`,
  } as CSSProperties

  return (
    <div className="process-workspace-shell">
      <div
        ref={workspaceRef}
        className={`process-workspace${right ? ' process-workspace--with-right' : ''}`}
        style={workspaceStyle}
        onPointerMove={dragWorkspace}
        onPointerUp={stopDragging}
        onPointerCancel={stopDragging}
        onPointerLeave={stopDragging}
      >
        <WorkspacePane kind="left" title={leftTitle}>{left}</WorkspacePane>
        <Divider target="left" value={leftWidth} dragging={dragging}
          onDragStart={startDragging} onDrag={dragPane} onKeyResize={resizeBy} onReset={resetPane} />
        <WorkspacePane kind="main" title={mainTitle}>{main}</WorkspacePane>
        {right && <Divider target="right" value={rightWidth} dragging={dragging}
          onDragStart={startDragging} onDrag={dragPane} onKeyResize={resizeBy} onReset={resetPane} />}
        {right && <WorkspacePane kind="right" title={rightTitle ?? ''}>{right}</WorkspacePane>}
        {right && (
          <Button className="process-workspace__preview-trigger" type="text" icon={<EyeOutlined />}
            aria-label={`打开${rightTitle ?? '预览'}`} onClick={() => setRightOpen(true)} />
        )}
      </div>
      {right && (
        <Drawer title={rightTitle ?? '预览'} open={rightOpen} width="min(520px, 92vw)"
          onClose={() => setRightOpen(false)}>
          {right}
        </Drawer>
      )}
    </div>
  )

  function startDragging(target?: DragTarget) {
    draggingRef.current = target
    setDragging(target)
  }

  function stopDragging() {
    startDragging(undefined)
  }

  function dragWorkspace(event: PointerEvent<HTMLDivElement>) {
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

  function resizeBy(target: DragTarget, delta: number) {
    if (target === 'left') setLeftWidth((current) => clamp(current + delta, 18, 58))
    if (target === 'right') setRightWidth((current) => clamp(current + delta, 20, 58))
  }

  function resetPane(target: DragTarget) {
    if (target === 'left') setLeftWidth(leftInitial)
    if (target === 'right') setRightWidth(rightInitial)
  }
}

function WorkspacePane({ title, children, kind }: { title: string; children: ReactNode; kind: DragTarget | 'main' }) {
  return (
    <section className={`process-workspace__pane process-workspace__pane--${kind}`}>
      <div className="process-workspace__pane-header">{title}</div>
      <div className="process-workspace__pane-body">{children}</div>
    </section>
  )
}

function Divider({ target, value, dragging, onDragStart, onDrag, onKeyResize, onReset }: DividerProps) {
  return (
    <div
      className={`process-workspace__divider process-workspace__divider--${target} ${dragging === target ? 'process-workspace__divider--dragging' : ''}`}
      role="separator"
      tabIndex={0}
      title="拖动或使用方向键调整宽度，双击复位"
      aria-label={`调整${target === 'left' ? '左侧列表' : '右侧预览'}宽度`}
      aria-orientation="vertical"
      aria-valuemin={18}
      aria-valuemax={58}
      aria-valuenow={Math.round(value)}
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
      onDoubleClick={() => onReset(target)}
      onKeyDown={(event) => handleDividerKey(event, target, onKeyResize, onReset)}
    />
  )
}

interface DividerProps {
  target: DragTarget
  dragging?: DragTarget
  value: number
  onDragStart: (target?: DragTarget) => void
  onDrag: (target: DragTarget, clientX: number) => void
  onKeyResize: (target: DragTarget, delta: number) => void
  onReset: (target: DragTarget) => void
}

function handleDividerKey(
  event: KeyboardEvent<HTMLDivElement>,
  target: DragTarget,
  resize: (target: DragTarget, delta: number) => void,
  reset: (target: DragTarget) => void,
) {
  if (event.key === 'Home') {
    event.preventDefault()
    reset(target)
    return
  }
  if (event.key !== 'ArrowLeft' && event.key !== 'ArrowRight') return
  event.preventDefault()
  const direction = event.key === 'ArrowRight' ? 1 : -1
  resize(target, target === 'left' ? direction * 2 : direction * -2)
}

function clamp(value: number, min: number, max: number) {
  return Math.min(max, Math.max(min, value))
}
