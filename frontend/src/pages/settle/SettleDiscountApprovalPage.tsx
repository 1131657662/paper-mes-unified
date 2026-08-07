import { ReloadOutlined } from '@ant-design/icons'
import { Button, Form, Input, Modal, Tabs, message } from 'antd'
import { useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router'
import DocumentPaginationBar from '../../components/biz/DocumentPaginationBar'
import MesPageHeader from '../../components/layout/MesPageHeader'
import QueryLoadErrorAlert from '../../components/feedback/QueryLoadErrorAlert'
import { PERMISSIONS } from '../../constants/permissions'
import { useSettleDiscountApprovalDecision } from '../../features/settle/hooks/useSettleDiscountApprovalDecision'
import { useSettleDiscountApprovalDetail } from '../../features/settle/hooks/useSettleDiscountApprovalDetail'
import { useSettleDiscountApprovalPage } from '../../features/settle/hooks/useSettleDiscountApprovalPage'
import { useAuthUser, useHasPermission } from '../../stores/authStore'
import type { SettleDiscountApproval, SettleDiscountApprovalQuery } from '../../types/settle'
import SettleDiscountApprovalTable from './SettleDiscountApprovalTable'
import SettleDiscountApprovalDetailDrawer from './SettleDiscountApprovalDetailDrawer'
import './SettleDiscountApprovalPage.css'

type Scope = SettleDiscountApprovalQuery['scope']

export default function SettleDiscountApprovalPage() {
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const scope = parseScope(searchParams.get('scope'))
  const [keyword, setKeyword] = useState('')
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(20)
  const [rejectTarget, setRejectTarget] = useState<SettleDiscountApproval | null>(null)
  const [rejectForm] = Form.useForm<{ reason: string }>()
  const approvalUuid = searchParams.get('approval') ?? ''
  const user = useAuthUser()
  const canApproveFinance = useHasPermission(PERMISSIONS.settleDiscountApprove)
  const canApproveAdmin = useHasPermission(PERMISSIONS.settleDiscountAdminApprove)
  const query = useSettleDiscountApprovalPage({ scope, keyword: keyword || undefined,
    current: page, size: pageSize })
  const detailQuery = useSettleDiscountApprovalDetail(approvalUuid)
  const decision = useSettleDiscountApprovalDecision()

  async function act(record: SettleDiscountApproval, action: 'approve' | 'cancel', reason?: string) {
    await decision.mutateAsync({ uuid: record.uuid, action, data: reason ? { reason } : undefined })
    message.success(action === 'approve' ? '优惠审批已批准' : '优惠申请已取消')
  }

  async function reject() {
    const values = await rejectForm.validateFields()
    if (!rejectTarget) return
    await decision.mutateAsync({ uuid: rejectTarget.uuid, action: 'reject', data: values })
    message.success('优惠审批已驳回')
    setRejectTarget(null)
    rejectForm.resetFields()
  }

  function changeScope(next: string) {
    const value = parseScope(next)
    setPage(1)
    setSearchParams(preserveApproval(searchParams, value), { replace: true })
  }

  function closeDetail() {
    const next = new URLSearchParams(searchParams)
    next.delete('approval')
    setSearchParams(next, { replace: true })
  }

  return <div className="discount-approval-page">
    <MesPageHeader title="优惠审批" eyebrow="结算管理"
      actions={<Button icon={<ReloadOutlined />} loading={query.isFetching}
        onClick={() => void query.refetch()}>刷新</Button>} />
    <div className="discount-approval-toolbar">
      <Tabs activeKey={scope} onChange={changeScope} items={[
        { key: 'pending', label: '待我审批' },
        { key: 'processed', label: '已处理' },
        { key: 'mine', label: '我的申请' },
      ]} />
      <Input.Search allowClear placeholder="结算单号或客户" onSearch={(value) => {
        setKeyword(value.trim()); setPage(1)
      }} />
    </div>
    {query.isError && <QueryLoadErrorAlert message="优惠审批记录加载失败"
      description="请重试，当前页面不会修改审批状态。" onRetry={() => void query.refetch()} />}
    <div className="discount-approval-table">
      <SettleDiscountApprovalTable canApproveAdmin={canApproveAdmin}
        canApproveFinance={canApproveFinance} currentUserUuid={user?.uuid}
        data={query.data?.records ?? []} loading={query.isLoading}
        mutationLoading={decision.isPending} onApprove={(record) => void act(record, 'approve')}
        onCancel={(record) => void act(record, 'cancel', '申请人从审批工作台取消')}
        onOpen={(record) => navigate(`/settle-orders/${record.settleUuid}`)}
        onReject={(record) => { rejectForm.resetFields(); setRejectTarget(record) }} />
    </div>
    <DocumentPaginationBar current={page} pageSize={pageSize} total={query.data?.total ?? 0}
      onChange={(next, size) => { setPage(next); setPageSize(size) }} />
    <SettleDiscountApprovalDetailDrawer approval={detailQuery.data} error={detailQuery.isError}
      loading={detailQuery.isLoading} onClose={closeDetail} open={Boolean(approvalUuid)}
      onOpenSettlement={(uuid) => navigate(`/settle-orders/${uuid}`)} />
    <Modal title="驳回优惠审批" open={Boolean(rejectTarget)} confirmLoading={decision.isPending}
      onCancel={() => setRejectTarget(null)} onOk={() => void reject()} okText="确认驳回">
      <Form form={rejectForm} layout="vertical">
        <Form.Item name="reason" label="驳回原因" rules={[{ required: true, message: '请填写驳回原因' }]}>
          <Input.TextArea maxLength={255} rows={4} />
        </Form.Item>
      </Form>
    </Modal>
  </div>
}

function parseScope(value: string | null): Scope {
  return value === 'processed' || value === 'mine' ? value : 'pending'
}

function preserveApproval(params: URLSearchParams, scope: Scope): URLSearchParams {
  const next = new URLSearchParams({ scope })
  const approval = params.get('approval')
  if (approval) next.set('approval', approval)
  return next
}
