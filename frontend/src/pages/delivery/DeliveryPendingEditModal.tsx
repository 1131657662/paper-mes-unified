import { DatePicker, Form, Input, Modal, message } from 'antd'
import { useUpdatePendingDelivery } from '../../features/delivery/hooks/useUpdatePendingDelivery'
import type { DeliveryOrder } from '../../types/delivery'
import {
  buildDeliveryPendingUpdateDTO,
  deliveryPendingEditInitialValues,
  type DeliveryPendingEditFormValues,
} from './deliveryPendingEditModel'

interface Props {
  open: boolean
  order: DeliveryOrder
  onCancel: () => void
  onSaved: () => void
}

export default function DeliveryPendingEditModal(props: Props) {
  const [form] = Form.useForm<DeliveryPendingEditFormValues>()
  const updateMutation = useUpdatePendingDelivery()

  const focusCarNo = (visible: boolean) => {
    if (visible) form.getFieldInstance('carNo')?.focus()
  }

  const handleSave = async () => {
    const values = await form.validateFields()
    await updateMutation.mutateAsync({
      uuid: props.order.uuid,
      data: buildDeliveryPendingUpdateDTO(values),
    })
    message.success('待出库信息已更新')
    props.onSaved()
  }

  return (
    <Modal title="编辑出库信息" open={props.open} width={720}
      confirmLoading={updateMutation.isPending} okText="保存" cancelText="取消"
      afterOpenChange={focusCarNo}
      onCancel={props.onCancel} onOk={() => void handleSave().catch(() => undefined)}>
      <Form form={form} layout="vertical" initialValues={deliveryPendingEditInitialValues(props.order)}>
        <PendingHeaderFields />
      </Form>
    </Modal>
  )
}

function PendingHeaderFields() {
  return (
    <div className="document-module-grid">
      <Form.Item name="receiverCustomerName" label="收货客户（选填）"
        rules={[{ max: 100, message: '收货客户名称不能超过100个字符' }]}>
        <Input maxLength={100} placeholder="货主告知的收货客户，不填则留空" />
      </Form.Item>
      <Form.Item name="deliveryDate" label="出库日期"
        rules={[{ required: true, message: '请选择出库日期' }]}>
        <DatePicker allowClear={false} format="YYYY-MM-DD" />
      </Form.Item>
      <Form.Item name="pickerName" label="提货人"
        rules={[{ max: 50, message: '提货人不能超过50个字符' }]}>
        <Input maxLength={50} placeholder="司机或提货联系人" />
      </Form.Item>
      <Form.Item name="carNo" label="车牌号"
        rules={[{ max: 50, message: '车牌号不能超过50个字符' }]}>
        <Input maxLength={50} placeholder="例如：浙A12345" />
      </Form.Item>
      <Form.Item name="containerNo" label="柜号"
        rules={[{ max: 50, message: '柜号不能超过50个字符' }]}>
        <Input maxLength={50} placeholder="集装箱或货柜编号" />
      </Form.Item>
      <Form.Item className="document-module-grid__full" name="remark" label="备注"
        rules={[{ max: 255, message: '备注不能超过255个字符' }]}>
        <Input.TextArea maxLength={255} rows={3} placeholder="本次出库备注" showCount />
      </Form.Item>
    </div>
  )
}
