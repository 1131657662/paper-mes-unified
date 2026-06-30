import { useEffect, useState } from 'react'
import {
  Alert,
  Button,
  Drawer,
  Form,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Select,
  Spin,
  Table,
  Tag,
  message,
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import {
  appendSpareRolls,
  batchGenerateFinishRolls,
  batchVoidFinishRolls,
  getProcessOrder,
  voidFinishRoll,
} from '../../api/processOrder'
import type {
  FinishRoll,
  FinishRollBatchDTO,
  ProcessOrderDetailVO,
  SpareRollAppendDTO,
  SpareRollBatchVoidDTO,
} from '../../types/processOrder'
import { FINISH_STATUS, ROLL_NO_STATUS } from '../../constants/processOrder'
import MesTooltip from '../../components/biz/MesTooltip'

interface Props {
  orderUuid: string | null
  open: boolean
  onClose: () => void
  onSuccess: () => void
}

export default function FinishRollManageDrawer({
  orderUuid,
  open,
  onClose,
  onSuccess,
}: Props) {
  const [loading, setLoading] = useState(false)
  const [detail, setDetail] = useState<ProcessOrderDetailVO | null>(null)
  const [filterStatus, setFilterStatus] = useState<number | undefined>()
  const [filterSpare, setFilterSpare] = useState<number | undefined>()
  const [filterSource, setFilterSource] = useState<number | undefined>()
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([])

  const [batchOpen, setBatchOpen] = useState(false)
  const [batchForm] = Form.useForm()
  const [batchSubmitting, setBatchSubmitting] = useState(false)

  const [spareOpen, setSpareOpen] = useState(false)
  const [spareForm] = Form.useForm()
  const [spareSubmitting, setSpareSubmitting] = useState(false)

  useEffect(() => {
    if (open && orderUuid) {
      loadDetail()
    } else {
      setDetail(null)
      setFilterStatus(undefined)
      setFilterSpare(undefined)
      setFilterSource(undefined)
      setSelectedRowKeys([])
    }
  }, [open, orderUuid])

  const loadDetail = async () => {
    if (!orderUuid) return
    setLoading(true)
    try {
      const res = await getProcessOrder(orderUuid)
      setDetail(res)
    } finally {
      setLoading(false)
    }
  }

  const handleBatchGenerate = async () => {
    const values = await batchForm.validateFields()
    const dto: FinishRollBatchDTO = {
      count: values.count,
      paperName: values.paperName,
      gramWeight: values.gramWeight,
      finishWidth: values.finishWidth,
      finishDiameter: values.finishDiameter,
      finishCoreDiameter: values.finishCoreDiameter,
      remark: values.remark,
    }
    setBatchSubmitting(true)
    try {
      const rollNos = await batchGenerateFinishRolls(orderUuid!, dto)
      message.success(`已生成 ${rollNos.length} 个卷号`)
      batchForm.resetFields()
      setBatchOpen(false)
      await loadDetail()
      onSuccess()
    } finally {
      setBatchSubmitting(false)
    }
  }

  const handleAppendSpare = async () => {
    const values = await spareForm.validateFields()
    const dto: SpareRollAppendDTO = { count: values.count }
    setSpareSubmitting(true)
    try {
      const rollNos = await appendSpareRolls(orderUuid!, dto)
      message.success(`已追加 ${rollNos.length} 个备用号`)
      spareForm.resetFields()
      setSpareOpen(false)
      await loadDetail()
      onSuccess()
    } finally {
      setSpareSubmitting(false)
    }
  }

  const handleBatchVoid = () => {
    Modal.confirm({
      title: '批量作废',
      content: `已勾选 ${selectedRowKeys.length} 个卷号，作废后不可恢复，是否继续？`,
      onOk: async () => {
        const dto: SpareRollBatchVoidDTO = { uuids: selectedRowKeys as string[] }
        await batchVoidFinishRolls(dto)
        message.success(`已作废 ${selectedRowKeys.length} 个卷号`)
        setSelectedRowKeys([])
        await loadDetail()
        onSuccess()
      },
    })
  }

  const handleVoidSingle = async (uuid: string) => {
    await voidFinishRoll(uuid)
    message.success('已作废')
    await loadDetail()
    onSuccess()
  }

  const finishRolls = detail?.finishRolls ?? []
  const filteredRolls = finishRolls.filter((r) => {
    if (filterStatus !== undefined && r.rollNoStatus !== filterStatus) return false
    if (filterSpare !== undefined && r.isSpare !== filterSpare) return false
    if (filterSource !== undefined && r.sourceType !== filterSource) return false
    return true
  })

  const isAllDirectShip =
    detail?.originalRolls?.every((r) => r.processMode === 3) ?? false
  const isSettled = detail?.order?.orderStatus === 5

  const columns: ColumnsType<FinishRoll> = [
    {
      title: '成品卷号',
      dataIndex: 'finishRollNo',
      width: 120,
      fixed: 'left',
      render: (v, r) => (
        <span style={r.rollNoStatus === 3 ? { textDecoration: 'line-through' } : {}}>
          {v}
        </span>
      ),
    },
    {
      title: '卷号状态',
      dataIndex: 'rollNoStatus',
      width: 100,
      render: (v) => {
        const s = ROLL_NO_STATUS[v]
        return s ? <Tag color={s.color}>{s.text}</Tag> : '-'
      },
    },
    {
      title: '类型',
      dataIndex: 'isSpare',
      width: 80,
      render: (v) => (v === 1 ? <Tag color="orange">备用</Tag> : '正式'),
    },
    {
      title: '来源',
      dataIndex: 'sourceType',
      width: 80,
      render: (v) => (v === 2 ? <Tag color="green">直发</Tag> : '加工'),
    },
    {
      title: '品名',
      dataIndex: 'paperName',
      width: 120,
      render: (v) => (
        <span style={v === '待定' ? { color: '#999' } : {}}>{v ?? '-'}</span>
      ),
    },
    {
      title: '克重',
      dataIndex: 'gramWeight',
      width: 80,
      render: (v) => (
        <span style={v === 0 ? { color: '#999' } : {}}>
          {v ? `${v}g/㎡` : '-'}
        </span>
      ),
    },
    {
      title: '门幅',
      dataIndex: 'finishWidth',
      width: 80,
      render: (v) => (
        <span style={v === 0 ? { color: '#999' } : {}}>{v ? `${v}mm` : '-'}</span>
      ),
    },
    {
      title: '预估(kg)',
      dataIndex: 'estimateWeight',
      width: 90,
      render: (v) => v ?? '-',
    },
    {
      title: '实际(kg)',
      dataIndex: 'actualWeight',
      width: 90,
      render: (v) => v ?? '-',
    },
    {
      title: '成品状态',
      dataIndex: 'finishStatus',
      width: 80,
      render: (v) => FINISH_STATUS[v] ?? '-',
    },
    {
      title: '操作',
      width: 80,
      fixed: 'right',
      render: (_, r) => {
        if (r.rollNoStatus !== 1) return '-'
        if (r.sourceType === 2) return '-'
        return (
          <Popconfirm
            title="作废后不可恢复，是否继续？"
            onConfirm={() => handleVoidSingle(r.uuid)}
          >
            <Button danger type="link" size="small">作废</Button>
          </Popconfirm>
        )
      },
    },
  ]

  const rowSelection = {
    selectedRowKeys,
    onChange: setSelectedRowKeys,
    getCheckboxProps: (record: FinishRoll) => ({
      disabled: record.rollNoStatus !== 1,
    }),
  }

  return (
    <>
      <Drawer
        title={`成品卷号管理${detail?.order?.orderNo ? `（${detail.order.orderNo}）` : ''}`}
        width={1200}
        open={open}
        onClose={onClose}
        destroyOnHidden
        className="mes-detail-drawer"
      >
        <Spin spinning={loading}>
          <div className="mes-drawer-content">
            {isSettled && (
              <Alert
                type="warning"
                showIcon
                message="已结算订单费用已锁定，不可修改成品卷号"
              />
            )}
            <div className="mes-drawer-toolbar">
              <div className="mes-drawer-toolbar__actions">
                <MesTooltip title={isAllDirectShip ? '直发卷在回录时自动产出，无需预生成' : undefined}>
                  <span>
                    <Button
                      type="primary"
                      onClick={() => setBatchOpen(true)}
                      disabled={isAllDirectShip || isSettled}
                    >
                      批量生成正式号
                    </Button>
                  </span>
                </MesTooltip>
                <Button onClick={() => setSpareOpen(true)} disabled={isSettled}>
                  追加备用号
                </Button>
                <Button
                  danger
                  onClick={handleBatchVoid}
                  disabled={selectedRowKeys.length === 0 || isSettled}
                >
                  批量作废（已勾选 {selectedRowKeys.length}）
                </Button>
              </div>
              <div className="mes-drawer-toolbar__filters">
              <Select
                placeholder="卷号状态"
                allowClear
                value={filterStatus}
                onChange={setFilterStatus}
                options={[
                  { value: 1, label: '预生成' },
                  { value: 3, label: '作废' },
                ]}
              />
              <Select
                placeholder="卷号类型"
                allowClear
                value={filterSpare}
                onChange={setFilterSpare}
                options={[
                  { value: 0, label: '正式' },
                  { value: 1, label: '备用' },
                ]}
              />
              <Select
                placeholder="来源类型"
                allowClear
                value={filterSource}
                onChange={setFilterSource}
                options={[
                  { value: 1, label: '加工产出' },
                  { value: 2, label: '原纸直发' },
                ]}
              />
              </div>
            </div>
            <div className="mes-drawer-table">
              <Table
                rowKey="uuid"
                size="small"
                columns={columns}
                dataSource={filteredRolls}
                rowSelection={rowSelection}
                pagination={false}
                scroll={{ x: 'max-content' }}
              />
            </div>
          </div>
        </Spin>
      </Drawer>

      <Modal
        title="批量生成正式卷号"
        open={batchOpen}
        onCancel={() => setBatchOpen(false)}
        onOk={handleBatchGenerate}
        confirmLoading={batchSubmitting}
        okText="生成"
        cancelText="取消"
        destroyOnHidden
      >
        <Form className="mes-modal-form" form={batchForm} layout="vertical">
          <div className="mes-form-grid">
            <Form.Item
              name="count"
              label="生成数量"
              rules={[
                { required: true, message: '必填' },
                { type: 'number', min: 1, max: 500, message: '1-500' },
              ]}
            >
              <InputNumber min={1} max={500} />
            </Form.Item>
            <Form.Item name="paperName" label="成品品名">
              <Input placeholder="可留空，回录时补齐实际品名" />
            </Form.Item>
            <Form.Item name="gramWeight" label="克重 g/㎡">
              <InputNumber min={1} placeholder="可留空，回录时补齐" />
            </Form.Item>
            <Form.Item name="finishWidth" label="门幅 mm">
              <InputNumber min={1} placeholder="可留空，回录时补齐" />
            </Form.Item>
            <Form.Item name="finishDiameter" label="直径（英寸）">
              <InputNumber min={1} />
            </Form.Item>
            <Form.Item name="finishCoreDiameter" label="纸芯直径（英寸）">
              <InputNumber min={1} />
            </Form.Item>
            <Form.Item className="mes-form-grid__full" name="remark" label="备注">
              <Input.TextArea rows={2} />
            </Form.Item>
          </div>
        </Form>
      </Modal>

      <Modal
        title="追加备用卷号"
        open={spareOpen}
        onCancel={() => setSpareOpen(false)}
        onOk={handleAppendSpare}
        confirmLoading={spareSubmitting}
        okText="追加"
        cancelText="取消"
        destroyOnHidden
      >
        <Form className="mes-modal-form" form={spareForm} layout="vertical">
          <Form.Item
            name="count"
            label="追加数量"
            rules={[
              { required: true, message: '必填' },
              { type: 'number', min: 1, max: 500, message: '1-500' },
            ]}
          >
            <InputNumber min={1} max={500} />
          </Form.Item>
        </Form>
      </Modal>
    </>
  )
}
