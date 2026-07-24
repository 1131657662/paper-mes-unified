import { useState } from 'react'
import { createPortal } from 'react-dom'
import { Drawer, Form, Input, InputNumber, Spin, message } from 'antd'
import QueryLoadErrorAlert from '../../../components/feedback/QueryLoadErrorAlert'
import type { PrintResultVO, ProcessOrderDetailVO, ProcessOrderPrintViewVO, PrintViewVersion } from '../../../types/processOrder'
import { useIssueProcessOrder, usePhysicalReprintProcessOrder, usePrintProcessOrder } from '../hooks/usePrintProcessOrder'
import { useProcessOrderPrintView } from '../hooks/useProcessOrderPrintView'
import { resolvePrintIssueMode, type PrintIssueMode } from '../printIssueMode'
import { printVersionProps } from '../printVersionModel'
import PrintOnlySheet from './PrintOnlySheet'
import PrintPreviewSheet from './PrintPreviewSheet'
import { drawerTitle, IssueNotice, PrintActions, type PendingPrintConfirmation } from './PrintIssueActions'
import PrintVersionControl from './PrintVersionControl'
import './PrintIssueDrawer.css'

interface Props {
  detail: ProcessOrderDetailVO
  open: boolean
  onClose: () => void
  onPrinted: () => void | Promise<void>
}

export default function PrintIssueDrawer({ detail, open, onClose, onPrinted }: Props) {
  const [form] = Form.useForm()
  const [result, setResult] = useState<PrintResultVO | null>(null)
  const [pendingConfirmation, setPendingConfirmation] = useState<PendingPrintConfirmation | null>(null)
  const [copies, setCopies] = useState(1)
  const [version, setVersion] = useState<PrintViewVersion>('ISSUED')
  const { mutateAsync: printOrder, isPending: isPrinting } = usePrintProcessOrder(detail.order.uuid)
  const { mutateAsync: issueOrder, isPending: isIssuing } = useIssueProcessOrder(detail.order.uuid)
  const { mutateAsync: physicalReprint, isPending: isPhysicalReprinting } = usePhysicalReprintProcessOrder(detail.order.uuid)
  const {
    data: printView,
    isError: isPrintViewError,
    isLoading: isLoadingPrintView,
    refetch: refetchPrintView,
  } = useProcessOrderPrintView(detail.order.uuid, version, open)
  const mode = resolvePrintIssueMode(detail.order.orderStatus, detail.order.printCount, version, detail.order.printStatus)
  const printDetail = printView?.detail ?? detail
  const disabledReason = isLoadingPrintView
    ? '正在加载打印版本，请稍候'
    : isPrintViewError ? '打印版本加载失败，请重新加载' : undefined

  const openBrowserPrint = () => window.setTimeout(() => window.print(), 0)

  const handleIssue = async () => {
    const issueResult = await issueOrder()
    setResult(issueResult)
    setPendingConfirmation({})
    message.success('加工单已下发，请完成物理打印')
    await onPrinted()
    await refetchPrintView()
    openBrowserPrint()
  }

  const handleOpenPrint = async () => {
    if (mode === 'preview' || mode === 'unprinted') {
      if (mode === 'unprinted') setPendingConfirmation({})
      openBrowserPrint()
      return
    }
    if (mode !== 'reprint' && mode !== 'audited-reprint') return
    const values = await form.validateFields()
    setPendingConfirmation({ reason: values.reason?.trim() })
    openBrowserPrint()
  }

  const handleConfirmPrint = async () => {
    if (!pendingConfirmation) return
    const dto = pendingConfirmation.reason ? { reason: pendingConfirmation.reason } : undefined
    const printResult = mode === 'audited-reprint'
      ? await physicalReprint({ reason: pendingConfirmation.reason ?? '', version })
      : await printOrder(dto)
    setResult(printResult)
    setPendingConfirmation(null)
    message.success(printResult.reprint ? '补打已确认' : '打印已确认')
    await onPrinted()
  }

  return (
    <>
      <Drawer
        title={drawerTitle(mode, version)}
        rootClassName="print-issue-drawer-root"
        className="print-issue-drawer"
        width="min(1180px, calc(100vw - 32px))"
        open={open}
        onClose={onClose}
        destroyOnHidden
        extra={<PrintActions disabledReason={disabledReason} isBusy={isPrinting || isIssuing || isPhysicalReprinting} mode={mode}
          pendingConfirmation={pendingConfirmation} result={result} version={version}
          onConfirmPrint={() => void handleConfirmPrint()} onOpenPrint={() => void handleOpenPrint()}
          onIssue={() => void handleIssue()} />}
      >
        {isPrintViewError ? (
          <QueryLoadErrorAlert
            message="打印版本加载失败"
            description="系统不会使用未确认的当前数据代替历史快照，请重新加载后再打印。"
            onRetry={() => void refetchPrintView()}
          />
        ) : (
          <PrintDrawerContent copies={copies} detail={printDetail} form={form} loading={isLoadingPrintView}
            mode={mode} pendingConfirmation={pendingConfirmation} result={result} version={version}
            view={printView} onCopiesChange={setCopies}
            onVersionChange={setVersion} />
        )}
      </Drawer>
      {open && !isLoadingPrintView && !isPrintViewError
        && createPortal(<PrintOnlySheet copies={copies} detail={printDetail} version={version} view={printView} />, document.body)}
    </>
  )
}

export function PrintDrawerContent({ copies, detail, form, loading, mode, pendingConfirmation, result, version, view, onCopiesChange, onVersionChange }: ContentProps) {
  return (
    <div className="print-issue__content">
      <PrintVersionControl value={version} view={view} onChange={onVersionChange} />
      <div className="print-issue__screen-only">
        <IssueNotice mode={mode} pendingConfirmation={pendingConfirmation} result={result} />
      </div>
      {!result && <PrintCopies value={copies} onChange={onCopiesChange} />}
      {(mode === 'reprint' || mode === 'audited-reprint') && <ReprintReasonForm form={form} />}
      <Spin spinning={loading}>
        {loading ? (
          <div className="print-issue__loading" aria-label="打印版本加载中" />
        ) : (
          <div className="print-issue__sheet-frame">
            <PrintPreviewSheet detail={detail} {...printVersionProps(version, view)} />
          </div>
        )}
      </Spin>
    </div>
  )
}

function ReprintReasonForm({ form }: { form: ReturnType<typeof Form.useForm>[0] }) {
  return (
    <Form form={form} layout="vertical" className="print-issue__reason print-issue__screen-only">
      <Form.Item name="reason" label="补打原因" rules={[{ required: true, message: '补打必须填写原因' }, { max: 255, message: '补打原因不能超过255个字符' }]}>
        <Input.TextArea maxLength={255} rows={2} placeholder="例如：车间单据污损，需要重新打印" showCount />
      </Form.Item>
    </Form>
  )
}

function PrintCopies({ value, onChange }: { value: number; onChange: (value: number) => void }) {
  return (
    <div className="print-issue__copies print-issue__screen-only">
      <span>打印份数</span>
      <InputNumber aria-label="打印份数" min={1} max={10} precision={0} value={value} onChange={(next) => onChange(next ?? 1)} />
      <span className="print-issue__copies-hint">仅影响本次纸张输出，不改变下发次数</span>
    </div>
  )
}

interface ContentProps {
  copies: number
  detail: ProcessOrderDetailVO
  form: ReturnType<typeof Form.useForm>[0]
  loading: boolean
  mode: PrintIssueMode
  pendingConfirmation: PendingPrintConfirmation | null
  onCopiesChange: (value: number) => void
  result: PrintResultVO | null
  version: PrintViewVersion
  view?: ProcessOrderPrintViewVO
  onVersionChange: (version: PrintViewVersion) => void
}
