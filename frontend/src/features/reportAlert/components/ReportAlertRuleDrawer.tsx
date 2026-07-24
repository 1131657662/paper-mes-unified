import { PlusOutlined } from '@ant-design/icons'
import { Alert, Button, Drawer, Skeleton } from 'antd'
import { useState } from 'react'
import type { Customer } from '../../../types/customer'
import type { Paper } from '../../../types/paper'
import { useDeleteReportAlertRule } from '../hooks/useDeleteReportAlertRule'
import { useReportAlertRules } from '../hooks/useReportAlertRules'
import type { ReportAlertRule } from '../types'
import ReportAlertRuleList from './ReportAlertRuleList'
import ReportAlertRuleModal from './ReportAlertRuleModal'

interface Props {
  customers: Customer[]
  onClose: () => void
  open: boolean
  papers: Paper[]
}

export default function ReportAlertRuleDrawer({ customers, onClose, open, papers }: Props) {
  const [editing, setEditing] = useState<ReportAlertRule | null | undefined>(undefined)
  const { data: rules = [], isLoading: isLoadingRules, isError: isRulesError,
    refetch: refetchRules } = useReportAlertRules(open)
  const { mutate: deleteRule, isPending: isDeletingRule } = useDeleteReportAlertRule()
  return <Drawer className="report-alert-rule-drawer" width="min(920px, calc(100vw - 24px))"
    open={open} onClose={onClose} title="异常阈值规则"
    extra={<Button type="primary" icon={<PlusOutlined />} onClick={() => setEditing(null)}>
      新建规则
    </Button>}>
    <Alert className="report-alert-rule-priority" type="info" showIcon
      message="规则按客户、纸张、工艺、全局的顺序匹配；同一指标命中首条后停止。" />
    {isRulesError && <Alert className="report-alert-rule-error" type="error" showIcon
      message="阈值规则加载失败" action={<Button size="small" onClick={() => void refetchRules()}>重试</Button>} />}
    {isLoadingRules ? <Skeleton active paragraph={{ rows: 6 }} /> :
      <ReportAlertRuleList customers={customers} deleting={isDeletingRule} items={rules}
        onDelete={(item) => deleteRule({ uuid: item.uuid, version: item.version })}
        onEdit={setEditing} papers={papers} />}
    {editing !== undefined && <ReportAlertRuleModal key={editing?.uuid ?? 'create'} open
      customers={customers} initial={editing} papers={papers}
      onClose={() => setEditing(undefined)} onSaved={() => setEditing(undefined)} />}
  </Drawer>
}
