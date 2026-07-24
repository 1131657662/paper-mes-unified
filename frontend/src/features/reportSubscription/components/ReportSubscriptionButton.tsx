import { ClockCircleOutlined } from '@ant-design/icons'
import { Button } from 'antd'
import { useState } from 'react'
import type { ReportQuery, ReportSourcePath } from '../../../types/report'
import ReportSubscriptionDrawer from './ReportSubscriptionDrawer'

interface Props { query: ReportQuery; reportPath: ReportSourcePath }

export default function ReportSubscriptionButton({ query, reportPath }: Props) {
  const [open, setOpen] = useState(false)
  return <>
    <Button icon={<ClockCircleOutlined />} onClick={() => setOpen(true)}>订阅</Button>
    <ReportSubscriptionDrawer currentQuery={query} reportPath={reportPath}
      open={open} onClose={() => setOpen(false)} />
  </>
}
