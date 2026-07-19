import { DatePicker, Form, Input, Modal, Select, message } from 'antd'
import type { Dayjs } from 'dayjs'
import type { SettleOrder } from '../../types/settle'
import { useRecordCollectionReminder } from '../../features/settle/hooks/useRecordCollectionReminder'

interface FormValues {
  contactName?: string
  nextFollowUpDate?: Dayjs
  reminderChannel: number
  reminderResult: number
  remark: string
}

interface Props {
  record: SettleOrder | null
  onClose: () => void
}

const channelOptions = [
  { label: '电话', value: 1 },
  { label: '微信', value: 2 },
  { label: '短信', value: 3 },
  { label: '上门', value: 4 },
  { label: '其他', value: 5 },
]

const resultOptions = [
  { label: '已联系', value: 1 },
  { label: '未接通', value: 2 },
  { label: '承诺付款', value: 3 },
  { label: '金额或业务有异议', value: 4 },
  { label: '其他', value: 5 },
]

export default function CollectionReminderModal({ record, onClose }: Props) {
  const [form] = Form.useForm<FormValues>()
  const { mutateAsync: recordReminder, isPending: isRecording } = useRecordCollectionReminder()

  const handleSubmit = async () => {
    if (!record) return
    const values = await form.validateFields()
    await recordReminder({
      uuid: record.uuid,
      data: {
        requestId: crypto.randomUUID(),
        reminderChannel: values.reminderChannel,
        reminderResult: values.reminderResult,
        contactName: values.contactName?.trim() || undefined,
        nextFollowUpDate: values.nextFollowUpDate?.format('YYYY-MM-DD'),
        remark: values.remark.trim(),
      },
    })
    message.success('催收提醒已记录')
    form.resetFields()
    onClose()
  }

  const handleClose = () => {
    form.resetFields()
    onClose()
  }

  return (
    <Modal open={Boolean(record)} title={`记录催收 · ${record?.settleNo ?? ''}`} okText="保存提醒"
      cancelText="取消" confirmLoading={isRecording} destroyOnHidden onCancel={handleClose}
      onOk={() => void handleSubmit()}>
      <Form form={form} layout="vertical" initialValues={{ reminderChannel: 1, reminderResult: 1 }}>
        <Form.Item name="reminderChannel" label="联系渠道" rules={[{ required: true }]}>
          <Select options={channelOptions} />
        </Form.Item>
        <Form.Item name="reminderResult" label="联系结果" rules={[{ required: true }]}>
          <Select options={resultOptions} />
        </Form.Item>
        <Form.Item name="contactName" label="联系人">
          <Input maxLength={100} placeholder="客户联系人，可不填" />
        </Form.Item>
        <Form.Item name="nextFollowUpDate" label="下次跟进日期">
          <DatePicker style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item name="remark" label="提醒记录" rules={[{ required: true, whitespace: true, max: 500 }]}>
          <Input.TextArea rows={3} maxLength={500} showCount placeholder="记录客户反馈、承诺日期或争议点" />
        </Form.Item>
      </Form>
    </Modal>
  )
}
