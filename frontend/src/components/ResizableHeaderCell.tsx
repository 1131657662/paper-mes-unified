import type React from 'react'
import { Resizable } from 'react-resizable'
import type { ResizeCallbackData } from 'react-resizable'
import {
  resizableColumnMaxWidth,
  resizableColumnMinWidth,
} from './resizableTableStorage'
import './ResizableTable.css'

interface ResizableHeaderCellProps extends React.ThHTMLAttributes<HTMLTableCellElement> {
  width?: number
  onResize?: (event: React.SyntheticEvent, data: ResizeCallbackData) => void
}

export default function ResizableHeaderCell({
  onResize,
  width,
  ...restProps
}: ResizableHeaderCellProps) {
  if (!width || !onResize) return <th {...restProps} />

  const className = ['resizable-col-cell', restProps.className].filter(Boolean).join(' ')

  return (
    <Resizable
      width={width}
      height={0}
      minConstraints={[resizableColumnMinWidth, 0]}
      maxConstraints={[resizableColumnMaxWidth, 0]}
      handle={<span className="resizable-col-handle" onClick={(event) => event.stopPropagation()} />}
      onResize={onResize}
      draggableOpts={{ enableUserSelectHack: false }}
    >
      <th {...restProps} className={className} />
    </Resizable>
  )
}
