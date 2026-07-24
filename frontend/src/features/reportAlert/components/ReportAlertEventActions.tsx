import { CheckOutlined, EyeInvisibleOutlined } from '@ant-design/icons'
import { Button, Form, Input, Modal, Space, Tooltip } from 'antd'
import { useState } from 'react'

interface Props {
  acknowledged: boolean
  busy: boolean
  onAcknowledge: () => void
  onIgnore: (reason: string) => void
}

export default function ReportAlertEventActions({ acknowledged, busy, onAcknowledge, onIgnore }: Props) {
  const [open, setOpen] = useState(false)
  const [reason, setReason] = useState('')
  const submitIgnore = () => {
    const value = reason.trim()
    if (!value) return
    onIgnore(value)
    setReason('')
    setOpen(false)
  }
  return <>
    <Space size={4}>
      <Tooltip title={acknowledged ? '已确认' : '标记为已知晓'}>
        <Button size="small" type="text" icon={<CheckOutlined />} disabled={acknowledged || busy}
          onClick={onAcknowledge} aria-label="确认预警" />
      </Tooltip>
      <Tooltip title="忽略此预警">
        <Button size="small" type="text" icon={<EyeInvisibleOutlined />} disabled={busy}
          onClick={() => setOpen(true)} aria-label="忽略预警" />
      </Tooltip>
    </Space>
    <Modal title="忽略预警" open={open} okText="确认忽略" cancelText="取消"
      okButtonProps={{ disabled: reason.trim().length === 0 || reason.length > 500, loading: busy }}
      onOk={submitIgnore} onCancel={() => setOpen(false)}>
      <Form layout="vertical">
        <Form.Item label="忽略原因" required>
          <Input.TextArea value={reason} maxLength={500} showCount rows={4}
            placeholder="请填写本次忽略的业务原因" onChange={(event) => setReason(event.target.value)} />
        </Form.Item>
      </Form>
    </Modal>
  </>
}
