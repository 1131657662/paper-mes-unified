import { PlusOutlined } from '@ant-design/icons'
import { Alert, Button, Skeleton } from 'antd'
import { useState } from 'react'
import { useReportPapers } from '../../features/report/hooks/useReportReferenceData'
import ReportAlertRuleList from '../../features/reportAlert/components/ReportAlertRuleList'
import ReportAlertRuleModal from '../../features/reportAlert/components/ReportAlertRuleModal'
import { useDeleteReportAlertRule } from '../../features/reportAlert/hooks/useDeleteReportAlertRule'
import { useReportAlertRules } from '../../features/reportAlert/hooks/useReportAlertRules'
import type { ReportAlertRule } from '../../features/reportAlert/types'
import { useCustomers } from '../../features/processOrderCreate/hooks/useReferenceData'

export default function ReportAlertRuleManagement() {
  const [editing, setEditing] = useState<ReportAlertRule | null | undefined>(undefined)
  const rules = useReportAlertRules(true)
  const customers = useCustomers()
  const papers = useReportPapers()
  const deleteMutation = useDeleteReportAlertRule()
  const retry = () => { void rules.refetch(); void customers.refetch(); void papers.refetch() }
  const loading = rules.isLoading || customers.isLoading || papers.isLoading
  return <div className="report-management__content">
    <div className="report-management__toolbar">
      <div><strong>分层阈值规则</strong><span>按客户、纸张、工艺、全局依次匹配，命中首条后停止。</span></div>
      <Button type="primary" icon={<PlusOutlined />} onClick={() => setEditing(null)}>新建规则</Button>
    </div>
    {(rules.isError || customers.isError || papers.isError) && <Alert showIcon type="error"
      message="阈值规则资料加载失败" action={<Button size="small" onClick={retry}>重试</Button>} />}
    {loading ? <Skeleton active paragraph={{ rows: 6 }} /> : <ReportAlertRuleList
      customers={customers.data?.records ?? []} deleting={deleteMutation.isPending} items={rules.data ?? []}
      onDelete={(item) => deleteMutation.mutate({ uuid: item.uuid, version: item.version })}
      onEdit={setEditing} papers={papers.data?.records ?? []} />}
    {editing !== undefined && <ReportAlertRuleModal key={editing?.uuid ?? 'create'} open
      customers={customers.data?.records ?? []} initial={editing} papers={papers.data?.records ?? []}
      onClose={() => setEditing(undefined)} onSaved={() => setEditing(undefined)} />}
  </div>
}
