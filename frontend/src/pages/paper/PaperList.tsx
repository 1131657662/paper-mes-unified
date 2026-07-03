import { useRef, useState } from 'react'
import type { ReactNode } from 'react'
import { Button, Popconfirm, Tag, message } from 'antd'
import { DownloadOutlined, PlusOutlined } from '@ant-design/icons'
import { ProTable } from '@ant-design/pro-components'
import type { ActionType, ProColumns } from '@ant-design/pro-components'
import { useNavigate } from 'react-router-dom'
import { pagePapers, deletePaper } from '../../api/paper'
import { mesProTableOptions } from '../../components/biz/mesProTableOptions'
import TooltipText from '../../components/biz/TooltipText'
import { mesTablePagination } from '../../components/biz/mesPaginationUtils'
import { useResizableTableColumns } from '../../components/useResizableTableColumns'
import { PERMISSIONS } from '../../constants/permissions'
import { useTableColumnsState } from '../../hooks/useTableColumnsState'
import { useHasPermission } from '../../stores/authStore'
import type { Paper, PaperQuery } from '../../types/paper'
import { datedCsvFilename, exportRowsToCsv } from '../../utils/exportCsv'

export default function PaperList() {
  const actionRef = useRef<ActionType>(null)
  const latestQueryRef = useRef<PaperQuery>({})
  const navigate = useNavigate()
  const canManageBase = useHasPermission(PERMISSIONS.baseManage)
  const columnsState = useTableColumnsState('table-columns-papers')
  const [exporting, setExporting] = useState(false)

  const handleDelete = async (record: Paper) => {
    await deletePaper(record.uuid)
    message.success('删除成功')
    actionRef.current?.reload()
  }

  const handleExport = async () => {
    setExporting(true)
    try {
      const res = await pagePapers({ ...latestQueryRef.current, current: 1, size: 10000 })
      const result = exportRowsToCsv({
        columns: paperExportColumns(),
        filename: datedCsvFilename('纸张档案'),
        rows: res.records ?? [],
      })
      message.success(`已导出 ${result.filename}`)
    } finally {
      setExporting(false)
    }
  }

  const columns: ProColumns<Paper>[] = [
    { title: '纸张编码', dataIndex: 'paperCode', width: 140 },
    { title: '纸张品名', dataIndex: 'paperName', width: 220, render: textCell },
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
      key: 'actions',
      valueType: 'option',
      width: 140,
      render: (_, record) => (
        <div className="mes-table-actions">
          <Button type="link" size="small" onClick={() => navigate(`/papers/${record.uuid}`)}>
            详情
          </Button>
          {canManageBase && (
            <Button type="link" size="small" onClick={() => navigate(`/papers/${record.uuid}/edit`)}>
              编辑
            </Button>
          )}
          {canManageBase && (
            <Popconfirm
              title="确认删除该纸张？"
              description="删除后录入原纸不能快速引用该纸张，历史单据不受影响。"
              onConfirm={() => handleDelete(record)}
            >
              <Button danger type="link" size="small">删除</Button>
            </Popconfirm>
          )}
        </div>
      ),
    },
  ]
  const resizable = useResizableTableColumns<Paper, ProColumns<Paper>>(columns, 'papers')

  return (
    <ProTable<Paper>
      className="mes-pro-table-page"
      rowKey="uuid"
      actionRef={actionRef}
      columns={resizable.columns}
      columnsState={columnsState}
      components={resizable.components}
      headerTitle="纸张档案"
      toolBarRender={() => [
        <Button key="export" icon={<DownloadOutlined />} loading={exporting} onClick={handleExport}>
          导出
        </Button>,
        canManageBase && (
          <Button key="add" type="primary" icon={<PlusOutlined />} onClick={() => navigate('/papers/create')}>
            新增纸张
          </Button>
        ),
      ].filter(Boolean)}
      request={async (params) => {
        const query = {
          current: params.current,
          size: params.pageSize,
          keyword: params.paperCode || params.paperName,
        }
        latestQueryRef.current = query
        const res = await pagePapers(query)
        return { data: res.records ?? [], total: res.total ?? 0, success: true }
      }}
      bordered
      pagination={mesTablePagination(10)}
      search={{ labelWidth: 'auto' }}
      scroll={{ x: resizable.scrollX, y: '100%' }}
      tableLayout="fixed"
      options={mesProTableOptions()}
    />
  )
}

function textCell(value?: ReactNode) {
  return <TooltipText value={value} />
}

function paperExportColumns() {
  return [
    { header: '纸张编码', value: (row: Paper) => row.paperCode },
    { header: '纸张品名', value: (row: Paper) => row.paperName },
    { header: '克重', value: (row: Paper) => row.gramWeight },
    { header: '类型', value: (row: Paper) => row.paperType },
    { header: '备注', value: (row: Paper) => row.remark },
    { header: '创建时间', value: (row: Paper) => row.createTime },
  ]
}
