import { InfoCircleOutlined } from '@ant-design/icons'
import { Button, Tooltip } from 'antd'
import { useState } from 'react'
import type { ReportMetricContextVO } from '../../../types/report'
import ReportMetricCatalogDrawer from './ReportMetricCatalogDrawer'

export default function ReportMetricCatalogButton({ context }: { context: ReportMetricContextVO }) {
  const [open, setOpen] = useState(false)
  return <>
    <Tooltip title="查看指标口径">
      <Button className="report-metric-context__action" type="text" size="small"
        aria-label="查看指标口径" icon={<InfoCircleOutlined />} onClick={() => setOpen(true)} />
    </Tooltip>
    <ReportMetricCatalogDrawer context={context} open={open} onClose={() => setOpen(false)} />
  </>
}
