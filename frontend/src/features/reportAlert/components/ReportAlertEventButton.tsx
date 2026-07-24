import { AlertOutlined } from '@ant-design/icons'
import { Badge, Button, Tooltip } from 'antd'
import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useReportAlertEvents } from '../hooks/useReportAlertEvents'
import ReportAlertEventDrawer from './ReportAlertEventDrawer'

export default function ReportAlertEventButton() {
  const [open, setOpen] = useState(false)
  const [searchParams, setSearchParams] = useSearchParams()
  const focusUuid = searchParams.get('alertEvent') ?? undefined
  const { data: activePage } = useReportAlertEvents({ page: 1, size: 1, status: 1 }, true)
  const close = () => {
    setOpen(false)
    if (!focusUuid) return
    const next = new URLSearchParams(searchParams)
    next.delete('alertEvent')
    setSearchParams(next, { replace: true })
  }
  return <>
    <Tooltip title="查看预警事件">
      <Badge count={activePage?.activeCount ?? 0} overflowCount={99} size="small">
        <Button icon={<AlertOutlined />} onClick={() => setOpen(true)}>预警事件</Button>
      </Badge>
    </Tooltip>
    <ReportAlertEventDrawer open={open || Boolean(focusUuid)} focusedUuid={focusUuid}
      onClose={close} />
  </>
}
