import { useEffect, useState } from 'react'
import { Button, DatePicker, Drawer, Form, Radio, Input, Select, Space, message } from 'antd'
import dayjs from 'dayjs'
import { pageCustomers } from '../../api/customer'
import { createSettleByMonth } from '../../api/settle'
import type { SettleByMonthDTO } from '../../types/settle'

const { RangePicker } = DatePicker

interface Props {
  open: boolean
  onClose: () => void
  onSuccess: () => void
}

export default function SettleByMonthDrawer({ open, onClose, onSuccess }: Props) {
  const [form] = Form.useForm()
  const [submitting, setSubmitting] = useState(false)
  const [customerOptions, setCustomerOptions] = useState<{ value: string; label: string }[]>([])

  useEffect(() => {
    if (open) {
      pageCustomers({ current: 1, size: 200 })
        .then((res) => {
          setCustomerOptions(
            (res.records ?? []).map((c) => ({ value: c.uuid, label: c.customerName })),
          )
        })
        .catch(() => {
          message.error('客户列表加载失败')
        })
    } else {
      form.resetFields()
    }
  }, [open, form])

  const handleSubmit = async () => {
    const values = await form.validateFields()

    const dto: SettleByMonthDTO = {
      customerUuid: values.customerUuid,
      periodStart: values.periodRange[0].format('YYYY-MM-DD'),
      periodEnd: values.periodRange[1].format('YYYY-MM-DD'),
      settleDate: values.settleDate?.format('YYYY-MM-DD'),
      isInvoice: values.isInvoice,
      remark: values.remark,
    }

    setSubmitting(true)
    try {
      await createSettleByMonth(dto)
      message.success('结算单创建成功')
      onSuccess()
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Drawer
      title="按月批量生成结算单"
      width={500}
      open={open}
      onClose={onClose}
      destroyOnClose
      footer={
        <Space style={{ float: 'right' }}>
          <Button onClick={onClose}>取消</Button>
          <Button type="primary" onClick={handleSubmit} loading={submitting}>
            确定
          </Button>
        </Space>
      }
    >
      <Form form={form} layout="vertical">
        <Form.Item
          name="customerUuid"
          label="客户"
          rules={[{ required: true, message: '请选择客户' }]}
        >
          <Select
            showSearch
            placeholder="选择客户"
            options={customerOptions}
            filterOption={(input, option) =>
              (option?.label ?? '').toLowerCase().includes(input.toLowerCase())
            }
          />
        </Form.Item>

        <Form.Item
          name="periodRange"
          label="账期范围"
          rules={[{ required: true, message: '请选择账期范围' }]}
        >
          <RangePicker style={{ width: '100%' }} />
        </Form.Item>

        <Form.Item
          name="settleDate"
          label="结算日期"
          initialValue={dayjs()}
        >
          <DatePicker style={{ width: '100%' }} />
        </Form.Item>

        <Form.Item name="isInvoice" label="是否开票" initialValue={2}>
          <Radio.Group>
            <Radio value={1}>开票</Radio>
            <Radio value={2}>不开票</Radio>
          </Radio.Group>
        </Form.Item>

        <Form.Item name="remark" label="备注">
          <Input.TextArea rows={3} placeholder="备注信息" />
        </Form.Item>
      </Form>
    </Drawer>
  )
}
