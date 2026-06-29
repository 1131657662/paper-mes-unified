import { useEffect, useState } from 'react'
import {
  Button,
  Col,
  DatePicker,
  Divider,
  Drawer,
  Form,
  Input,
  InputNumber,
  Row,
  Select,
  Space,
  Typography,
  message,
} from 'antd'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import { createProcessOrder } from '../../api/processOrder'
import { pageCustomers } from '../../api/customer'
import { pageWarehouses } from '../../api/warehouse'
import type { ProcessOrderCreateDTO } from '../../types/processOrder'
import { IS_INVOICE, PRIORITY, PROCESS_MODE, STEP_TYPE } from '../../constants/processOrder'

interface Props {
  open: boolean
  onClose: () => void
  onSuccess: () => void
}

interface Option {
  value: string
  label: string
}

const toOptions = (map: Record<number, string>) =>
  Object.entries(map).map(([value, label]) => ({ value: Number(value), label }))

export default function OrderCreateDrawer({ open, onClose, onSuccess }: Props) {
  const [form] = Form.useForm()
  const [submitting, setSubmitting] = useState(false)
  const [customers, setCustomers] = useState<Option[]>([])
  const [warehouses, setWarehouses] = useState<Option[]>([])

  useEffect(() => {
    if (!open) return
    form.resetFields()
    form.setFieldsValue({
      orderDate: dayjs(),
      priority: 1,
      isInvoice: 1,
      originalRolls: [{ pieceNum: 1, processMode: 1, mainStepType: 1 }],
    })
    pageCustomers({ current: 1, size: 200 }).then((res) =>
      setCustomers(
        (res.records ?? []).map((c) => ({ value: c.uuid, label: c.customerName })),
      ),
    )
    pageWarehouses({ current: 1, size: 200 }).then((res) =>
      setWarehouses(
        (res.records ?? []).map((w) => ({ value: w.uuid, label: w.warehouseName })),
      ),
    )
  }, [open, form])

  const handleSubmit = async () => {
    const values = await form.validateFields()
    const dto: ProcessOrderCreateDTO = {
      ...values,
      orderDate: values.orderDate ? dayjs(values.orderDate).format('YYYY-MM-DD') : undefined,
      expectFinishDate: values.expectFinishDate
        ? dayjs(values.expectFinishDate).format('YYYY-MM-DD')
        : undefined,
    }
    setSubmitting(true)
    try {
      await createProcessOrder(dto)
      message.success('新增成功')
      onSuccess()
      onClose()
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Drawer
      title="新建加工单"
      width={920}
      open={open}
      onClose={onClose}
      destroyOnClose
      className="mes-detail-drawer"
      extra={
        <Space>
          <Button onClick={onClose}>取消</Button>
          <Button type="primary" loading={submitting} onClick={handleSubmit}>
            提交
          </Button>
        </Space>
      }
    >
      <div className="mes-drawer-content">
        <section className="mes-drawer-section">
          <div className="mes-drawer-section__head">
            <div>
              <h3>基础信息</h3>
              <p>填写客户、日期、仓库、费用和开票信息。</p>
            </div>
          </div>
          <Form form={form} layout="vertical">
        <Row gutter={16}>
          <Col span={8}>
            <Form.Item
              name="customerUuid"
              label="客户"
              rules={[{ required: true, message: '请选择客户' }]}
            >
              <Select
                showSearch
                optionFilterProp="label"
                placeholder="请选择客户"
                options={customers}
              />
            </Form.Item>
          </Col>
          <Col span={8}>
            <Form.Item
              name="orderDate"
              label="制单日期"
              rules={[{ required: true, message: '请选择制单日期' }]}
            >
              <DatePicker style={{ width: '100%' }} />
            </Form.Item>
          </Col>
          <Col span={8}>
            <Form.Item name="expectFinishDate" label="期望完成日期">
              <DatePicker style={{ width: '100%' }} />
            </Form.Item>
          </Col>
          <Col span={8}>
            <Form.Item name="priority" label="优先级">
              <Select options={toOptions(PRIORITY)} />
            </Form.Item>
          </Col>
          <Col span={8}>
            <Form.Item name="warehouseUuid" label="仓库">
              <Select allowClear placeholder="请选择仓库" options={warehouses} />
            </Form.Item>
          </Col>
          <Col span={8}>
            <Form.Item name="teamGroup" label="班组">
              <Input placeholder="班组" />
            </Form.Item>
          </Col>
          <Col span={8}>
            <Form.Item name="isInvoice" label="是否开票">
              <Select options={toOptions(IS_INVOICE)} />
            </Form.Item>
          </Col>
          <Col span={8}>
            <Form.Item name="taxRate" label="税率(%)">
              <InputNumber min={0} max={100} style={{ width: '100%' }} placeholder="如 13" />
            </Form.Item>
          </Col>
          <Col span={8}>
            <Form.Item name="urgentFee" label="加急费">
              <InputNumber min={0} style={{ width: '100%' }} />
            </Form.Item>
          </Col>
          <Col span={8}>
            <Form.Item name="palletFee" label="托盘费">
              <InputNumber min={0} style={{ width: '100%' }} />
            </Form.Item>
          </Col>
          <Col span={8}>
            <Form.Item name="freightFee" label="运费">
              <InputNumber min={0} style={{ width: '100%' }} />
            </Form.Item>
          </Col>
          <Col span={8}>
            <Form.Item name="otherFee" label="其他费">
              <InputNumber min={0} style={{ width: '100%' }} />
            </Form.Item>
          </Col>
          <Col span={24}>
            <Form.Item name="remark" label="备注">
              <Input.TextArea rows={1} placeholder="备注" />
            </Form.Item>
          </Col>
        </Row>

        <Divider orientation="left">
          <Typography.Text strong>原纸明细</Typography.Text>
        </Divider>

        <Form.List
          name="originalRolls"
          rules={[
            {
              validator: async (_, rolls) => {
                if (!rolls || rolls.length < 1) {
                  return Promise.reject(new Error('至少添加一条原纸明细'))
                }
              },
            },
          ]}
        >
          {(fields, { add, remove }, { errors }) => (
            <>
              {fields.map(({ key, name, ...rest }) => (
                <Row gutter={8} key={key} align="middle" style={{ marginBottom: 4 }}>
                  <Col span={4}>
                    <Form.Item
                      {...rest}
                      name={[name, 'paperName']}
                      rules={[{ required: true, message: '品名' }]}
                      style={{ marginBottom: 8 }}
                    >
                      <Input placeholder="品名*" />
                    </Form.Item>
                  </Col>
                  <Col span={3}>
                    <Form.Item
                      {...rest}
                      name={[name, 'gramWeight']}
                      rules={[{ required: true, message: '克重' }]}
                      style={{ marginBottom: 8 }}
                    >
                      <InputNumber min={1} style={{ width: '100%' }} placeholder="克重*" />
                    </Form.Item>
                  </Col>
                  <Col span={3}>
                    <Form.Item
                      {...rest}
                      name={[name, 'originalWidth']}
                      rules={[{ required: true, message: '门幅' }]}
                      style={{ marginBottom: 8 }}
                    >
                      <InputNumber min={1} style={{ width: '100%' }} placeholder="门幅*" />
                    </Form.Item>
                  </Col>
                  <Col span={3}>
                    <Form.Item
                      {...rest}
                      name={[name, 'rollWeight']}
                      rules={[{ required: true, message: '单重' }]}
                      style={{ marginBottom: 8 }}
                    >
                      <InputNumber min={0} style={{ width: '100%' }} placeholder="单重kg*" />
                    </Form.Item>
                  </Col>
                  <Col span={2}>
                    <Form.Item {...rest} name={[name, 'pieceNum']} style={{ marginBottom: 8 }}>
                      <InputNumber min={1} style={{ width: '100%' }} placeholder="件数" />
                    </Form.Item>
                  </Col>
                  <Col span={3}>
                    <Form.Item {...rest} name={[name, 'processMode']} style={{ marginBottom: 8 }}>
                      <Select options={toOptions(PROCESS_MODE)} placeholder="加工方式" />
                    </Form.Item>
                  </Col>
                  <Col span={2}>
                    <Form.Item {...rest} name={[name, 'mainStepType']} style={{ marginBottom: 8 }}>
                      <Select options={toOptions(STEP_TYPE)} placeholder="工艺" />
                    </Form.Item>
                  </Col>
                  <Col span={3}>
                    <Form.Item {...rest} name={[name, 'rollNo']} style={{ marginBottom: 8 }}>
                      <Input placeholder="母卷号" />
                    </Form.Item>
                  </Col>
                  <Col span={1}>
                    <Button
                      type="text"
                      danger
                      icon={<DeleteOutlined />}
                      onClick={() => remove(name)}
                      disabled={fields.length <= 1}
                    />
                  </Col>
                </Row>
              ))}
              <Button
                type="dashed"
                block
                icon={<PlusOutlined />}
                onClick={() => add({ pieceNum: 1, processMode: 1, mainStepType: 1 })}
              >
                添加一行
              </Button>
              <Form.ErrorList errors={errors} />
            </>
          )}
        </Form.List>
      </Form>
        </section>
      </div>
    </Drawer>
  )
}
