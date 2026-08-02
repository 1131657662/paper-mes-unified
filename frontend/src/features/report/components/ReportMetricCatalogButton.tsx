import { InfoCircleOutlined } from '@ant-design/icons'
import { Button, Tooltip } from 'antd'
import { useState } from 'react'
import { DISPLAY_TERMS } from '../../../constants/displayTerms'
import type { ReportMetricContextVO } from '../../../types/report'
import ReportMetricCatalogDrawer from './ReportMetricCatalogDrawer'

export default function ReportMetricCatalogButton({ context }: { context: ReportMetricContextVO }) {
  const [open, setOpen] = useState(false)
  return <>
    <Tooltip title={`查看${DISPLAY_TERMS.metricDefinition}`}>
      <Button className="report-metric-context__action" type="text" size="small"
        aria-label={`查看${DISPLAY_TERMS.metricDefinition}`} icon={<InfoCircleOutlined />} onClick={() => setOpen(true)} />
    </Tooltip>
    <ReportMetricCatalogDrawer context={context} open={open} onClose={() => setOpen(false)} />
  </>
}
