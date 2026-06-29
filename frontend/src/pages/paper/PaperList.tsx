import { useRef, useState } from 'react'
import { Button, Form, Input, InputNumber, Modal, Popconfirm, Tag, message } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { ProTable } from '@ant-design/pro-components'
import type { ActionType, ProColumns } from '@ant-design/pro-components'
import { pagePapers, createPaper, updatePaper, deletePaper } from '../../api/paper'
import type { Paper, PaperSaveDTO } from '../../types/paper'

export default function PaperList() {
  const actionRef = useRef<ActionType>(null)
  const [form] = Form.useForm<PaperSaveDTO>()
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<Paper | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const openCreate = () => {
    setEditing(null)
    form.resetFields()
    setModalOpen(true)
  }

  const openEdit = (record: Paper) => {
    setEditing(record)
    form.setFieldsValue({
      paperCode: record.paperCode,
      paperName: record.paperName,
      gramWeight: record.gramWeight,
      paperType: record.paperType,
      remark: record.remark,
    })
    setModalOpen(true)
  }

  const handleSubmit = async () => {
    const values = await form.validateFields()
    setSubmitting(true)
    try {
      if (editing) {
        await updatePaper(editing.uuid, values)
        message.success('修改成功')
      } else {
        await createPaper(values)
        message.success('新增成功')
      }
      setModalOpen(false)
      actionRef.current?.reload()
    } finally {
      setSubmitting(false)
    }
  }

  const handleDelete = async (record: Paper) => {
    await deletePaper(record.uuid)
    message.success('删除成功')
    actionRef.current?.reload()
  }

  const columns: ProColumns<Paper>[] = [
    { title: '纸张编码', dataIndex: 'paperCode', width: 140 },
    { title: '纸张品名', dataIndex: 'paperName', ellipsis: true },
    { title: '克重(g/㎡)', dataIndex: 'gramWeight', width: 120, search: false },
    {
      title: '类型',
      dataIndex: 'paperType',
      width: 120,
      search: false,
      render: (text) => <Tag className="mes-data-tag">{text || '-'}</Tag>,
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
          <Popconfirm title="确认删除该纸张？" onConfirm={() => handleDelete(record)}>
            <Button danger type="link" size="small">删除</Button>
          </Popconfirm>
        </div>
      ),
    },
  ]

  return (
    <>
      <ProTable<Paper>
        className="mes-pro-table-page"
        rowKey="uuid"
        actionRef={actionRef}
        columns={columns}
        headerTitle="纸张档案"
        toolBarRender={() => [
          <Button key="add" type="primary" icon={<PlusOutlined />} onClick={openCreate}>
            新增
          </Button>,
        ]}
        request={async (params) => {
          const res = await pagePapers({
            current: params.current,
            size: params.pageSize,
            keyword: params.paperCode || params.paperName,
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
        title={editing ? '编辑纸张' : '新增纸张'}
        open={modalOpen}
        onOk={handleSubmit}
        onCancel={() => setModalOpen(false)}
        confirmLoading={submitting}
        destroyOnClose
        width={560}
      >
        <Form className="mes-modal-form" form={form} layout="vertical">
          <div className="mes-form-grid">
            <Form.Item name="paperCode" label="纸张编码">
              <Input placeholder="如 ZZ001" disabled={!!editing} />
            </Form.Item>
            <Form.Item
              name="paperName"
              label="纸张品名"
              rules={[{ required: true, message: '请输入纸张品名' }]}
            >
              <Input placeholder="请输入纸张品名" />
            </Form.Item>
            <Form.Item name="gramWeight" label="克重(g/㎡)">
              <InputNumber min={0} step={1} placeholder="如 250" />
            </Form.Item>
            <Form.Item name="paperType" label="类型">
              <Input placeholder="如 白卡/牛皮" />
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
