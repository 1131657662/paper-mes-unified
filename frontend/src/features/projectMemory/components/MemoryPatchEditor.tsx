import { useState } from 'react'
import { Button, Form, Input, message } from 'antd'
import { SaveOutlined } from '@ant-design/icons'
import { usePatchProjectMemory } from '../hooks/usePatchProjectMemory'
import { parseProjectMemoryOperations } from '../projectMemoryPatchParser'

interface Props {
  expectedVersion: string
}

interface PatchFormValues {
  operations: string
  reason: string
}

export default function MemoryPatchEditor({ expectedVersion }: Props) {
  const [form] = Form.useForm<PatchFormValues>()
  const [idempotencyKey, setIdempotencyKey] = useState(() => crypto.randomUUID())
  const { mutate: patchMemory, isPending: isPatching } = usePatchProjectMemory()

  function submit(values: PatchFormValues) {
    let operations
    try {
      operations = parseProjectMemoryOperations(values.operations)
    } catch (error) {
      message.error(error instanceof Error ? error.message : '补丁内容无效')
      return
    }
    patchMemory({ expectedMemoryVersion: expectedVersion, idempotencyKey, operations,
      reason: values.reason.trim() }, { onSuccess: resetEditor })
  }

  function resetEditor() {
    form.resetFields()
    setIdempotencyKey(crypto.randomUUID())
  }

  return (
    <section className="project-memory-editor">
      <div className="project-memory-section-head"><strong>RFC 6902 补丁</strong><span>{expectedVersion}</span></div>
      <Form<PatchFormValues> form={form} layout="vertical" initialValues={{ operations: '[]', reason: '' }} onFinish={submit}>
        <Form.Item name="operations" label="Operations JSON" rules={[{ required: true, message: '请输入补丁操作' }]}>
          <Input.TextArea className="project-memory-editor__json" autoSize={{ minRows: 12, maxRows: 22 }} spellCheck={false} />
        </Form.Item>
        <Form.Item name="reason" label="变更原因" rules={[{ required: true, whitespace: true, message: '请输入变更原因' }]}>
          <Input.TextArea maxLength={500} rows={3} showCount />
        </Form.Item>
        <Button type="primary" htmlType="submit" icon={<SaveOutlined />} loading={isPatching}>提交补丁</Button>
      </Form>
    </section>
  )
}
