import { useEffect, useState } from 'react'
import { Button, DatePicker, Drawer, Form, Input, InputNumber, Modal, Select, Table, Tag, message } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'
import { pageCustomers } from '../../api/customer'
import { getAvailableFinishes, createDeliveryOrder } from '../../api/delivery'
import type { AvailableFinishVO, DeliveryCreateDTO } from '../../types/delivery'
import { SOURCE_TYPE } from '../../constants/delivery'

interface Props {
  open: boolean
  onClose: () => void
  onSuccess: () => void
}

interface FinishItem extends AvailableFinishVO {
  outWeight: number
  remark?: string
}

export default function DeliveryCreateDrawer({ open, onClose, onSuccess }: Props) {
  const [form] = Form.useForm()
  const [submitting, setSubmitting] = useState(false)
  const [customerOptions, setCustomerOptions] = useState<{ value: string; label: string }[]>([])
  const [finishes, setFinishes] = useState<FinishItem[]>([])
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([])
  const [loadingFinishes, setLoadingFinishes] = useState(false)

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
      setFinishes([])
      setSelectedRowKeys([])
    }
  }, [open, form])

  const handleCustomerChange = async (customerUuid: string) => {
    if (!customerUuid) {
      setFinishes([])
      setSelectedRowKeys([])
      return
    }

    setLoadingFinishes(true)
    try {
      const res = await getAvailableFinishes(customerUuid)
      const items: FinishItem[] = res.map((f) => ({
        ...f,
        outWeight: f.actualWeight,
      }))
      setFinishes(items)
      setSelectedRowKeys([])
    } finally {
      setLoadingFinishes(false)
    }
  }

  const handleOutWeightChange = (finishUuid: string, value: number | null) => {
    setFinishes((prev) =>
      prev.map((f) => (f.finishUuid === finishUuid ? { ...f, outWeight: value ?? 0 } : f)),
    )
  }

  const handleRemarkChange = (finishUuid: string, value: string) => {
    setFinishes((prev) =>
      prev.map((f) => (f.finishUuid === finishUuid ? { ...f, remark: value } : f)),
    )
  }

  const handleSubmit = async () => {
    const values = await form.validateFields()

    if (selectedRowKeys.length === 0) {
      message.error('请至少选择一件成品')
      return
    }

    const selectedFinishes = finishes.filter((f) => selectedRowKeys.includes(f.finishUuid))

    const dto: DeliveryCreateDTO = {
      customerUuid: values.customerUuid,
      deliveryDate: values.deliveryDate.format('YYYY-MM-DD'),
      pickerName: values.pickerName,
      carNo: values.carNo,
      containerNo: values.containerNo,
      remark: values.remark,
      forceRelease: false,
      items: selectedFinishes.map((f) => ({
        finishUuid: f.finishUuid,
        outWeight: f.outWeight !== f.actualWeight ? f.outWeight : undefined,
        remark: f.remark,
      })),
    }

    setSubmitting(true)
    try {
      await createDeliveryOrder(dto)
      message.success('出库单创建成功')
      onSuccess()
    } catch (error: any) {
      if (error.message && error.message.includes('次结客户存在未结清款项')) {
        Modal.confirm({
          title: '现结拦截',
          content: '该客户有未结清款项，是否授权放行出库？',
          onOk: async () => {
            try {
              await createDeliveryOrder({ ...dto, forceRelease: true })
              message.success('出库单创建成功（授权放行）')
              onSuccess()
            } catch (err) {
              setSubmitting(false)
              throw err
            }
          },
          onCancel: () => {
            setSubmitting(false)
          },
        })
      } else {
        setSubmitting(false)
        throw error
      }
    } finally {
      setSubmitting(false)
    }
  }

  const columns: ColumnsType<FinishItem> = [
    {
      title: '成品卷号',
      dataIndex: 'finishRollNo',
      width: 120,
      fixed: 'left',
    },
    {
      title: '加工单号',
      dataIndex: 'orderNo',
      width: 140,
    },
    {
      title: '品名',
      dataIndex: 'paperName',
      width: 120,
    },
    {
      title: '实际重量(kg)',
      dataIndex: 'actualWeight',
      width: 100,
    },
    {
      title: '出库重量(kg)',
      dataIndex: 'outWeight',
      width: 120,
      render: (_, record) => (
        <InputNumber
          min={0}
          max={record.actualWeight}
          value={record.outWeight}
          onChange={(v) => handleOutWeightChange(record.finishUuid, v)}
          style={{ width: '100%' }}
        />
      ),
    },
    {
      title: '来源类型',
      dataIndex: 'sourceType',
      width: 90,
      render: (v) => {
        const s = SOURCE_TYPE[v]
        return s ? <Tag color={s.color}>{s.text}</Tag> : '-'
      },
    },
    {
      title: '备注',
      dataIndex: 'remark',
      width: 150,
      render: (_, record) => (
        <Input
          placeholder="备注"
          value={record.remark}
          onChange={(e) => handleRemarkChange(record.finishUuid, e.target.value)}
        />
      ),
    },
  ]

  const rowSelection = {
    selectedRowKeys,
    onChange: setSelectedRowKeys,
  }

  return (
    <Drawer
      title="新建出库单"
      width={1000}
      open={open}
      onClose={onClose}
      destroyOnHidden
      className="mes-detail-drawer"
      footer={
        <div className="mes-drawer-footer">
          <Button onClick={onClose}>取消</Button>
          <Button type="primary" onClick={handleSubmit} loading={submitting}>
            确定
          </Button>
        </div>
      }
    >
      <div className="mes-drawer-content">
        <section className="mes-drawer-section">
          <div className="mes-drawer-section__head">
            <div>
              <h3>出库信息</h3>
              <p>选择客户后载入可出库成品卷。</p>
            </div>
          </div>
          <Form className="mes-modal-form" form={form} layout="vertical">
            <div className="mes-form-grid">
              <Form.Item
                name="customerUuid"
                label="客户"
                rules={[{ required: true, message: '请选择客户' }]}
              >
                <Select
                  showSearch
                  placeholder="选择客户"
                  allowClear
                  options={customerOptions}
                  onChange={(value) => handleCustomerChange(value || '')}
                  filterOption={(input, option) =>
                    (option?.label ?? '').toLowerCase().includes(input.toLowerCase())
                  }
                />
              </Form.Item>

              <Form.Item
                name="deliveryDate"
                label="出库日期"
                rules={[{ required: true, message: '请选择出库日期' }]}
                initialValue={dayjs()}
              >
                <DatePicker />
              </Form.Item>

              <Form.Item name="pickerName" label="提货人">
                <Input placeholder="提货人姓名" />
              </Form.Item>

              <Form.Item name="carNo" label="车牌号">
                <Input placeholder="车牌号" />
              </Form.Item>

              <Form.Item name="containerNo" label="柜号">
                <Input placeholder="柜号" />
              </Form.Item>

              <Form.Item className="mes-form-grid__full" name="remark" label="备注">
                <Input.TextArea rows={2} placeholder="备注信息" />
              </Form.Item>
            </div>
          </Form>
        </section>

        <section className="mes-drawer-section">
          <div className="mes-drawer-section__head">
            <div>
              <h3>选择成品</h3>
              <p>共 {finishes.length} 件可出库，已选 {selectedRowKeys.length} 件。</p>
            </div>
            <Button
              size="small"
              onClick={() => {
                const customerUuid = form.getFieldValue('customerUuid')
                if (customerUuid) handleCustomerChange(customerUuid)
              }}
            >
              刷新
            </Button>
          </div>
          <div className="mes-drawer-table">
            <Table
              rowKey="finishUuid"
              size="small"
              columns={columns}
              dataSource={finishes}
              rowSelection={rowSelection}
              loading={loadingFinishes}
              pagination={false}
              scroll={{ x: 'max-content', y: 400 }}
            />
          </div>
        </section>
      </div>
    </Drawer>
  )
}
