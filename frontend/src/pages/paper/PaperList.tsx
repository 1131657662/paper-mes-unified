import { useRef } from 'react'
import type { ReactNode } from 'react'
import { Button, Popconfirm, Tag, message } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { ProTable } from '@ant-design/pro-components'
import type { ActionType, ProColumns } from '@ant-design/pro-components'
import { useNavigate } from 'react-router-dom'
import { pagePapers, deletePaper } from '../../api/paper'
import TooltipText from '../../components/biz/TooltipText'
import { mesTablePagination } from '../../components/biz/MesPaginationBar'
import { MES_PRO_TABLE_SCROLL } from '../../components/biz/tableScroll'
import { PERMISSIONS } from '../../constants/permissions'
import { useHasPermission } from '../../stores/authStore'
import type { Paper } from '../../types/paper'

export default function PaperList() {
  const actionRef = useRef<ActionType>(null)
  const navigate = useNavigate()
  const canManageBase = useHasPermission(PERMISSIONS.baseManage)

  const handleDelete = async (record: Paper) => {
    await deletePaper(record.uuid)
    message.success('删除成功')
    actionRef.current?.reload()
  }

  const columns: ProColumns<Paper>[] = [
    { title: '纸张编码', dataIndex: 'paperCode', width: 140 },
    { title: '纸张品名', dataIndex: 'paperName', render: textCell },
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

  return (
    <ProTable<Paper>
      className="mes-pro-table-page"
      rowKey="uuid"
      actionRef={actionRef}
      columns={columns}
      headerTitle="纸张档案"
      toolBarRender={() => canManageBase ? [
        <Button key="add" type="primary" icon={<PlusOutlined />} onClick={() => navigate('/papers/create')}>
          新增纸张
        </Button>,
      ] : []}
      request={async (params) => {
        const res = await pagePapers({
          current: params.current,
          size: params.pageSize,
          keyword: params.paperCode || params.paperName,
        })
        return { data: res.records ?? [], total: res.total ?? 0, success: true }
      }}
      bordered
      pagination={mesTablePagination(10)}
      search={{ labelWidth: 'auto' }}
      scroll={MES_PRO_TABLE_SCROLL}
    />
  )
}

function textCell(value?: ReactNode) {
  return <TooltipText value={value} />
}
