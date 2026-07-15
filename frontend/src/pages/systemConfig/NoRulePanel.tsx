import { useRef, useState } from 'react'
import type { ReactNode } from 'react'
import { Button, Form, Input, InputNumber, Modal, Select, Tag, message } from 'antd'
import { ProTable } from '@ant-design/pro-components'
import type { ActionType, ProColumns } from '@ant-design/pro-components'
import { pageNoRules, previewNoRule } from '../../api/systemConfig'
import { mesTablePagination } from '../../components/biz/mesPaginationUtils'
import { mesProTableOptions } from '../../components/biz/mesProTableOptions'
import { renderCompatibleTableOptions } from '../../components/biz/tableToolbarOptionsRender'
import TooltipText from '../../components/biz/TooltipText'
import { useResizableTableColumns } from '../../components/useResizableTableColumns'
import { useUpdateNoRule } from '../../features/systemConfig/hooks/useSystemConfigMutations'
import { useTableColumnsState } from '../../hooks/useTableColumnsState'
import type { ConfigStatus, NoRule, NoRuleSaveDTO } from '../../types/systemConfig'
import { statusOptions, statusTag } from './systemConfigDisplay'

interface NoRulePanelProps {
  onDirtyChange?: (dirty: boolean) => void
}

export default function NoRulePanel({ onDirtyChange }: NoRulePanelProps) {
  const actionRef = useRef<ActionType>(null)
  const [editing, setEditing] = useState<NoRule>()
  const [dirty, setDirty] = useState(false)
  const columnsState = useTableColumnsState('table-columns-system-no-rule')
  const { mutateAsync: updateRule, isPending } = useUpdateNoRule()
  const columns = useNoRuleColumns({ onEdit: openEdit })
  const resizable = useResizableTableColumns<NoRule, ProColumns<NoRule>>(columns, 'system-no-rule')

  async function submit(values: NoRuleSaveDTO) {
    if (!editing) return
    await updateRule({ uuid: editing.uuid, data: values })
    message.success('单号规则已保存')
    setDirty(false)
    onDirtyChange?.(false)
    setEditing(undefined)
    actionRef.current?.reload()
  }

  function openEdit(record: NoRule) {
    setDirty(false)
    onDirtyChange?.(false)
    setEditing(record)
  }

  function closeModal() {
    if (!dirty) {
      finishClose()
      return
    }
    Modal.confirm({
      title: '放弃未保存修改？',
      content: '当前单号规则尚未保存，关闭后修改会丢失。',
      okText: '放弃修改',
      cancelText: '继续编辑',
      onOk: finishClose,
    })
  }

  function finishClose() {
    setDirty(false)
    onDirtyChange?.(false)
    setEditing(undefined)
  }

  return (
    <>
      <ProTable<NoRule>
        className="mes-pro-table-page system-config-table"
        rowKey="uuid"
        actionRef={actionRef}
        columns={resizable.columns}
        columnsState={columnsState}
        components={resizable.components}
        headerTitle="单号规则"
        request={async (params) => {
          const res = await pageNoRules({
            bizType: params.bizType as string | undefined,
            current: params.current,
            keyword: params.keyword as string | undefined,
            size: params.pageSize,
            status: params.status as ConfigStatus | undefined,
          })
          return { data: res.records ?? [], total: res.total ?? 0, success: true }
        }}
        bordered
        pagination={mesTablePagination(20)}
        search={{ defaultCollapsed: false, labelWidth: 'auto' }}
        scroll={{ x: resizable.scrollX, y: '100%' }}
        options={mesProTableOptions()}
        optionsRender={renderCompatibleTableOptions}
        tableLayout="fixed"
      />
      <NoRuleModal
        item={editing}
        open={Boolean(editing)}
        submitting={isPending}
        onCancel={closeModal}
        onDirtyChange={(nextDirty) => {
          setDirty(nextDirty)
          onDirtyChange?.(nextDirty)
        }}
        onSubmit={submit}
      />
    </>
  )
}

function useNoRuleColumns(options: { onEdit: (record: NoRule) => void }) {
  return [
    { title: '业务类型', dataIndex: 'bizType', width: 150, render: (_, r) => bizTypeTag(r.bizType) },
    { title: '关键字', dataIndex: 'keyword', hideInTable: true },
    { title: '规则名称', dataIndex: 'ruleName', width: 150, search: false, render: textCell },
    { title: '前缀', dataIndex: 'prefix', width: 100, search: false, render: textCell },
    { title: '格式', dataIndex: 'patternType', width: 150, search: false, render: (_, r) => patternText(r.patternType) },
    { title: '日期格式', dataIndex: 'datePattern', width: 110, search: false, render: textCell },
    { title: '流水位数', dataIndex: 'serialLength', width: 100, search: false },
    { title: '重置周期', dataIndex: 'resetCycle', width: 110, search: false, render: (_, r) => resetText(r.resetCycle) },
    { title: '状态', dataIndex: 'status', width: 100, valueType: 'select', valueEnum: statusValueEnum(), render: (_, r) => statusTag(r.status) },
    {
      title: '下一号预览',
      dataIndex: 'preview',
      width: 190,
      search: false,
      render: (_, record) => <PreviewButton bizType={record.bizType} />,
    },
    { title: '备注', dataIndex: 'remark', width: 240, search: false, render: textCell },
    {
      title: '操作',
      key: 'actions',
      valueType: 'option',
      width: 90,
      render: (_, record) => (
        <div className="mes-table-actions">
          <Button type="link" size="small" onClick={() => options.onEdit(record)}>
            编辑
          </Button>
        </div>
      ),
    },
  ] satisfies ProColumns<NoRule>[]
}

function PreviewButton({ bizType }: { bizType: string }) {
  const [loading, setLoading] = useState(false)
  const [text, setText] = useState<string>()

  async function loadPreview() {
    setLoading(true)
    try {
      const res = await previewNoRule(bizType)
      setText(res.exampleNo)
    } finally {
      setLoading(false)
    }
  }

  return (
    <Button type="link" size="small" loading={loading} onClick={loadPreview}>
      {text || '查看下一号'}
    </Button>
  )
}

function NoRuleModal({ item, onCancel, onDirtyChange, onSubmit, open, submitting }: {
  item?: NoRule
  open: boolean
  submitting: boolean
  onCancel: () => void
  onDirtyChange?: (dirty: boolean) => void
  onSubmit: (values: NoRuleSaveDTO) => Promise<void>
}) {
  const [form] = Form.useForm<NoRuleSaveDTO>()
  return (
    <Modal
      title="编辑单号规则"
      open={open}
      width={720}
      destroyOnHidden
      confirmLoading={submitting}
      onCancel={onCancel}
      onOk={() => form.submit()}
    >
      <Form className="mes-modal-form" form={form} initialValues={item ? toValues(item) : undefined} layout="vertical" onFieldsChange={() => onDirtyChange?.(form.isFieldsTouched())} onFinish={onSubmit}>
        <div className="mes-form-grid">
          <Form.Item name="bizType" label="业务类型" rules={[{ required: true, message: '业务类型不能为空' }]}>
            <Input disabled />
          </Form.Item>
          <Form.Item name="ruleName" label="规则名称" rules={[{ required: true, message: '请输入规则名称' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="prefix" label="前缀" rules={[{ required: true, message: '请输入前缀' }]}>
            <Input placeholder="如 JG、CK、JS、A" />
          </Form.Item>
          <Form.Item name="patternType" label="格式" rules={[{ required: true, message: '请选择格式' }]}>
            <Select options={patternOptions} />
          </Form.Item>
          <Form.Item name="datePattern" label="日期格式">
            <Select options={datePatternOptions} />
          </Form.Item>
          <Form.Item name="serialLength" label="流水位数" rules={[{ required: true, message: '请输入流水位数' }]}>
            <InputNumber min={3} max={10} />
          </Form.Item>
          <Form.Item name="resetCycle" label="重置周期" rules={[{ required: true, message: '请选择重置周期' }]}>
            <Select options={resetOptions} />
          </Form.Item>
          <Form.Item name="status" label="状态" rules={[{ required: true, message: '请选择状态' }]}>
            <Select options={statusOptions} />
          </Form.Item>
          <Form.Item className="mes-form-grid__full" name="remark" label="备注">
            <Input.TextArea rows={3} placeholder="说明该单号规则适用的业务场景" />
          </Form.Item>
        </div>
      </Form>
    </Modal>
  )
}

const patternOptions = [
  { label: '前缀 + 日期 + 序号', value: 1 },
  { label: '前缀 + 序号', value: 2 },
]

const datePatternOptions = [
  { label: 'yyyyMMdd', value: 'yyyyMMdd' },
  { label: 'yyyyMM', value: 'yyyyMM' },
  { label: 'yyyy', value: 'yyyy' },
]

const resetOptions = [
  { label: '不重置', value: 0 },
  { label: '按日', value: 1 },
  { label: '按月', value: 2 },
  { label: '按年', value: 3 },
]

function toValues(item: NoRule): NoRuleSaveDTO {
  return {
    bizType: item.bizType,
    datePattern: item.datePattern || 'yyyyMMdd',
    patternType: item.patternType,
    prefix: item.prefix,
    remark: item.remark,
    resetCycle: item.resetCycle,
    ruleName: item.ruleName,
    serialLength: item.serialLength,
    status: item.status,
  }
}

function bizTypeTag(value: string) {
  const text = bizTypeText(value)
  return <Tag color="blue">{text}</Tag>
}

function bizTypeText(value: string) {
  const map: Record<string, string> = {
    customer: '客户编码',
    delivery_order: '出库单号',
    finish_roll: '成品卷号',
    machine: '机台编码',
    paper: '纸张编码',
    process_order: '加工单号',
    settle_order: '结算单号',
    warehouse: '仓库编码',
  }
  return map[value] || value
}

function patternText(value: number) {
  return value === 2 ? '前缀 + 序号' : '前缀 + 日期 + 序号'
}

function resetText(value: number) {
  const map: Record<number, string> = { 0: '不重置', 1: '按日', 2: '按月', 3: '按年' }
  return map[value] || '-'
}

function statusValueEnum() {
  return Object.fromEntries(statusOptions.map((item) => [item.value, { text: item.label }]))
}

function textCell(value?: ReactNode) {
  return <TooltipText value={value} />
}
