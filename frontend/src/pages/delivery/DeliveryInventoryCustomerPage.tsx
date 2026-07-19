import { ArrowLeftOutlined, DownloadOutlined } from '@ant-design/icons'
import { Button, Segmented, Space, Tag, Typography } from 'antd'
import DocumentPaginationBar from '../../components/biz/DocumentPaginationBar'
import QueryLoadErrorAlert from '../../components/feedback/QueryLoadErrorAlert'
import DeliveryInventoryFilterBar from './DeliveryInventoryFilterBar'
import DeliveryInventoryFinishTable from './DeliveryInventoryFinishTable'
import DeliveryInventoryOrderGroupTable from './DeliveryInventoryOrderGroupTable'
import DeliveryInventorySummary from './DeliveryInventorySummary'
import { useDeliveryInventoryCustomerPage } from './useDeliveryInventoryCustomerPage'
import './DeliveryInventoryPage.css'
import './DeliveryInventoryTable.css'
import './DeliveryInventoryCustomerPage.css'

export default function DeliveryInventoryCustomerPage() {
  const model = useDeliveryInventoryCustomerPage()
  return (
    <section className="delivery-inventory-customer-page">
      <header className="delivery-inventory-customer-header">
        <Button aria-label="返回库存" type="text" icon={<ArrowLeftOutlined />} onClick={model.back} />
        <div className="delivery-inventory-customer-title">
          <Typography.Title level={4}>{model.customer?.customerName ?? '客户库存明细'}</Typography.Title>
          <Space size={6} wrap>
            <Typography.Text type="secondary">成品库存</Typography.Text>
            {model.activeWarehouseName && <Tag className="delivery-inventory-tag delivery-inventory-tag--primary">{model.activeWarehouseName}</Tag>}
          </Space>
        </div>
        <Segmented aria-label="库存明细视图" value={model.view}
          options={[{ label: '按卷', value: 'rolls' }, { label: '按加工单', value: 'orders' }]}
          onChange={(value) => model.setView(value as 'rolls' | 'orders')} />
        <Space className="delivery-inventory-customer-header__actions">
          <Button icon={<DownloadOutlined />} loading={model.exportMutation.isPending} onClick={model.exportRows}>导出</Button>
          {model.canManage && <Button type="primary" loading={model.validation.isPending} disabled={!model.selected.length} onClick={() => void model.createDelivery()}>新建出库 {model.selected.length || ''}</Button>}
        </Space>
      </header>
      <DeliveryInventorySummary summary={model.summaryQuery.data} />
      <div className="delivery-inventory-customer-controls">
        <DeliveryInventoryFilterBar filters={model.filters} warehouses={model.warehouses}
          onChange={model.updateFilters} onSearch={(keyword) => model.updateFilters({ ...model.filters, keyword })} />
      </div>
      {model.activeIsError && <QueryLoadErrorAlert message="库存明细加载失败" description="当前空表不代表没有库存，请重新加载。" onRetry={model.reload} />}
      <div className="delivery-inventory-grid delivery-inventory-customer-grid">
        <div className="delivery-inventory-table-shell delivery-inventory-customer-results">
          {model.view === 'rolls' ? (
            <DeliveryInventoryFinishTable data={model.rows} fillHeight loading={model.finishQuery.isLoading || model.finishQuery.isFetching}
              tableTitle="成品卷库存" onReload={model.reload} onOpenDelivery={model.openDelivery}
              selection={model.canManage ? { selectedRowKeys: model.selectedKeys, onChange: model.changeSelection, onToggle: model.toggleSelection, disabled: model.selectionDisabled } : undefined} />
          ) : (
            <DeliveryInventoryOrderGroupTable groups={model.groups}
              loading={model.orderGroupQuery.isLoading || model.orderGroupQuery.isFetching}
              selectedByUuid={model.selectedByUuid} selectionDisabled={model.selectionDisabled}
              onReload={model.reload} onToggle={model.toggleSelection} onToggleGroup={model.toggleGroup} />
          )}
        </div>
        <DocumentPaginationBar current={model.page} pageSize={model.pageSize} total={model.activeTotal} onChange={model.changePage} />
      </div>
    </section>
  )
}
