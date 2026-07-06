import { useState } from 'react'
import { Button, DatePicker, Form, Input, Modal, Select, message } from 'antd'
import { CheckOutlined, DownloadOutlined, PrinterOutlined, ReloadOutlined, RollbackOutlined, SearchOutlined } from '@ant-design/icons'
import type { Dayjs } from 'dayjs'
import { useNavigate } from 'react-router-dom'
import DocumentPaginationBar from '../../components/biz/DocumentPaginationBar'
import DocumentListShell from '../../components/biz/DocumentListShell'
import { useDocumentRowSelection } from '../../components/biz/useDocumentRowSelection'
import { DELIVERY_STATUS, SETTLE_BLOCK_ACTION } from '../../constants/delivery'
import { PERMISSIONS } from '../../constants/permissions'
import { useCustomers } from '../../features/processOrderCreate/hooks/useReferenceData'
import DeliveryOrderTable from '../../features/delivery/components/DeliveryOrderTable'
import { useConfirmDelivery } from '../../features/delivery/hooks/useConfirmDelivery'
import { useDeliveryOrders } from '../../features/delivery/hooks/useDeliveryOrders'
import { useConfiguredPageSize } from '../../features/systemConfig/hooks/useConfiguredPageSize'
import { exportDeliveryOrder, getDeliveryOrderList, rollbackDeliveryOrder } from '../../api/delivery'
import { useHasPermission } from '../../stores/authStore'
import type { DeliveryOrder, DeliveryQuery } from '../../types/delivery'
import { datedCsvFilename, exportRowsToCsv } from '../../utils/exportCsv'
import { formatTonFromKg } from '../../utils/numberFormatters'
import './DeliveryOrderList.css'

type QueueFilter = 'all' | 'pending' | 'done'
const EXPORT_PAGE_SIZE = 10000

interface SearchFormValues {
  customerUuid?: string
  dateRange?: [Dayjs, Dayjs] | null
  deliveryStatus?: number
  keyword?: string
}

export default function DeliveryOrderList() {
  const [form] = Form.useForm<SearchFormValues>()
  const navigate = useNavigate()
  const customersQuery = useCustomers()
  const confirmMutation = useConfirmDelivery()
  const [queueFilter, setQueueFilter] = useState<QueueFilter>('all')
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useConfiguredPageSize(20)
  const [filters, setFilters] = useState<DeliveryQuery>({})
  const [exportingList, setExportingList] = useState(false)
  const [rollingBackSelected, setRollingBackSelected] = useState(false)
  const canManageDelivery = useHasPermission(PERMISSIONS.deliveryManage)
  const rowSelection = useDocumentRowSelection<DeliveryOrder>()
  const query = { ...filters, current: page, deliveryStatus: deliveryStatus(queueFilter, filters), size: pageSize }
  const ordersQuery = useDeliveryOrders(query)
  const orders = ordersQuery.data?.records ?? []
  const tableDensity = tableDensityMode(orders.length, pageSize, ordersQuery.isLoading)
  const selectedPendingOrders = rowSelection.selectedRows.filter((record) => record.deliveryStatus === 1)
  const selectedSingle = rowSelection.selectedRows.length === 1 ? rowSelection.selectedRows[0] : undefined

  const handleSearch = (values: SearchFormValues) => {
    setFilters({
      customerUuid: values.customerUuid,
      dateFrom: values.dateRange?.[0]?.format('YYYY-MM-DD'),
      dateTo: values.dateRange?.[1]?.format('YYYY-MM-DD'),
      deliveryStatus: values.deliveryStatus,
      keyword: values.keyword?.trim() || undefined,
    })
    setPage(1)
    rowSelection.clear()
  }

  const handleReset = () => {
    form.resetFields()
    setFilters({})
    setPage(1)
    rowSelection.clear()
  }

  const handleConfirm = async (record: DeliveryOrder) => {
    if (!canManageDelivery) return
    const signUser = await askSignUser(record)
    if (signUser === null) return
    await confirmMutation.mutateAsync({
      uuid: record.uuid,
      data: signUser ? { signUser } : undefined,
    })
    message.success('出库签收完成')
    rowSelection.clear()
  }

  const handleBatchConfirm = async () => {
    if (!canManageDelivery) return
    if (selectedPendingOrders.length === 0) {
      message.warning('请先选择待出库单据')
      return
    }
    const confirmed = await confirmBatchSign(selectedPendingOrders.length)
    if (!confirmed) return
    for (const record of selectedPendingOrders) {
      await confirmMutation.mutateAsync({ uuid: record.uuid })
    }
    message.success(`已签收 ${selectedPendingOrders.length} 张出库单`)
    rowSelection.clear()
  }

  const handlePrintSelected = () => {
    if (!selectedSingle) {
      message.warning('请选择一张出库单打印')
      return
    }
    navigate(`/delivery-orders/${selectedSingle.uuid}?print=1`)
  }

  const handleExportSelected = async () => {
    if (!selectedSingle) {
      message.warning('请选择一张出库单导出')
      return
    }
    await exportDeliveryOrder({
      documentNo: selectedSingle.deliveryNo,
      uuid: selectedSingle.uuid,
    })
  }

  const handleExportList = async () => {
    setExportingList(true)
    try {
      const res = await getDeliveryOrderList(buildExportQuery(filters, queueFilter))
      const rows = res.records ?? []
      if (rows.length === 0) {
        message.warning('当前筛选条件下暂无可导出的出库单')
        return
      }
      const result = exportRowsToCsv({
        columns: deliveryExportColumns(),
        filename: datedCsvFilename('出库对账'),
        rows,
      })
      message.success(`已导出 ${result.filename}`)
    } finally {
      setExportingList(false)
    }
  }

  const handleRollbackSelected = async () => {
    if (!canManageDelivery) return
    if (!selectedSingle || selectedSingle.deliveryStatus !== 2) {
      message.warning('请选择一张已出库单回退')
      return
    }
    const reason = await askRollbackReason(selectedSingle.deliveryNo)
    if (!reason) return
    setRollingBackSelected(true)
    try {
      await rollbackDeliveryOrder(selectedSingle.uuid, { reason })
      message.success('已回退为待出库，可继续改单')
      rowSelection.clear()
      ordersQuery.refetch()
    } finally {
      setRollingBackSelected(false)
    }
  }

  const handlePageChange = (nextPage: number, nextPageSize: number) => {
    setPage(nextPage)
    setPageSize(nextPageSize)
    rowSelection.clear()
  }

  return (
    <DocumentListShell
      title="出库管理"
      createText="新建出库单"
      canCreate={canManageDelivery}
      queue={queueFilter}
      queueOptions={[
        { label: '全部', value: 'all' },
        { label: '待出库', value: 'pending' },
        { label: '已出库', value: 'done' },
      ]}
      search={(
        <DeliverySearchBar
          form={form}
          customers={customersQuery.data?.records ?? []}
          loadingCustomers={customersQuery.isLoading}
          onReset={handleReset}
          onSearch={handleSearch}
        />
      )}
      leftActions={(
        <>
          {canManageDelivery && (
            <Button
              icon={<CheckOutlined />}
              disabled={selectedPendingOrders.length === 0}
              loading={confirmMutation.isPending}
              onClick={handleBatchConfirm}
            >
              批量签收
            </Button>
          )}
          <Button icon={<PrinterOutlined />} disabled={!selectedSingle} onClick={handlePrintSelected}>打印出库单</Button>
          <Button icon={<DownloadOutlined />} disabled={!selectedSingle} onClick={handleExportSelected}>导出 Excel</Button>
          <Button icon={<DownloadOutlined />} loading={exportingList} onClick={handleExportList}>导出对账</Button>
          {canManageDelivery && (
            <Button
              danger
              icon={<RollbackOutlined />}
              disabled={!selectedSingle || selectedSingle.deliveryStatus !== 2}
              loading={rollingBackSelected}
              onClick={handleRollbackSelected}
            >
              回退出库
            </Button>
          )}
        </>
      )}
      onCreate={() => navigate('/delivery-orders/create')}
      onQueueChange={(value) => {
        setQueueFilter(value)
        setPage(1)
        rowSelection.clear()
      }}
    >
      <div className="document-page-table" data-table-density={tableDensity}>
        <DeliveryOrderTable
          canManageDelivery={canManageDelivery}
          data={orders}
          loading={ordersQuery.isLoading || ordersQuery.isFetching}
          onReload={() => ordersQuery.refetch()}
          rowClassName={rowSelection.rowClassName}
          rowSelection={rowSelection.rowSelection}
          onConfirm={handleConfirm}
          onDetail={(record) => navigate(`/delivery-orders/${record.uuid}`)}
          onRow={rowSelection.onRow}
        />
      </div>
      <DocumentPaginationBar
        current={page}
        pageSize={pageSize}
        total={ordersQuery.data?.total ?? 0}
        onChange={handlePageChange}
      />
    </DocumentListShell>
  )
}

function tableDensityMode(rowCount: number, pageSize: number, loading: boolean) {
  if (loading) return 'fill'
  if (rowCount === 0) return 'empty'
  return rowCount < pageSize ? 'short' : 'fill'
}

function DeliverySearchBar({
  customers,
  form,
  loadingCustomers,
  onReset,
  onSearch,
}: {
  customers: { customerName: string; uuid: string }[]
  form: ReturnType<typeof Form.useForm<SearchFormValues>>[0]
  loadingCustomers: boolean
  onReset: () => void
  onSearch: (values: SearchFormValues) => void
}) {
  return (
    <Form form={form} layout="vertical" className="document-searchbar" onFinish={onSearch}>
      <div className="document-searchbar__grid">
        <Form.Item name="keyword" label="出库单号/客户">
          <Input allowClear placeholder="输入单号、客户或备注" />
        </Form.Item>
        <Form.Item name="customerUuid" label="客户">
          <Select
            allowClear
            showSearch
            loading={loadingCustomers}
            placeholder="全部客户"
            options={customers.map((item) => ({ label: item.customerName, value: item.uuid }))}
            optionFilterProp="label"
          />
        </Form.Item>
        <Form.Item name="dateRange" label="出库日期">
          <DatePicker.RangePicker placeholder={['开始日期', '结束日期']} style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item name="deliveryStatus" label="状态">
          <Select allowClear placeholder="全部状态" options={statusOptions()} />
        </Form.Item>
        <div className="document-searchbar__actions">
          <Button type="primary" htmlType="submit" icon={<SearchOutlined />}>查询</Button>
          <Button icon={<ReloadOutlined />} onClick={onReset}>重置</Button>
        </div>
      </div>
    </Form>
  )
}

function deliveryStatus(filter: QueueFilter, filters: DeliveryQuery) {
  if (filters.deliveryStatus) return filters.deliveryStatus
  if (filter === 'pending') return 1
  if (filter === 'done') return 2
  return undefined
}

function statusOptions() {
  return Object.entries(DELIVERY_STATUS).map(([value, item]) => ({
    label: item.text,
    value: Number(value),
  }))
}

function buildExportQuery(filters: DeliveryQuery, filter: QueueFilter): DeliveryQuery {
  return {
    ...filters,
    current: 1,
    deliveryStatus: deliveryStatus(filter, filters),
    size: EXPORT_PAGE_SIZE,
  }
}

function statusText(status?: number) {
  if (status == null) return '-'
  return DELIVERY_STATUS[status]?.text ?? String(status)
}

function settleBlockText(action?: number) {
  if (action == null) return '-'
  return SETTLE_BLOCK_ACTION[action] ?? String(action)
}

function deliveryExportColumns() {
  return [
    { header: '出库单号', value: (row: DeliveryOrder) => row.deliveryNo },
    { header: '客户', value: (row: DeliveryOrder) => row.customerName },
    { header: '出库日期', value: (row: DeliveryOrder) => row.deliveryDate },
    { header: '卷数', value: (row: DeliveryOrder) => row.totalCount },
    { header: '出库吨位', value: (row: DeliveryOrder) => formatTonFromKg(row.totalWeight) },
    { header: '提货人', value: (row: DeliveryOrder) => row.pickerName },
    { header: '车牌', value: (row: DeliveryOrder) => row.carNo },
    { header: '柜号', value: (row: DeliveryOrder) => row.containerNo },
    { header: '状态', value: (row: DeliveryOrder) => statusText(row.deliveryStatus) },
    { header: '结算拦截', value: (row: DeliveryOrder) => settleBlockText(row.settleBlockAction) },
    { header: '签收人', value: (row: DeliveryOrder) => row.signUser },
    { header: '签收时间', value: (row: DeliveryOrder) => row.signTime },
    { header: '备注', value: (row: DeliveryOrder) => row.remark },
    { header: '创建时间', value: (row: DeliveryOrder) => row.createTime },
  ]
}

function askSignUser(record: DeliveryOrder) {
  let signUser = ''
  return new Promise<string | null>((resolve) => {
    Modal.confirm({
      title: `确认签收 ${record.deliveryNo}`,
      content: (
        <div className="delivery-sign-modal">
          <p>确认后将扣减所选成品库存，状态变为已出库。</p>
          <Input
            placeholder="签收人姓名（可选）"
            onChange={(event) => {
              signUser = event.target.value
            }}
          />
        </div>
      ),
      okText: '确认签收',
      cancelText: '取消',
      onOk: () => resolve(signUser.trim()),
      onCancel: () => resolve(null),
    })
  })
}

function confirmBatchSign(count: number) {
  return new Promise<boolean>((resolve) => {
    Modal.confirm({
      title: `批量签收 ${count} 张出库单`,
      content: '确认后将逐张扣减所选出库单对应的成品库存。请确认这些单据均已完成司机签收。',
      okText: '确认签收',
      cancelText: '取消',
      onOk: () => resolve(true),
      onCancel: () => resolve(false),
    })
  })
}

function askRollbackReason(deliveryNo: string) {
  let reason = ''
  return new Promise<string | null>((resolve) => {
    Modal.confirm({
      title: `回退出库 ${deliveryNo}`,
      content: (
        <div className="delivery-sign-modal">
          <p>回退后成品卷恢复为已入库，出库单回到待出库状态，可在详情页移出装不下的明细后重新签收。</p>
          <Input.TextArea
            rows={3}
            placeholder="请输入回退原因，例如：车辆装不下，需要减少本次装车卷数"
            onChange={(event) => {
              reason = event.target.value
            }}
          />
        </div>
      ),
      okText: '确认回退',
      okButtonProps: { danger: true },
      cancelText: '取消',
      onOk: () => {
        const value = reason.trim()
        if (!value) {
          message.warning('请填写回退原因')
          return Promise.reject(new Error('reason required'))
        }
        resolve(value)
      },
      onCancel: () => resolve(null),
    })
  })
}
