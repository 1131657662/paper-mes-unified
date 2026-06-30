import { useEffect, useState } from 'react'
import { Alert, Descriptions, Empty, Form, Input, Modal, Space, Tag, message } from 'antd'
import { printProcessOrder } from '../../api/processOrder'
import type { PrintResultVO } from '../../types/processOrder'

interface Props {
  uuid: string | null
  orderNo?: string
  printCount?: number
  open: boolean
  onClose: () => void
  onSuccess: () => void
}

export default function PrintModal({
  uuid,
  orderNo,
  printCount,
  open,
  onClose,
  onSuccess,
}: Props) {
  const [form] = Form.useForm()
  const [submitting, setSubmitting] = useState(false)
  const [result, setResult] = useState<PrintResultVO | null>(null)

  const isReprint = (printCount ?? 0) >= 1

  useEffect(() => {
    if (open) {
      form.resetFields()
      setResult(null)
    }
  }, [open, form])

  const handleSubmit = async () => {
    if (!uuid) return
    const values = isReprint ? await form.validateFields() : {}
    setSubmitting(true)
    try {
      const res = await printProcessOrder(uuid, isReprint ? { reason: values.reason } : undefined)
      setResult(res)
      message.success(res.reprint ? '补打成功' : '打印成功，已下发')
      onSuccess()
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Modal
      title={`打印加工单${orderNo ? `（${orderNo}）` : ''}`}
      open={open}
      onCancel={onClose}
      onOk={result ? onClose : handleSubmit}
      okText={result ? '关闭' : '确认打印'}
      cancelButtonProps={{ style: result ? { display: 'none' } : undefined }}
      confirmLoading={submitting}
      destroyOnHidden
    >
      {result ? (
        <>
          <Descriptions column={1} size="small" style={{ marginBottom: 12 }}>
            <Descriptions.Item label="打印次数">{result.printCount ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="类型">
              {result.reprint ? '补打' : '首打（已流转为加工中）'}
            </Descriptions.Item>
            <Descriptions.Item label="打印时间">{result.printTime ?? '-'}</Descriptions.Item>
          </Descriptions>
          <div style={{ marginBottom: 8 }}>
            <strong>正式卷号</strong>
          </div>
          {result.finishRollNos && result.finishRollNos.length > 0 ? (
            <Space wrap style={{ marginBottom: 12 }}>
              {result.finishRollNos.map((n) => (
                <Tag key={n} color="blue">
                  {n}
                </Tag>
              ))}
            </Space>
          ) : (
            <Empty
              image={Empty.PRESENTED_IMAGE_SIMPLE}
              description="本次无预生成正式卷号"
              style={{ margin: '8px 0' }}
            />
          )}
          <div style={{ marginBottom: 8 }}>
            <strong>备用卷号</strong>
          </div>
          {result.spareRollNos && result.spareRollNos.length > 0 ? (
            <Space wrap>
              {result.spareRollNos.map((n) => (
                <Tag key={n}>{n}</Tag>
              ))}
            </Space>
          ) : (
            <Empty
              image={Empty.PRESENTED_IMAGE_SIMPLE}
              description="无备用卷号"
              style={{ margin: '8px 0' }}
            />
          )}
        </>
      ) : (
        <>
          <Alert
            type="info"
            showIcon
            style={{ marginBottom: 16 }}
            message={
              isReprint
                ? '补打：需填写补打原因，不影响单据状态。'
                : '首打：将锁定原纸标称参数、生成下发快照与卷号，并将单据流转为「加工中」。'
            }
          />
          {isReprint && (
            <Form form={form} layout="vertical">
              <Form.Item
                name="reason"
                label="补打原因"
                rules={[{ required: true, message: '补打需填写原因' }]}
              >
                <Input.TextArea rows={3} placeholder="请填写补打原因" />
              </Form.Item>
            </Form>
          )}
        </>
      )}
    </Modal>
  )
}
