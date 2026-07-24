import { Form, Input, Modal } from 'antd'
import { useState } from 'react'

interface Props { open: boolean; onCancel: () => void; onApply: (text: string) => void }

export default function CustomerSpecPasteModal({ open, onCancel, onApply }: Props) {
  const [text, setText] = useState('')
  const apply = () => { onApply(text); setText('') }
  return (
    <Modal title="粘贴客户规格" open={open} okText="应用" okButtonProps={{ disabled: !text.trim() }} onCancel={onCancel} onOk={apply}>
      <Form layout="vertical">
        <Form.Item label="卷号、品名、克重、门幅、客户重量">
          <Input.TextArea autoFocus rows={10} value={text} onChange={(event) => setText(event.target.value)} placeholder={'A000001\t食品卡\t75\t500\t1185\nA000002\t食品卡\t75\t500\t1186'} />
        </Form.Item>
      </Form>
    </Modal>
  )
}
