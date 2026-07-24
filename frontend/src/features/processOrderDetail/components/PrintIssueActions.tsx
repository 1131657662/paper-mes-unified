import { Alert, Button, Space } from 'antd'
import { PrinterOutlined, SendOutlined } from '@ant-design/icons'
import type { ReactNode } from 'react'
import MesTooltip from '../../../components/biz/MesTooltip'
import type { PrintResultVO, PrintViewVersion } from '../../../types/processOrder'
import type { PrintIssueMode } from '../printIssueMode'
import PrintResultSummary from './PrintResultSummary'

export interface PendingPrintConfirmation {
  reason?: string
}

interface ActionProps {
  disabledReason?: string
  isBusy: boolean
  mode: PrintIssueMode
  pendingConfirmation: PendingPrintConfirmation | null
  result: PrintResultVO | null
  version: PrintViewVersion
  onConfirmPrint: () => void
  onOpenPrint: () => void
  onIssue: () => void
}

export function PrintActions({ disabledReason, isBusy, mode, pendingConfirmation, result: _result, version, onConfirmPrint, onOpenPrint, onIssue }: ActionProps) {
  const disabled = Boolean(disabledReason)
  if (mode === 'preview') {
    return <GuardedPrintAction reason={disabledReason}><Button type="primary" icon={<PrinterOutlined />} disabled={disabled} onClick={onOpenPrint}>打印{version === 'FINISHED' ? '完工版本' : '下发版本'}</Button></GuardedPrintAction>
  }
  if (pendingConfirmation) {
    return <GuardedPrintAction reason={disabledReason}><Space><Button icon={<PrinterOutlined />} disabled={disabled} onClick={onOpenPrint}>再次打开打印</Button><Button type="primary" icon={<PrinterOutlined />} disabled={disabled} loading={isBusy} onClick={onConfirmPrint}>确认已完成打印</Button></Space></GuardedPrintAction>
  }
  if (mode === 'unprinted') {
    return <GuardedPrintAction reason={disabledReason}><Button type="primary" icon={<PrinterOutlined />} disabled={disabled} onClick={onOpenPrint}>打开打印</Button></GuardedPrintAction>
  }
  if (mode === 'reprint' || mode === 'audited-reprint') {
    return <GuardedPrintAction reason={disabledReason}><Button type="primary" icon={<PrinterOutlined />} disabled={disabled} onClick={onOpenPrint}>打开打印并准备补打</Button></GuardedPrintAction>
  }
  return <GuardedPrintAction reason={disabledReason}><Button type="primary" icon={<SendOutlined />} disabled={disabled} loading={isBusy} onClick={onIssue}>确认下发</Button></GuardedPrintAction>
}

function GuardedPrintAction({ children, reason }: { children: ReactNode; reason?: string }) {
  return <MesTooltip title={reason}><span className="print-issue-action-tooltip" title={reason}>{children}</span></MesTooltip>
}

export function IssueNotice({ mode, pendingConfirmation, result }: { mode: PrintIssueMode; pendingConfirmation: PendingPrintConfirmation | null; result: PrintResultVO | null }) {
  if (pendingConfirmation) {
    return <Alert type="warning" showIcon message="请完成物理打印后确认" description="浏览器打印窗口取消不会撤销下发；确认纸张已输出后再点击“确认已完成打印”。" />
  }
  if (result) {
    const reprint = mode === 'reprint' || mode === 'audited-reprint'
    return <Alert type={result.printStatus === 1 ? 'success' : 'info'} showIcon message={result.printStatus === 1 ? (reprint ? '补打已完成' : '打印已确认') : '单据已下发，尚未确认打印'} description={<PrintResultSummary result={result} />} />
  }
  if (mode === 'preview') {
    return <Alert type="info" showIcon message="当前单据为只读打印预览" description="本次打印不会重新下发，也不会增加补打次数或改变单据状态。" />
  }
  if (mode === 'unprinted') {
    return <Alert type="warning" showIcon message="单据已下发但尚未确认打印" description="打开浏览器打印窗口完成纸张输出后，点击确认按钮记录打印结果。" />
  }
  const reprint = mode === 'reprint' || mode === 'audited-reprint'
  return <Alert type="info" showIcon message={reprint ? '补打不会改变单据状态' : '下发与打印分两步确认'} description={reprint ? '填写补打原因后打开打印，确认纸张已输出才会记录补打次数。' : '确认下发后系统会锁定下发快照，再打开浏览器打印窗口。'} />
}

export function drawerTitle(mode: PrintIssueMode, version: PrintViewVersion): string {
  if (mode === 'issue') return '下发加工单'
  if (mode === 'unprinted') return '确认加工单打印'
  if (mode === 'reprint') return '补打加工单'
  if (mode === 'audited-reprint') return version === 'FINISHED' ? '补打完工版本' : '补打下发版本'
  return version === 'FINISHED' ? '完工版本打印预览' : '下发版本打印预览'
}
