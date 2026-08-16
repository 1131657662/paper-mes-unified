import { useRef, useState } from 'react'
import { Button, message } from 'antd'
import { DownloadOutlined, PlusOutlined } from '@ant-design/icons'
import { ProTable } from '@ant-design/pro-components'
import type { ActionType, ProColumns } from '@ant-design/pro-components'
import { useNavigate } from 'react-router'
import { pageUsers } from '../../api/user'
import { mesProTableOptions } from '../../components/biz/mesProTableOptions'
import { renderCompatibleTableOptions } from '../../components/biz/tableToolbarOptionsRender'
import { mesTablePagination } from '../../components/biz/mesPaginationUtils'
import { useResizableTableColumns } from '../../components/useResizableTableColumns'
import { useAuthUser } from '../../stores/authStore'
import type {
  SystemUser,
  UserQuery,
  UserRoleCode,
  UserStatus,
} from '../../types/user'
import {
  useResetUserPassword,
  useUpdateUserStatus,
} from '../../features/user/hooks/useUserMutations'
import { getRoleModuleNames } from '../../constants/permissionMeta'
import { useTableColumnsState } from '../../hooks/useTableColumnsState'
import { datedCsvFilename, exportRowsToCsv } from '../../utils/exportCsv'
import { formatDateTime } from '../../utils/dateTime'
import { roleText } from './userDisplay'
import { createUserListColumns } from './userListColumns'
import UserPasswordModal from './UserPasswordModal'
import '../documentModule.css'
import './UserProfile.css'

export default function UserList() {
  const actionRef = useRef<ActionType>(null)
  const latestQueryRef = useRef<UserQuery>({})
  const navigate = useNavigate()
  const currentUser = useAuthUser()
  const columnsState = useTableColumnsState('table-columns-users')
  const [passwordUser, setPasswordUser] = useState<SystemUser>()
  const [exporting, setExporting] = useState(false)
  const { mutateAsync: changeStatus, isPending: isChangingStatus } =
    useUpdateUserStatus()
  const { mutateAsync: resetPassword, isPending: isResettingPassword } =
    useResetUserPassword()

  const handleStatus = async (record: SystemUser, status: UserStatus) => {
    await changeStatus({ uuid: record.uuid, data: { status } })
    message.success(status === 1 ? '账号已启用' : '账号已停用')
    actionRef.current?.reload()
  }

  const handleExport = async () => {
    setExporting(true)
    try {
      const res = await pageUsers({
        ...latestQueryRef.current,
        current: 1,
        size: 10000,
      })
      const result = exportRowsToCsv({
        columns: userExportColumns(),
        filename: datedCsvFilename('用户权限'),
        rows: res.records ?? [],
      })
      message.success(`已导出 ${result.filename}`)
    } finally {
      setExporting(false)
    }
  }

  const columns = createUserListColumns({
    currentUserUuid: currentUser?.uuid,
    isChangingStatus,
    onDetail: (record) => navigate(`/users/${record.uuid}`),
    onEdit: (record) => navigate(`/users/${record.uuid}/edit`),
    onPasswordReset: setPasswordUser,
    onStatusChange: handleStatus,
  })
  const resizable = useResizableTableColumns<
    SystemUser,
    ProColumns<SystemUser>
  >(columns, 'users')

  return (
    <>
      <ProTable<SystemUser>
        className="mes-pro-table-page"
        tableClassName="user-list__table"
        rowKey="uuid"
        actionRef={actionRef}
        columns={resizable.columns}
        columnsState={columnsState}
        components={resizable.components}
        headerTitle={<h1 className="user-list__title">用户权限</h1>}
        toolBarRender={() => [
          <Button
            key="export"
            icon={<DownloadOutlined />}
            loading={exporting}
            onClick={handleExport}
          >
            导出
          </Button>,
          <Button
            key="add"
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => navigate('/users/create')}
          >
            新增用户
          </Button>,
        ]}
        request={async (params) => {
          const query = {
            current: params.current,
            size: params.pageSize,
            keyword: params.username || params.realName,
            roleCode: params.roleCode as UserRoleCode | undefined,
            status: params.status as UserStatus | undefined,
          }
          latestQueryRef.current = query
          const res = await pageUsers(query)
          return {
            data: res.records ?? [],
            total: res.total ?? 0,
            success: true,
          }
        }}
        tableViewRender={(_tableProps, defaultTable) => (
          <div ref={makeUserTableBodyFocusable} className="user-list__table-focus-anchor">
            {defaultTable}
          </div>
        )}
        bordered
        pagination={mesTablePagination(10)}
        search={{ labelWidth: 'auto' }}
        scroll={{ x: resizable.scrollX, y: '100%' }}
        tableLayout="fixed"
        options={mesProTableOptions()}
        optionsRender={renderCompatibleTableOptions}
      />
      <UserPasswordModal
        open={!!passwordUser}
        submitting={isResettingPassword}
        userName={
          passwordUser
            ? `${passwordUser.realName}（${passwordUser.username}）`
            : undefined
        }
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

function makeUserTableBodyFocusable(anchor: HTMLDivElement | null) {
  anchor
    ?.querySelector<HTMLElement>('.ant-table-body')
    ?.setAttribute('tabindex', '0')
}

function userExportColumns() {
  return [
    { header: '登录账号', value: (row: SystemUser) => row.username },
    { header: '姓名', value: (row: SystemUser) => row.realName },
    { header: '角色', value: (row: SystemUser) => roleText(row.roleCode) },
    {
      header: '权限范围',
      value: (row: SystemUser) => getRoleModuleNames(row.roleCode).join('、'),
    },
    {
      header: '状态',
      value: (row: SystemUser) => (row.status === 1 ? '启用' : '停用'),
    },
    {
      header: '最近登录',
      value: (row: SystemUser) => formatDateTime(row.lastLoginTime),
    },
    { header: '备注', value: (row: SystemUser) => row.remark },
    { header: '创建时间', value: (row: SystemUser) => row.createTime },
  ]
}

export { UserRoleSummary } from './UserListDisplay'
