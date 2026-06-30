import { useRef, useState } from 'react'
import type { ReactNode } from 'react'
import { Button, Popconfirm, message } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { ProTable } from '@ant-design/pro-components'
import type { ActionType, ProColumns } from '@ant-design/pro-components'
import { pageConfigItems } from '../../api/systemConfig'
import { mesTablePagination } from '../../components/biz/MesPaginationBar'
import TooltipText from '../../components/biz/TooltipText'
import { useResizableTableColumns } from '../../components/useResizableTableColumns'
import type { ConfigItem, ConfigItemSaveDTO, ConfigStatus } from '../../types/systemConfig'
import { useCreateConfigItem, useDeleteConfigItem, useUpdateConfigItem } from '../../features/systemConfig/hooks/useSystemConfigMutations'
import { useTableColumnsState } from '../../hooks/useTableColumnsState'
import { builtInTag, statusOptions, statusTag, valueTypeTag } from './systemConfigDisplay'
import { ConfigItemModal } from './SystemConfigModal'

export default function ConfigItemPanel() {
  const actionRef = useRef<ActionType>(null)
  const [editing, setEditing] = useState<ConfigItem>()
  const [modalOpen, setModalOpen] = useState(false)
  const columnsState = useTableColumnsState('table-columns-system-config')
  const { mutateAsync: createItem, isPending: isCreating } = useCreateConfigItem()
  const { mutateAsync: updateItem, isPending: isUpdating } = useUpdateConfigItem()
  const { mutateAsync: deleteItem } = useDeleteConfigItem()
  const columns = useConfigColumns({ deleteItem, onEdit: openEdit })
  const resizable = useResizableTableColumns<ConfigItem, ProColumns<ConfigItem>>(columns, 'system-config')

  async function submit(values: ConfigItemSaveDTO) {
    if (editing) {
      await updateItem({ uuid: editing.uuid, data: values })
      message.success('系统参数已保存')
    } else {
      await createItem(values)
      message.success('系统参数已新增')
    }
    setModalOpen(false)
    setEditing(undefined)
    actionRef.current?.reload()
  }

  function openEdit(record: ConfigItem) {
    setEditing(record)
    setModalOpen(true)
  }

  return (
    <>
      <ProTable<ConfigItem>
        className="mes-pro-table-page system-config-table"
        rowKey="uuid"
        actionRef={actionRef}
        columns={resizable.columns}
        columnsState={columnsState}
        components={resizable.components}
        headerTitle="系统参数"
        toolBarRender={() => [
          <Button key="add" type="primary" icon={<PlusOutlined />} onClick={() => setModalOpen(true)}>
            新增参数
          </Button>,
        ]}
        request={async (params) => {
          const res = await pageConfigItems({
            configGroup: params.configGroup as string | undefined,
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
        options={{ density: true, reload: true, setting: true }}
      />
      <ConfigItemModal
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

function useConfigColumns(options: { deleteItem: (uuid: string) => Promise<void>; onEdit: (record: ConfigItem) => void }) {
  return [
    { title: '参数分组', dataIndex: 'configGroup', width: 130, render: textCell },
    { title: '关键字', dataIndex: 'keyword', hideInTable: true },
    { title: '参数键', dataIndex: 'configKey', width: 220, search: false, render: textCell },
    { title: '参数名称', dataIndex: 'configName', width: 180, search: false, render: textCell },
    { title: '参数值', dataIndex: 'configValue', width: 160, search: false, render: textCell },
    { title: '类型', dataIndex: 'valueType', width: 100, search: false, render: (_, r) => valueTypeTag(r.valueType) },
    { title: '单位', dataIndex: 'unit', width: 80, search: false, render: (_, r) => r.unit || '-' },
    { title: '排序', dataIndex: 'sortNo', width: 80, search: false },
    { title: '状态', dataIndex: 'status', width: 100, valueType: 'select', valueEnum: statusValueEnum(), render: (_, r) => statusTag(r.status) },
    { title: '来源', dataIndex: 'builtIn', width: 100, search: false, render: (_, r) => builtInTag(r.builtIn) },
    { title: '备注', dataIndex: 'remark', width: 260, search: false, render: textCell },
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
            title="确认删除该系统参数？"
            description={record.builtIn === 1 ? '内置系统参数不能删除，可以改为停用或修改参数值。' : '删除后系统不再读取该参数。'}
            onConfirm={async () => {
              await options.deleteItem(record.uuid)
              message.success('系统参数已删除')
            }}
          >
            <Button danger type="link" size="small" disabled={record.builtIn === 1}>
              删除
            </Button>
          </Popconfirm>
        </div>
      ),
    },
  ] satisfies ProColumns<ConfigItem>[]
}

function statusValueEnum() {
  return Object.fromEntries(statusOptions.map((item) => [item.value, { text: item.label }]))
}

function textCell(value?: ReactNode) {
  return <TooltipText value={value} />
}
