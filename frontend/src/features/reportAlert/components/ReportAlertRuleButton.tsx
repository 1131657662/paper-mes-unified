import { SlidersOutlined } from '@ant-design/icons'
import { Button } from 'antd'
import { useState } from 'react'
import { PERMISSIONS } from '../../../constants/permissions'
import { useHasPermission } from '../../../stores/authStore'
import type { Customer } from '../../../types/customer'
import type { Paper } from '../../../types/paper'
import ReportAlertRuleDrawer from './ReportAlertRuleDrawer'

interface Props {
  customers: Customer[]
  papers: Paper[]
}

export default function ReportAlertRuleButton({ customers, papers }: Props) {
  const [open, setOpen] = useState(false)
  const canConfigure = useHasPermission(PERMISSIONS.systemConfig)
  if (!canConfigure) return null
  return <>
    <Button icon={<SlidersOutlined />} onClick={() => setOpen(true)}>阈值规则</Button>
    <ReportAlertRuleDrawer customers={customers} papers={papers} open={open}
      onClose={() => setOpen(false)} />
  </>
}
