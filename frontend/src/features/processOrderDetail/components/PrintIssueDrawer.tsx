import { useState } from 'react'
import { createPortal } from 'react-dom'
import { Alert, Button, Drawer, Form, Input, InputNumber, Spin, message } from 'antd'
import { PrinterOutlined, SendOutlined } from '@ant-design/icons'
import type { PrintResultVO, ProcessOrderDetailVO, ProcessOrderPrintViewVO, PrintViewVersion } from '../../../types/processOrder'
import { usePrintProcessOrder } from '../hooks/usePrintProcessOrder'
import { useProcessOrderPrintView } from '../hooks/useProcessOrderPrintView'
import { resolvePrintIssueMode, type PrintIssueMode } from '../printIssueMode'
import { printVersionProps } from '../printVersionModel'
import PrintOnlySheet from './PrintOnlySheet'
import PrintPreviewSheet from './PrintPreviewSheet'
import PrintResultSummary from './PrintResultSummary'
import PrintVersionControl from './PrintVersionControl'
import './PrintIssueDrawer.css'

interface Props {
  detail: ProcessOrderDetailVO
  open: boolean
  onClose: () => void
  onPrinted: () => void
}

export default function PrintIssueDrawer({ detail, open, onClose, onPrinted }: Props) {
  const [form] = Form.useForm()
  const [result, setResult] = useState<PrintResultVO | null>(null)
  const [copies, setCopies] = useState(1)
  const [version, setVersion] = useState<PrintViewVersion>('ISSUED')
  const { mutateAsync: printOrder, isPending: isPrinting } = usePrintProcessOrder(detail.order.uuid)
  const { data: printView, isLoading: isLoadingPrintView, isError: isPrintViewError } = useProcessOrderPrintView(
    detail.order.uuid,
    version,
    open,
  )
  const mode = resolvePrintIssueMode(detail.order.orderStatus, detail.order.printCount, version)
  const printDetail = printView?.detail ?? detail

  const handleIssue = async () => {
    if (mode === 'preview') return
    const values = mode === 'reprint' ? await form.validateFields() : {}
    const printResult = await printOrder(mode === 'reprint' ? { reason: values.reason } : undefined)
    setResult(printResult)
    message.success(mode === 'reprint' ? '补打完成' : '打印下发完成')
    onPrinted()
    window.setTimeout(() => window.print(), 0)
  }

  return (
    <>
      <Drawer
        title={drawerTitle(mode, version)}
        rootClassName="print-issue-drawer-root"
        className="print-issue-drawer"
        width="88vw"
        open={open}
        onClose={onClose}
        destroyOnHidden
        extra={<PrintActions disabled={isLoadingPrintView || isPrintViewError} isPrinting={isPrinting} mode={mode} result={result} version={version} onIssue={handleIssue} />}
      >
        <PrintDrawerContent copies={copies} detail={printDetail} form={form} loading={isLoadingPrintView} mode={mode} result={result} version={version} view={printView} onCopiesChange={setCopies} onVersionChange={setVersion} />
        {isPrintViewError && <Alert type="error" showIcon message="打印版本加载失败" description="请关闭预览后重试；系统不会使用未确认的当前数据代替历史快照。" />}
      </Drawer>
      {open && createPortal(<PrintOnlySheet copies={copies} detail={printDetail} version={version} view={printView} />, document.body)}
    </>
  )
}

function PrintActions({ disabled, isPrinting, mode, result, version, onIssue }: ActionProps) {
  if (mode === 'preview') {
    return <Button type="primary" icon={<PrinterOutlined />} disabled={disabled} onClick={() => window.print()}>打印{version === 'FINISHED' ? '完工版本' : '下发版本'}</Button>
  }
  if (result) {
    return <Button icon={<PrinterOutlined />} onClick={() => window.print()}>再次打开打印</Button>
  }

  return (
    <Button type="primary" icon={mode === 'reprint' ? <PrinterOutlined /> : <SendOutlined />} loading={isPrinting} onClick={onIssue}>
      {mode === 'reprint' ? '确认补打并打印' : '确认下发并打印'}
    </Button>
  )
}

function PrintDrawerContent({ copies, detail, form, loading, mode, result, version, view, onCopiesChange, onVersionChange }: ContentProps) {
  return (
    <div className="print-issue__content">
      <PrintVersionControl value={version} view={view} onChange={onVersionChange} />
      <div className="print-issue__screen-only">
        <IssueNotice mode={mode} result={result} />
      </div>
      {!result && <PrintCopies value={copies} onChange={onCopiesChange} />}
      {mode === 'reprint' && !result && <ReprintReasonForm form={form} />}
      <Spin spinning={loading}>
        <div className="print-issue__sheet-frame">
          <PrintPreviewSheet detail={detail} {...printVersionProps(version, view)} />
        </div>
      </Spin>
    </div>
  )
}

function ReprintReasonForm({ form }: { form: ReturnType<typeof Form.useForm>[0] }) {
  return (
    <Form form={form} layout="vertical" className="print-issue__reason print-issue__screen-only">
      <Form.Item name="reason" label="补打原因" rules={[{ required: true, message: '补打必须填写原因' }]}>
        <Input.TextArea rows={2} placeholder="例如：车间单据污损，需要重新打印" />
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

interface ActionProps {
  disabled: boolean
  isPrinting: boolean
  mode: PrintIssueMode
  result: PrintResultVO | null
  version: PrintViewVersion
  onIssue: () => void
}

interface ContentProps {
  copies: number
  detail: ProcessOrderDetailVO
  form: ReturnType<typeof Form.useForm>[0]
  loading: boolean
  mode: PrintIssueMode
  onCopiesChange: (value: number) => void
  result: PrintResultVO | null
  version: PrintViewVersion
  view?: ProcessOrderPrintViewVO
  onVersionChange: (version: PrintViewVersion) => void
}

function IssueNotice({ mode, result }: { mode: PrintIssueMode; result: PrintResultVO | null }) {
  if (result) {
    return (
      <Alert
        type="success"
        showIcon
        message={mode === 'reprint' ? '补打已完成' : '单据已下发到加工中'}
        description={<PrintResultSummary result={result} />}
      />
    )
  }

  if (mode === 'preview') {
    return (
      <Alert
        type="info"
        showIcon
        message="当前单据为只读打印预览"
        description="本次打印不会重新下发，也不会增加补打次数或改变单据状态。"
      />
    )
  }

  return (
    <Alert
      type="info"
      showIcon
      message={mode === 'reprint' ? '补打不会改变单据状态' : '首打将锁定下发快照并流转为加工中'}
      description="请先核对下方车间加工单。正式保存后，后端会生成不可变下发快照，用于回录对账。"
    />
  )
}

function drawerTitle(mode: PrintIssueMode, version: PrintViewVersion): string {
  if (mode === 'issue') return '首次打印并下发'
  if (mode === 'reprint') return '补打加工单'
  return version === 'FINISHED' ? '完工版本打印预览' : '下发版本打印预览'
}
