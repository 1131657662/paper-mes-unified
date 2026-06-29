import { useRef, useState } from 'react'
import { Button, Form, Input, InputNumber, Modal, Popconfirm, Select, Tag, message } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { ProTable } from '@ant-design/pro-components'
import type { ActionType, ProColumns } from '@ant-design/pro-components'
import {
  pageCustomers,
  createCustomer,
  updateCustomer,
  deleteCustomer,
} from '../../api/customer'
import type { Customer, CustomerSaveDTO } from '../../types/customer'

const INVOICE_TYPE: Record<number, string> = { 1: '开票', 2: '不开票' }

export default function CustomerList() {
  const actionRef = useRef<ActionType>(null)
  const [form] = Form.useForm<CustomerSaveDTO>()
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<Customer | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const openCreate = () => {
    setEditing(null)
    form.resetFields()
    setModalOpen(true)
  }

  const openEdit = (record: Customer) => {
    setEditing(record)
    form.setFieldsValue({
      customerCode: record.customerCode,
      customerName: record.customerName,
      contact: record.contact,
      phone: record.phone,
      settleType: record.settleType,
      settleDay: record.settleDay,
      defaultInvoice: record.defaultInvoice,
      taxRate: record.taxRate,
      remark: record.remark,
    })
    setModalOpen(true)
  }

  const handleSubmit = async () => {
    const values = await form.validateFields()
    setSubmitting(true)
    try {
      if (editing) {
        await updateCustomer(editing.uuid, values)
        message.success('修改成功')
      } else {
        await createCustomer(values)
        message.success('新增成功')
      }
      setModalOpen(false)
      actionRef.current?.reload()
    } finally {
      setSubmitting(false)
    }
  }

  const handleDelete = async (record: Customer) => {
    await deleteCustomer(record.uuid)
    message.success('删除成功')
    actionRef.current?.reload()
  }

  const columns: ProColumns<Customer>[] = [
    { title: '客户编码', dataIndex: 'customerCode', width: 140 },
    { title: '客户名称', dataIndex: 'customerName', ellipsis: true },
    { title: '联系人', dataIndex: 'contact', width: 120, search: false },
    { title: '电话', dataIndex: 'phone', width: 140, search: false },
    {
      title: '结算方式',
      dataIndex: 'settleType',
      width: 100,
      search: false,
      render: (_, r) => (
        <Tag className="mes-data-tag" color={r.settleType === 2 ? 'blue' : 'default'}>
          {settleText(r)}
        </Tag>
      ),
    },
    {
      title: '默认开票',
      dataIndex: 'defaultInvoice',
      width: 100,
      search: false,
      render: (_, r) => (
        <Tag className="mes-data-tag" color={r.defaultInvoice === 1 ? 'green' : 'default'}>
          {r.defaultInvoice ? INVOICE_TYPE[r.defaultInvoice] ?? '-' : '-'}
        </Tag>
      ),
    },
    { title: '创建时间', dataIndex: 'createTime', width: 180, search: false, valueType: 'dateTime' },
    {
      title: '操作',
      valueType: 'option',
      width: 140,
      render: (_, record) => (
        <div className="mes-table-actions">
          <Button type="link" size="small" onClick={() => openEdit(record)}>
            编辑
          </Button>
          <Popconfirm title="确认删除该客户？" onConfirm={() => handleDelete(record)}>
            <Button danger type="link" size="small">删除</Button>
          </Popconfirm>
        </div>
      ),
    },
  ]

  return (
    <>
      <ProTable<Customer>
        className="mes-pro-table-page"
        rowKey="uuid"
        actionRef={actionRef}
        columns={columns}
        headerTitle="客户管理"
        toolBarRender={() => [
          <Button key="add" type="primary" icon={<PlusOutlined />} onClick={openCreate}>
            新增
          </Button>,
        ]}
        request={async (params) => {
          const res = await pageCustomers({
            current: params.current,
            size: params.pageSize,
            keyword: params.customerCode || params.customerName,
          })
          return { data: res.records ?? [], total: res.total ?? 0, success: true }
        }}
        bordered
        pagination={{
          defaultPageSize: 10,
          showSizeChanger: true,
          pageSizeOptions: [10, 20, 50, 100, 200, 500, 1000],
        }}
        search={{ labelWidth: 'auto' }}
        scroll={{ x: 'max-content' }}
      />

      <Modal
        title={editing ? '编辑客户' : '新增客户'}
        open={modalOpen}
        onOk={handleSubmit}
        onCancel={() => setModalOpen(false)}
        confirmLoading={submitting}
        destroyOnClose
        width={640}
      >
        <Form className="mes-modal-form" form={form} layout="vertical">
          <Form.Item
            name="customerCode"
            label="客户编码"
            rules={[{ required: true, message: '请输入客户编码' }]}
          >
            <Input placeholder="如 KH001" disabled={!!editing} />
          </Form.Item>
          <Form.Item
            name="customerName"
            label="客户名称"
            rules={[{ required: true, message: '请输入客户名称' }]}
          >
            <Input placeholder="请输入客户名称" />
          </Form.Item>
          <div className="mes-form-grid">
            <Form.Item name="contact" label="联系人">
              <Input placeholder="联系人" />
            </Form.Item>
            <Form.Item name="phone" label="电话">
              <Input placeholder="电话" />
            </Form.Item>
            <Form.Item name="settleType" label="结算方式">
              <Select
                placeholder="请选择"
                options={[
                  { value: 1, label: '次结' },
                  { value: 2, label: '月结' },
                ]}
              />
            </Form.Item>
            <Form.Item name="settleDay" label="月结日">
              <InputNumber min={1} max={31} placeholder="如 25" />
            </Form.Item>
            <Form.Item name="defaultInvoice" label="默认开票">
              <Select
                placeholder="请选择"
                options={[
                  { value: 1, label: '开票' },
                  { value: 2, label: '不开票' },
                ]}
              />
            </Form.Item>
            <Form.Item name="taxRate" label="税率(%)">
              <InputNumber min={0} max={100} step={1} placeholder="如 13" />
            </Form.Item>
            <Form.Item className="mes-form-grid__full" name="remark" label="备注">
              <Input.TextArea rows={2} placeholder="备注" />
            </Form.Item>
          </div>
        </Form>
      </Modal>
    </>
  )
}

function settleText(customer: Customer) {
  if (customer.settleType === 2) {
    return customer.settleDay ? `月结 ${customer.settleDay}日` : '月结'
  }
  if (customer.settleType === 1) return '次结'
  return '-'
}
