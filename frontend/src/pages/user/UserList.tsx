import { useRef, useState } from 'react'
import type { ReactNode } from 'react'
import { Button, Popconfirm, Space, Tag, message } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { ProTable } from '@ant-design/pro-components'
import type { ActionType, ProColumns } from '@ant-design/pro-components'
import dayjs from 'dayjs'
import { useNavigate } from 'react-router-dom'
import { pageUsers } from '../../api/user'
import TooltipText from '../../components/biz/TooltipText'
import { mesTablePagination } from '../../components/biz/MesPaginationBar'
import { MES_PRO_TABLE_SCROLL } from '../../components/biz/tableScroll'
import { useAuthUser } from '../../stores/authStore'
import type { SystemUser, UserRoleCode, UserStatus } from '../../types/user'
import { useResetUserPassword, useUpdateUserStatus } from '../../features/user/hooks/useUserMutations'
import { getRoleModuleNames, getRoleProfile } from '../../constants/permissionMeta'
import { roleTag, roleText, statusTag } from './userDisplay'
import UserPasswordModal from './UserPasswordModal'
import '../documentModule.css'
import './UserProfile.css'

export default function UserList() {
  const actionRef = useRef<ActionType>(null)
  const navigate = useNavigate()
  const currentUser = useAuthUser()
  const [passwordUser, setPasswordUser] = useState<SystemUser>()
  const { mutateAsync: changeStatus, isPending: isChangingStatus } = useUpdateUserStatus()
  const { mutateAsync: resetPassword, isPending: isResettingPassword } = useResetUserPassword()

  const handleStatus = async (record: SystemUser, status: UserStatus) => {
    await changeStatus({ uuid: record.uuid, data: { status } })
    message.success(status === 1 ? '账号已启用' : '账号已停用')
    actionRef.current?.reload()
  }

  const columns: ProColumns<SystemUser>[] = [
    { title: '登录账号', dataIndex: 'username', width: 150, render: textCell },
    { title: '姓名', dataIndex: 'realName', width: 150, render: textCell },
    {
      title: '角色',
      dataIndex: 'roleCode',
      width: 120,
      valueType: 'select',
      valueEnum: {
        admin: { text: '管理员' },
        operator: { text: '录单员' },
        finance: { text: '财务' },
        warehouse: { text: '仓库' },
      },
      render: (_, record) => <RoleCell roleCode={record.roleCode} />,
    },
    { title: '权限范围', dataIndex: 'roleCode', width: 260, search: false, render: (_, record) => <RoleScope roleCode={record.roleCode} /> },
    {
      title: '状态',
      dataIndex: 'status',
      width: 110,
      valueType: 'select',
      valueEnum: {
        1: { text: '启用' },
        0: { text: '停用' },
      },
      render: (_, record) => statusTag(record.status),
    },
    { title: '最近登录', dataIndex: 'lastLoginTime', width: 180, search: false, render: (_, r) => dateCell(r.lastLoginTime) },
    { title: '创建时间', dataIndex: 'createTime', width: 180, search: false, valueType: 'dateTime' },
    { title: '备注', dataIndex: 'remark', width: 220, search: false, render: textCell },
    {
      title: '操作',
      valueType: 'option',
      width: 240,
      render: (_, record) => (
        <div className="mes-table-actions">
          <Button type="link" size="small" onClick={() => navigate(`/users/${record.uuid}`)}>
            详情
          </Button>
          <Button type="link" size="small" onClick={() => navigate(`/users/${record.uuid}/edit`)}>
            编辑
          </Button>
          <Button type="link" size="small" onClick={() => setPasswordUser(record)}>
            重置密码
          </Button>
          {record.status === 1 ? (
            <Popconfirm
              title="确认停用该账号？"
              description="停用后该用户不能继续登录系统，历史单据和操作记录不受影响。"
              onConfirm={() => handleStatus(record, 0)}
            >
              <Button
                danger
                type="link"
                size="small"
                loading={isChangingStatus}
                disabled={record.uuid === currentUser?.uuid}
              >
                停用
              </Button>
            </Popconfirm>
          ) : (
            <Button type="link" size="small" onClick={() => handleStatus(record, 1)} loading={isChangingStatus}>
              启用
            </Button>
          )}
        </div>
      ),
    },
  ]

  return (
    <>
      <ProTable<SystemUser>
        className="mes-pro-table-page"
        rowKey="uuid"
        actionRef={actionRef}
        columns={columns}
        headerTitle="用户权限"
        toolBarRender={() => [
          <Button key="add" type="primary" icon={<PlusOutlined />} onClick={() => navigate('/users/create')}>
            新增用户
          </Button>,
        ]}
        request={async (params) => {
          const res = await pageUsers({
            current: params.current,
            size: params.pageSize,
            keyword: params.username || params.realName,
            roleCode: params.roleCode as UserRoleCode | undefined,
            status: params.status as UserStatus | undefined,
          })
          return { data: res.records ?? [], total: res.total ?? 0, success: true }
        }}
        bordered
        pagination={mesTablePagination(10)}
        search={{ labelWidth: 'auto' }}
        scroll={MES_PRO_TABLE_SCROLL}
      />
      <UserPasswordModal
        open={!!passwordUser}
        submitting={isResettingPassword}
        userName={passwordUser ? `${passwordUser.realName}（${passwordUser.username}）` : undefined}
        onCancel={() => setPasswordUser(undefined)}
        onSubmit={async (values) => {
          if (!passwordUser) return
          await resetPassword({ uuid: passwordUser.uuid, data: values })
          message.success('密码已重置')
          setPasswordUser(undefined)
        }}
      />
    </>
  )
}

function textCell(value?: ReactNode) {
  return <TooltipText value={value} />
}

function dateCell(value?: string) {
  return <span>{formatDate(value)}</span>
}

function formatDate(value?: string) {
  if (!value) return '-'
  const date = dayjs(value)
  return date.isValid() ? date.format('YYYY-MM-DD HH:mm:ss') : value
}

export function UserRoleSummary({ roleCode }: { roleCode?: string }) {
  return (
    <Space size={6}>
      {roleTag(roleCode)}
      <Tag className="mes-data-tag">{roleText(roleCode)}</Tag>
    </Space>
  )
}

function RoleCell({ roleCode }: { roleCode?: UserRoleCode }) {
  const profile = getRoleProfile(roleCode)
  return (
    <div className="user-role-cell">
      {roleTag(roleCode)}
      <span>{profile?.summary ?? roleText(roleCode)}</span>
    </div>
  )
}

function RoleScope({ roleCode }: { roleCode?: UserRoleCode }) {
  const names = getRoleModuleNames(roleCode)
  if (!names.length) return '-'
  return (
    <div className="user-role-scope">
      {names.map((name) => <Tag key={name}>{name}</Tag>)}
    </div>
  )
}
