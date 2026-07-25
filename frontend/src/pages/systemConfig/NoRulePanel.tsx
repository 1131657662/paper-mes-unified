import { useRef, useState } from 'react'
import type { ReactNode } from 'react'
import { Button, Modal, Tag, message } from 'antd'
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
import NoRuleModal from './NoRuleModal'
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
