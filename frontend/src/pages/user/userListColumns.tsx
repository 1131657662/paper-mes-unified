import { Button, Popconfirm } from 'antd'
import type { ProColumns } from '@ant-design/pro-components'
import type { SystemUser, UserStatus } from '../../types/user'
import { statusTag, userRoleValueEnum } from './userDisplay'
import { UserRoleCell, UserRoleScope } from './UserListDisplay'
import { userDateCell, userTextCell } from './userListCellRenderers'

interface UserListColumnOptions {
  currentUserUuid?: string
  isChangingStatus: boolean
  onDetail: (record: SystemUser) => void
  onEdit: (record: SystemUser) => void
  onPasswordReset: (record: SystemUser) => void
  onStatusChange: (record: SystemUser, status: UserStatus) => void
}

const BASE_COLUMNS: ProColumns<SystemUser>[] = [
  {
    title: '登录账号',
    dataIndex: 'username',
    width: 150,
    render: userTextCell,
  },
  { title: '姓名', dataIndex: 'realName', width: 150, render: userTextCell },
  {
    title: '角色',
    dataIndex: 'roleCode',
    key: 'role',
    width: 120,
    valueType: 'select',
    valueEnum: userRoleValueEnum,
    render: (_, record) => <UserRoleCell roleCode={record.roleCode} />,
  },
  {
    title: '权限范围',
    dataIndex: 'roleCode',
    key: 'roleScope',
    width: 260,
    search: false,
    render: (_, record) => <UserRoleScope roleCode={record.roleCode} />,
  },
  {
    title: '状态',
    dataIndex: 'status',
    width: 110,
    valueType: 'select',
    valueEnum: { 1: { text: '启用' }, 0: { text: '停用' } },
    render: (_, record) => statusTag(record.status),
  },
  {
    title: '最近登录',
    dataIndex: 'lastLoginTime',
    width: 180,
    search: false,
    render: (_, record) => userDateCell(record.lastLoginTime),
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 180,
    search: false,
    valueType: 'dateTime',
  },
  {
    title: '备注',
    dataIndex: 'remark',
    width: 220,
    search: false,
    render: userTextCell,
  },
]

export function createUserListColumns(
  options: UserListColumnOptions,
): ProColumns<SystemUser>[] {
  return [...BASE_COLUMNS, actionColumn(options)]
}

function actionColumn(options: UserListColumnOptions): ProColumns<SystemUser> {
  return {
    title: '操作',
    key: 'actions',
    valueType: 'option',
    width: 240,
    render: (_, record) => renderRowActions(record, options),
  }
}

function renderRowActions(record: SystemUser, options: UserListColumnOptions) {
  return (
    <div className="mes-table-actions">
      <Button type="link" size="small" onClick={() => options.onDetail(record)}>
        详情
      </Button>
      <Button type="link" size="small" onClick={() => options.onEdit(record)}>
        编辑
      </Button>
      <Button
        type="link"
        size="small"
        onClick={() => options.onPasswordReset(record)}
      >
        重置密码
      </Button>
      {renderStatusAction(record, options)}
    </div>
  )
}

function renderStatusAction(
  record: SystemUser,
  options: UserListColumnOptions,
) {
  if (record.status !== 1) {
    return (
      <Button
        type="link"
        size="small"
        loading={options.isChangingStatus}
        onClick={() => options.onStatusChange(record, 1)}
      >
        启用
      </Button>
    )
  }
  return (
    <Popconfirm
      title="确认停用该账号？"
      description="停用后该用户不能继续登录系统，历史单据和操作记录不受影响。"
      onConfirm={() => options.onStatusChange(record, 0)}
    >
      <Button
        danger
        type="link"
        size="small"
        loading={options.isChangingStatus}
        disabled={record.uuid === options.currentUserUuid}
      >
        停用
      </Button>
    </Popconfirm>
  )
}
