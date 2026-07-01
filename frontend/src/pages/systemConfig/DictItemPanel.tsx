import { useRef, useState } from 'react'
import type { ReactNode } from 'react'
import { Button, Popconfirm, message } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { ProTable } from '@ant-design/pro-components'
import type { ActionType, ProColumns } from '@ant-design/pro-components'
import { pageDictItems } from '../../api/systemConfig'
import { mesTablePagination } from '../../components/biz/mesPaginationUtils'
import { mesProTableOptions } from '../../components/biz/mesProTableOptions'
import TooltipText from '../../components/biz/TooltipText'
import { useResizableTableColumns } from '../../components/useResizableTableColumns'
import type { ConfigStatus, DictItem, DictItemSaveDTO } from '../../types/systemConfig'
import { useCreateDictItem, useDeleteDictItem, useUpdateDictItem } from '../../features/systemConfig/hooks/useSystemConfigMutations'
import { useTableColumnsState } from '../../hooks/useTableColumnsState'
import { builtInTag, statusOptions, statusTag } from './systemConfigDisplay'
import { DictItemModal } from './SystemConfigModal'

export default function DictItemPanel() {
  const actionRef = useRef<ActionType>(null)
  const [editing, setEditing] = useState<DictItem>()
  const [modalOpen, setModalOpen] = useState(false)
  const columnsState = useTableColumnsState('table-columns-system-dict')
  const { mutateAsync: createItem, isPending: isCreating } = useCreateDictItem()
  const { mutateAsync: updateItem, isPending: isUpdating } = useUpdateDictItem()
  const { mutateAsync: deleteItem } = useDeleteDictItem()
  const columns = useDictColumns({ deleteItem, onEdit: openEdit })
  const resizable = useResizableTableColumns<DictItem, ProColumns<DictItem>>(columns, 'system-dict')

  async function submit(values: DictItemSaveDTO) {
    if (editing) {
      await updateItem({ uuid: editing.uuid, data: values })
      message.success('字典项已保存')
    } else {
      await createItem(values)
      message.success('字典项已新增')
    }
    setModalOpen(false)
    setEditing(undefined)
    actionRef.current?.reload()
  }

  function openEdit(record: DictItem) {
    setEditing(record)
    setModalOpen(true)
  }

  return (
    <>
      <ProTable<DictItem>
        className="mes-pro-table-page system-config-table"
        rowKey="uuid"
        actionRef={actionRef}
        columns={resizable.columns}
        columnsState={columnsState}
        components={resizable.components}
        headerTitle="数据字典"
        toolBarRender={() => [
          <Button key="add" type="primary" icon={<PlusOutlined />} onClick={() => setModalOpen(true)}>
            新增字典项
          </Button>,
        ]}
        request={async (params) => {
          const res = await pageDictItems({
            current: params.current,
            dictType: params.dictType as string | undefined,
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
      />
      <DictItemModal
        item={editing}
        open={modalOpen}
        submitting={isCreating || isUpdating}
        onCancel={() => {
          setModalOpen(false)
          setEditing(undefined)
        }}
        onSubmit={submit}
      />
    </>
  )
}

function useDictColumns(options: { deleteItem: (uuid: string) => Promise<void>; onEdit: (record: DictItem) => void }) {
  return [
    { title: '字典分类', dataIndex: 'dictType', width: 150, render: textCell },
    { title: '分类名称', dataIndex: 'dictName', width: 150, search: false, render: textCell },
    { title: '关键字', dataIndex: 'keyword', hideInTable: true },
    { title: '字典编码', dataIndex: 'itemCode', width: 150, search: false, render: textCell },
    { title: '字典名称', dataIndex: 'itemName', width: 160, search: false, render: textCell },
    { title: '枚举值', dataIndex: 'itemValue', width: 90, search: false, render: (_, r) => r.itemValue ?? '-' },
    { title: '排序', dataIndex: 'sortNo', width: 80, search: false },
    { title: '状态', dataIndex: 'status', width: 100, valueType: 'select', valueEnum: statusValueEnum(), render: (_, r) => statusTag(r.status) },
    { title: '来源', dataIndex: 'builtIn', width: 100, search: false, render: (_, r) => builtInTag(r.builtIn) },
    { title: '备注', dataIndex: 'remark', width: 240, search: false, render: textCell },
    {
      title: '操作',
      key: 'actions',
      valueType: 'option',
      width: 130,
      render: (_, record) => (
        <div className="mes-table-actions">
          <Button type="link" size="small" onClick={() => options.onEdit(record)}>
            编辑
          </Button>
          <Popconfirm
            title="确认删除该字典项？"
            description={record.builtIn === 1 ? '内置字典项不能删除，可以改为停用。' : '删除后业务下拉不会再使用该项。'}
            onConfirm={async () => {
              await options.deleteItem(record.uuid)
              message.success('字典项已删除')
            }}
          >
            <Button danger type="link" size="small" disabled={record.builtIn === 1}>
              删除
            </Button>
          </Popconfirm>
        </div>
      ),
    },
  ] satisfies ProColumns<DictItem>[]
}

function statusValueEnum() {
  return Object.fromEntries(statusOptions.map((item) => [item.value, { text: item.label }]))
}

function textCell(value?: ReactNode) {
  return <TooltipText value={value} />
}
