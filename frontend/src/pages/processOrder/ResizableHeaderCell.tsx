import { Resizable } from 'react-resizable'
import type { ResizeCallbackData } from 'react-resizable'

export interface ResizableHeaderCellProps extends React.ThHTMLAttributes<HTMLTableCellElement> {
  width?: number
  onResize?: (event: React.SyntheticEvent, data: ResizeCallbackData) => void
}

export default function ResizableHeaderCell({
  onResize,
  width,
  ...restProps
}: ResizableHeaderCellProps) {
  if (!width || !onResize) return <th {...restProps} />

  return (
    <Resizable
      width={width}
      height={0}
      minConstraints={[72, 0]}
      handle={<span className="process-order-resize-handle" onClick={(event) => event.stopPropagation()} />}
      onResize={onResize}
      draggableOpts={{ enableUserSelectHack: false }}
    >
      <th {...restProps} />
    </Resizable>
  )
}
