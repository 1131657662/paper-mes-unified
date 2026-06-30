import { useState } from 'react'
import { Button, Card, DatePicker, Form, Input, Modal, Select, Space, message } from 'antd'
import dayjs from 'dayjs'
import { useNavigate } from 'react-router-dom'
import MesPageHeader from '../../components/layout/MesPageHeader'
import { useCustomers } from '../../features/processOrderCreate/hooks/useReferenceData'
import { useAvailableFinishes } from '../../features/delivery/hooks/useAvailableFinishes'
import { useCreateDelivery } from '../../features/delivery/hooks/useCreateDelivery'
import { formatKg } from '../../features/delivery/utils/deliveryFormatters'
import type { DeliveryCreateDTO } from '../../types/delivery'
import DeliveryCreateTable, { type DeliveryLineEdit } from './DeliveryCreateTable'
import '../documentModule.css'

interface DeliveryCreateForm {
  carNo?: string
  containerNo?: string
  customerUuid: string
  deliveryDate: dayjs.Dayjs
  pickerName?: string
  remark?: string
}

export default function DeliveryCreatePage() {
  const [form] = Form.useForm<DeliveryCreateForm>()
  const navigate = useNavigate()
  const customersQuery = useCustomers()
  const createMutation = useCreateDelivery()
  const [customerUuid, setCustomerUuid] = useState<string>()
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([])
  const [lineEdits, setLineEdits] = useState<Record<string, DeliveryLineEdit>>({})
  const finishesQuery = useAvailableFinishes(customerUuid)
  const finishes = finishesQuery.data ?? []
  const selectedFinishes = finishes.filter((item) => selectedRowKeys.includes(item.finishUuid))

  const handleCustomerChange = (value?: string) => {
    setCustomerUuid(value)
    setSelectedRowKeys([])
    setLineEdits({})
  }

  const handleEditChange = (finishUuid: string, value: DeliveryLineEdit) => {
    setLineEdits((prev) => ({ ...prev, [finishUuid]: { ...prev[finishUuid], ...value } }))
  }

  const handleSubmit = async () => {
    const values = await form.validateFields()
    if (selectedFinishes.length === 0) {
      message.warning('请先勾选本次要出库的成品卷')
      return
    }
    const hasRisk = selectedFinishes.some((item) => item.settlementRisk)
    if (hasRisk) {
      await confirmCashRelease()
    }
    const uuid = await createMutation.mutateAsync(buildCreateDTO(values, selectedFinishes, lineEdits, hasRisk))
    message.success('出库单已生成')
    navigate(`/delivery-orders/${uuid}`)
  }

  return (
    <div className="document-module-page">
      <MesPageHeader
        title="新建出库单"
        description="先选择客户和装车信息，再从已入库成品中勾选本次出库卷。保存后进入出库单详情，可打印给司机签收。"
        onBack={() => navigate('/delivery-orders')}
        actions={(
          <Space>
            <Button onClick={() => navigate('/delivery-orders')}>取消</Button>
            <Button type="primary" loading={createMutation.isPending} onClick={handleSubmit}>保存出库单</Button>
          </Space>
        )}
      />

      <Card className="document-module-card" title="基础信息">
        <Form form={form} layout="vertical" initialValues={{ deliveryDate: dayjs() }}>
          <div className="document-module-grid">
            <Form.Item
              name="customerUuid"
              label="客户"
              rules={[{ required: true, message: '请选择客户' }]}
            >
              <Select
                allowClear
                showSearch
                loading={customersQuery.isLoading}
                placeholder="选择客户"
                options={(customersQuery.data?.records ?? []).map((item) => ({
                  label: item.customerName,
                  value: item.uuid,
                }))}
                optionFilterProp="label"
                onChange={handleCustomerChange}
              />
            </Form.Item>
            <Form.Item
              name="deliveryDate"
              label="出库日期"
              rules={[{ required: true, message: '请选择出库日期' }]}
            >
              <DatePicker />
            </Form.Item>
            <Form.Item name="pickerName" label="提货人">
              <Input placeholder="司机或提货人姓名" />
            </Form.Item>
            <Form.Item name="carNo" label="车牌号">
              <Input placeholder="车牌号" />
            </Form.Item>
            <Form.Item name="containerNo" label="柜号">
              <Input placeholder="柜号" />
            </Form.Item>
            <Form.Item className="document-module-grid__full" name="remark" label="备注">
              <Input.TextArea rows={2} placeholder="本次出库备注" />
            </Form.Item>
          </div>
        </Form>
      </Card>

      <Card
        className="document-module-card"
        title="选择出库成品"
        extra={<SelectedSummary count={selectedFinishes.length} weight={selectedWeight(selectedFinishes, lineEdits)} />}
      >
        <div className="document-module-table">
          <DeliveryCreateTable
            data={finishes}
            edits={lineEdits}
            loading={finishesQuery.isLoading || finishesQuery.isFetching}
            selectedRowKeys={selectedRowKeys}
            onEditChange={handleEditChange}
            onSelectionChange={setSelectedRowKeys}
          />
        </div>
      </Card>
    </div>
  )
}

function buildCreateDTO(
  values: DeliveryCreateForm,
  selectedFinishes: { finishUuid: string; actualWeight: number }[],
  lineEdits: Record<string, DeliveryLineEdit>,
  forceRelease: boolean,
): DeliveryCreateDTO {
  return {
    carNo: values.carNo,
    containerNo: values.containerNo,
    customerUuid: values.customerUuid,
    deliveryDate: values.deliveryDate.format('YYYY-MM-DD'),
    forceRelease,
    items: selectedFinishes.map((item) => ({
      finishUuid: item.finishUuid,
      outWeight: lineEdits[item.finishUuid]?.outWeight,
      remark: lineEdits[item.finishUuid]?.remark,
    })),
    pickerName: values.pickerName,
    remark: values.remark,
  }
}

function selectedWeight(
  items: { finishUuid: string; actualWeight: number }[],
  edits: Record<string, DeliveryLineEdit>,
) {
  return items.reduce((total, item) => total + (edits[item.finishUuid]?.outWeight ?? item.actualWeight), 0)
}

function SelectedSummary({ count, weight }: { count: number; weight: number }) {
  return (
    <div className="document-module-summary">
      <span>已选 <strong>{count}</strong> 卷</span>
      <span>合计 <strong>{formatKg(weight)}</strong></span>
    </div>
  )
}

function confirmCashRelease() {
  return new Promise<void>((resolve, reject) => {
    Modal.confirm({
      title: '次结出库确认',
      content: '本次选择包含次结且有待收款风险的加工单。确认后将按“警告放行”生成出库单。',
      okText: '警告放行',
      cancelText: '取消',
      onOk: () => resolve(),
      onCancel: () => reject(new Error('cancel')),
    })
  })
}
