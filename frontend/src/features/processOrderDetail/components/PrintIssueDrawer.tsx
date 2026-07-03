import { useState } from 'react'
import { createPortal } from 'react-dom'
import { Alert, Button, Descriptions, Drawer, Form, Input, Space, Tag, message } from 'antd'
import { PrinterOutlined, SendOutlined } from '@ant-design/icons'
import type { PrintResultVO, ProcessOrderDetailVO } from '../../../types/processOrder'
import { usePrintProcessOrder } from '../hooks/usePrintProcessOrder'
import PrintPreviewSheet from './PrintPreviewSheet'
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
  const { mutateAsync: printOrder, isPending: isPrinting } = usePrintProcessOrder(detail.order.uuid)
  const isReprint = (detail.order.printCount ?? 0) > 0

  const handleIssue = async () => {
    const values = isReprint ? await form.validateFields() : {}
    const printResult = await printOrder(isReprint ? { reason: values.reason } : undefined)
    setResult(printResult)
    message.success(isReprint ? '补打完成' : '打印下发完成')
    onPrinted()
  }

  return (
    <>
      <Drawer
        title={isReprint ? '补打加工单' : '打印预览并下发'}
        rootClassName="print-issue-drawer-root"
        className="print-issue-drawer"
        width="88vw"
        open={open}
        onClose={onClose}
        destroyOnHidden
        extra={<PrintActions isPrinting={isPrinting} isReprint={isReprint} result={result} onIssue={handleIssue} />}
      >
        <PrintDrawerContent detail={detail} form={form} isReprint={isReprint} result={result} />
      </Drawer>
      {open && createPortal(<PrintOnlySheet detail={detail} />, document.body)}
    </>
  )
}

function PrintActions({ isPrinting, isReprint, result, onIssue }: ActionProps) {
  return (
    <Space>
      <Button icon={<PrinterOutlined />} onClick={() => window.print()}>
        浏览器打印
      </Button>
      <Button type="primary" icon={<SendOutlined />} loading={isPrinting} disabled={!!result} onClick={onIssue}>
        {isReprint ? '确认补打' : '确认下发'}
      </Button>
    </Space>
  )
}

function PrintDrawerContent({ detail, form, isReprint, result }: ContentProps) {
  return (
    <div className="print-issue__content">
      <div className="print-issue__screen-only">
        <IssueNotice isReprint={isReprint} result={result} />
      </div>
      {isReprint && !result && <ReprintReasonForm form={form} />}
      <div className="print-issue__sheet-frame">
        <PrintPreviewSheet detail={detail} />
      </div>
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

function PrintOnlySheet({ detail }: { detail: ProcessOrderDetailVO }) {
  return (
    <div className="print-issue-print-root">
      <PrintPreviewSheet detail={detail} />
    </div>
  )
}

interface ActionProps {
  isPrinting: boolean
  isReprint: boolean
  result: PrintResultVO | null
  onIssue: () => void
}

interface ContentProps {
  detail: ProcessOrderDetailVO
  form: ReturnType<typeof Form.useForm>[0]
  isReprint: boolean
  result: PrintResultVO | null
}

function IssueNotice({ isReprint, result }: { isReprint: boolean; result: PrintResultVO | null }) {
  if (result) {
    return (
      <Alert
        type="success"
        showIcon
        message={isReprint ? '补打已完成' : '单据已下发到加工中'}
        description={<PrintResultSummary result={result} />}
      />
    )
  }

  return (
    <Alert
      type="info"
      showIcon
      message={isReprint ? '补打不会改变单据状态' : '首打将锁定下发快照并流转为加工中'}
      description="请先核对下方车间加工单。正式保存后，后端会生成不可变下发快照，用于回录对账。"
    />
  )
}

function PrintResultSummary({ result }: { result: PrintResultVO }) {
  return (
    <Descriptions size="small" column={3} className="print-issue__result">
      <Descriptions.Item label="打印次数">{result.printCount ?? '-'}</Descriptions.Item>
      <Descriptions.Item label="打印时间">{result.printTime ?? '-'}</Descriptions.Item>
      <Descriptions.Item label="正式号">
        <Tag color="blue">{result.finishRollNos?.length ?? 0} 个</Tag>
      </Descriptions.Item>
    </Descriptions>
  )
}
