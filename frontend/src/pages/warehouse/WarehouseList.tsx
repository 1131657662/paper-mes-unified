import { useRef, useState } from 'react'
import { Button, Form, Input, Modal, Popconfirm, Select, Tag, message } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { ProTable } from '@ant-design/pro-components'
import type { ActionType, ProColumns } from '@ant-design/pro-components'
import {
  pageWarehouses,
  createWarehouse,
  updateWarehouse,
  deleteWarehouse,
} from '../../api/warehouse'
import type { Warehouse, WarehouseSaveDTO } from '../../types/warehouse'

/** 状态字典：1 启用 / 2 停用。 */
const STATUS: Record<number, string> = { 1: '启用', 2: '停用' }

export default function WarehouseList() {
  const actionRef = useRef<ActionType>(null)
  const [form] = Form.useForm<WarehouseSaveDTO>()
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<Warehouse | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const openCreate = () => {
    setEditing(null)
    form.resetFields()
    form.setFieldsValue({ status: 1 })
    setModalOpen(true)
  }

  const openEdit = (record: Warehouse) => {
    setEditing(record)
    form.setFieldsValue({
      warehouseCode: record.warehouseCode,
      warehouseName: record.warehouseName,
      location: record.location,
      status: record.status,
      remark: record.remark,
    })
    setModalOpen(true)
  }

  const handleSubmit = async () => {
    const values = await form.validateFields()
    setSubmitting(true)
    try {
      if (editing) {
        await updateWarehouse(editing.uuid, values)
        message.success('修改成功')
      } else {
        await createWarehouse(values)
        message.success('新增成功')
      }
      setModalOpen(false)
      actionRef.current?.reload()
    } finally {
      setSubmitting(false)
    }
  }

  const handleDelete = async (record: Warehouse) => {
    await deleteWarehouse(record.uuid)
    message.success('删除成功')
    actionRef.current?.reload()
  }

  const columns: ProColumns<Warehouse>[] = [
    { title: '仓库编码', dataIndex: 'warehouseCode', width: 140 },
    { title: '仓库名称', dataIndex: 'warehouseName', ellipsis: true },
    { title: '库位', dataIndex: 'location', width: 200, search: false },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      search: false,
      render: (_, r) => (
        <Tag className="mes-data-tag" color={r.status === 1 ? 'green' : 'default'}>
          {r.status ? STATUS[r.status] ?? '-' : '-'}
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
          <Popconfirm title="确认删除该仓库？" onConfirm={() => handleDelete(record)}>
            <Button danger type="link" size="small">删除</Button>
          </Popconfirm>
        </div>
      ),
    },
  ]

  return (
    <>
      <ProTable<Warehouse>
        className="mes-pro-table-page"
        rowKey="uuid"
        actionRef={actionRef}
        columns={columns}
        headerTitle="仓库档案"
        toolBarRender={() => [
          <Button key="add" type="primary" icon={<PlusOutlined />} onClick={openCreate}>
            新增
          </Button>,
        ]}
        request={async (params) => {
          const res = await pageWarehouses({
            current: params.current,
            size: params.pageSize,
            keyword: params.warehouseCode || params.warehouseName,
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
        title={editing ? '编辑仓库' : '新增仓库'}
        open={modalOpen}
        onOk={handleSubmit}
        onCancel={() => setModalOpen(false)}
        confirmLoading={submitting}
        destroyOnClose
        width={560}
      >
        <Form className="mes-modal-form" form={form} layout="vertical">
          <div className="mes-form-grid">
            <Form.Item name="warehouseCode" label="仓库编码">
              <Input placeholder="如 CK001" disabled={!!editing} />
            </Form.Item>
            <Form.Item
              name="warehouseName"
              label="仓库名称"
              rules={[{ required: true, message: '请输入仓库名称' }]}
            >
              <Input placeholder="请输入仓库名称" />
            </Form.Item>
            <Form.Item name="location" label="库位/地址">
              <Input placeholder="如 A区1号" />
            </Form.Item>
            <Form.Item name="status" label="状态">
              <Select
                options={[
                  { value: 1, label: '启用' },
                  { value: 2, label: '停用' },
                ]}
              />
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
