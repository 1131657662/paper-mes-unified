import { Tooltip } from 'antd'
import type { TooltipProps } from 'antd'

export const MES_TOOLTIP_ENTER_DELAY = 0.45
export const MES_TOOLTIP_LEAVE_DELAY = 0.08

export default function MesTooltip({
  mouseEnterDelay = MES_TOOLTIP_ENTER_DELAY,
  mouseLeaveDelay = MES_TOOLTIP_LEAVE_DELAY,
  ...props
}: TooltipProps) {
  return (
    <Tooltip
      mouseEnterDelay={mouseEnterDelay}
      mouseLeaveDelay={mouseLeaveDelay}
      {...props}
    />
  )
}
