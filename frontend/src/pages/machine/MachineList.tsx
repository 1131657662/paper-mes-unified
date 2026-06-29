import { useRef, useState } from 'react'
import { Button, Form, Input, Modal, Popconfirm, Select, Tag, message } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { ProTable } from '@ant-design/pro-components'
import type { ActionType, ProColumns } from '@ant-design/pro-components'
import { pageMachines, createMachine, updateMachine, deleteMachine } from '../../api/machine'
import type { Machine, MachineSaveDTO } from '../../types/machine'

/** 机台类型字典：1 锯纸 / 2 复卷 / 3 通用。 */
const MACHINE_TYPE: Record<number, string> = { 1: '锯纸', 2: '复卷', 3: '通用' }
/** 状态字典：1 启用 / 2 停用。 */
const STATUS: Record<number, string> = { 1: '启用', 2: '停用' }

export default function MachineList() {
  const actionRef = useRef<ActionType>(null)
  const [form] = Form.useForm<MachineSaveDTO>()
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<Machine | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const openCreate = () => {
    setEditing(null)
    form.resetFields()
    form.setFieldsValue({ status: 1 })
    setModalOpen(true)
  }

  const openEdit = (record: Machine) => {
    setEditing(record)
    form.setFieldsValue({
      machineCode: record.machineCode,
      machineName: record.machineName,
      machineType: record.machineType,
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
        await updateMachine(editing.uuid, values)
        message.success('修改成功')
      } else {
        await createMachine(values)
        message.success('新增成功')
      }
      setModalOpen(false)
      actionRef.current?.reload()
    } finally {
      setSubmitting(false)
    }
  }

  const handleDelete = async (record: Machine) => {
    await deleteMachine(record.uuid)
    message.success('删除成功')
    actionRef.current?.reload()
  }

  const columns: ProColumns<Machine>[] = [
    { title: '机台编码', dataIndex: 'machineCode', width: 140 },
    { title: '机台名称', dataIndex: 'machineName', ellipsis: true },
    {
      title: '类型',
      dataIndex: 'machineType',
      width: 100,
      search: false,
      render: (_, r) => (
        <Tag className="mes-data-tag" color={r.machineType ? 'blue' : 'default'}>
          {r.machineType ? MACHINE_TYPE[r.machineType] ?? '-' : '-'}
        </Tag>
      ),
    },
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
          <Popconfirm title="确认删除该机台？" onConfirm={() => handleDelete(record)}>
            <Button danger type="link" size="small">删除</Button>
          </Popconfirm>
        </div>
      ),
    },
  ]

  return (
    <>
      <ProTable<Machine>
        className="mes-pro-table-page"
        rowKey="uuid"
        actionRef={actionRef}
        columns={columns}
        headerTitle="机台档案"
        toolBarRender={() => [
          <Button key="add" type="primary" icon={<PlusOutlined />} onClick={openCreate}>
            新增
          </Button>,
        ]}
        request={async (params) => {
          const res = await pageMachines({
            current: params.current,
            size: params.pageSize,
            keyword: params.machineCode || params.machineName,
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
        title={editing ? '编辑机台' : '新增机台'}
        open={modalOpen}
        onOk={handleSubmit}
        onCancel={() => setModalOpen(false)}
        confirmLoading={submitting}
        destroyOnClose
        width={560}
      >
        <Form className="mes-modal-form" form={form} layout="vertical">
          <div className="mes-form-grid">
            <Form.Item name="machineCode" label="机台编码">
              <Input placeholder="如 JT001" disabled={!!editing} />
            </Form.Item>
            <Form.Item
              name="machineName"
              label="机台名称"
              rules={[{ required: true, message: '请输入机台名称' }]}
            >
              <Input placeholder="请输入机台名称" />
            </Form.Item>
            <Form.Item name="machineType" label="类型">
              <Select
                allowClear
                placeholder="请选择"
                options={[
                  { value: 1, label: '锯纸' },
                  { value: 2, label: '复卷' },
                  { value: 3, label: '通用' },
                ]}
              />
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
