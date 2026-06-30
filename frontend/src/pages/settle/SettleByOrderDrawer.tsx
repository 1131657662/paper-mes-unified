import { useEffect, useState } from 'react'
import { Button, DatePicker, Drawer, Form, Radio, Input, Select, message } from 'antd'
import dayjs from 'dayjs'
import { pageProcessOrders } from '../../api/processOrder'
import { createSettleByOrder } from '../../api/settle'
import type { SettleByOrderDTO } from '../../types/settle'

interface Props {
  open: boolean
  onClose: () => void
  onSuccess: () => void
}

export default function SettleByOrderDrawer({ open, onClose, onSuccess }: Props) {
  const [form] = Form.useForm()
  const [submitting, setSubmitting] = useState(false)
  const [orderOptions, setOrderOptions] = useState<{ value: string; label: string }[]>([])

  useEffect(() => {
    if (open) {
      pageProcessOrders({ current: 1, size: 200, orderStatus: 4 })
        .then((res) => {
          setOrderOptions(
            (res.records ?? []).map((o) => ({ value: o.uuid, label: o.orderNo || '' })),
          )
        })
        .catch(() => {
          message.error('加工单列表加载失败')
        })
    } else {
      form.resetFields()
    }
  }, [open, form])

  const handleSubmit = async () => {
    const values = await form.validateFields()

    const dto: SettleByOrderDTO = {
      orderUuid: values.orderUuid,
      settleDate: values.settleDate?.format('YYYY-MM-DD'),
      isInvoice: values.isInvoice,
      remark: values.remark,
    }

    setSubmitting(true)
    try {
      await createSettleByOrder(dto)
      message.success('结算单创建成功')
      onSuccess()
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Drawer
      title="按单生成结算单"
      width={640}
      open={open}
      onClose={onClose}
      destroyOnClose
      footer={
        <div className="mes-drawer-footer">
          <Button onClick={onClose}>取消</Button>
          <Button type="primary" onClick={handleSubmit} loading={submitting}>
            确定
          </Button>
        </div>
      }
    >
      <Form form={form} layout="vertical">
        <Form.Item
          name="orderUuid"
          label="加工单"
          rules={[{ required: true, message: '请选择加工单' }]}
        >
          <Select
            showSearch
            placeholder="选择已完成的加工单"
            options={orderOptions}
            filterOption={(input, option) =>
              (option?.label ?? '').toLowerCase().includes(input.toLowerCase())
            }
          />
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
