import { Input, InputNumber, Space, Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { SOURCE_TYPE } from '../../constants/delivery'
import {
  availableFinishWeight,
  finishSpecText,
  formatKg,
  settleText,
} from '../../features/delivery/utils/deliveryFormatters'
import type { AvailableFinishVO } from '../../types/delivery'

export interface DeliveryLineEdit {
  outWeight?: number
  remark?: string
}

interface Props {
  data: AvailableFinishVO[]
  edits: Record<string, DeliveryLineEdit>
  loading: boolean
  selectedRowKeys: React.Key[]
  onEditChange: (finishUuid: string, value: DeliveryLineEdit) => void
  onSelectionChange: (keys: React.Key[]) => void
}

export default function DeliveryCreateTable({
  data,
  edits,
  loading,
  onEditChange,
  onSelectionChange,
  selectedRowKeys,
}: Props) {
  return (
    <Table<AvailableFinishVO>
      rowKey="finishUuid"
      size="small"
      loading={loading}
      columns={buildColumns(edits, onEditChange)}
      dataSource={data}
      pagination={false}
      rowSelection={{ selectedRowKeys, onChange: onSelectionChange }}
      rowClassName={(record) => (selectedRowKeys.includes(record.finishUuid) ? 'is-selected' : '')}
      onRow={(record) => ({
        onClick: () => toggleKey(record.finishUuid, selectedRowKeys, onSelectionChange),
      })}
      scroll={{ x: 1220, y: 460 }}
    />
  )
}

function buildColumns(
  edits: Record<string, DeliveryLineEdit>,
  onEditChange: (finishUuid: string, value: DeliveryLineEdit) => void,
): ColumnsType<AvailableFinishVO> {
  return [
    {
      title: '成品卷号',
      dataIndex: 'finishRollNo',
      fixed: 'left',
      width: 150,
      render: (value, record) => (
        <Space size={4} wrap>
          <Typography.Text strong>{value}</Typography.Text>
          {record.isRemain === 1 ? <Tag color="orange">余料</Tag> : <Tag color="green">成品</Tag>}
        </Space>
      ),
    },
    {
      title: '加工单',
      dataIndex: 'orderNo',
      width: 150,
      render: (value, record) => (
        <div className="delivery-cell-stack mes-cell-stack">
          <Typography.Text>{value}</Typography.Text>
          <span>{record.orderDate ?? '-'}</span>
        </div>
      ),
    },
    {
      title: '品名/规格',
      dataIndex: 'paperName',
      width: 230,
      render: (value, record) => (
        <div className="delivery-cell-stack mes-cell-stack">
          <Typography.Text>{value}</Typography.Text>
          <span>{finishSpecText(record)}</span>
        </div>
      ),
    },
    {
      title: '重量',
      dataIndex: 'actualWeight',
      align: 'right',
      width: 140,
      render: (_, record) => (
        <div className="delivery-cell-stack mes-cell-stack">
          <Typography.Text>{formatKg(record.actualWeight)}</Typography.Text>
          <span>可出库 {formatKg(availableFinishWeight(record))}</span>
        </div>
      ),
    },
    {
      title: '出库重量',
      dataIndex: 'outWeight',
      width: 130,
      render: (_, record) => {
        const maxWeight = availableFinishWeight(record)
        return (
          <InputNumber
            min={0}
            max={maxWeight}
            precision={3}
            disabled={record.sourceType === 2}
            value={edits[record.finishUuid]?.outWeight ?? maxWeight}
            onChange={(value) => onEditChange(record.finishUuid, { outWeight: value ?? 0 })}
            onClick={(event) => event.stopPropagation()}
            style={{ width: '100%' }}
          />
        )
      },
    },
    {
      title: '来源',
      dataIndex: 'sourceType',
      width: 105,
      render: (value) => {
        const source = SOURCE_TYPE[value]
        return source ? <Tag className="mes-status-tag" color={source.color}>{source.text}</Tag> : '-'
      },
    },
    {
      title: '结算/开票',
      dataIndex: 'settleType',
      width: 150,
      render: (_, record) => (
        <div className="delivery-cell-stack mes-cell-stack">
          <span>{settleText(record.settleType, record.settleDay)}</span>
          <span>{record.isInvoice === 1 ? '开票' : '不开票'}</span>
        </div>
      ),
    },
    {
      title: '风险',
      dataIndex: 'settlementRisk',
      width: 112,
      render: (value) => value
        ? <Tag className="mes-status-tag" color="orange">待收款确认</Tag>
        : <Tag className="mes-status-tag">正常</Tag>,
    },
    {
      title: '备注',
      dataIndex: 'remark',
      width: 180,
      render: (_, record) => (
        <Input
          placeholder="本次出库备注"
          value={edits[record.finishUuid]?.remark}
          onChange={(event) => onEditChange(record.finishUuid, { remark: event.target.value })}
          onClick={(event) => event.stopPropagation()}
        />
      ),
    },
  ]
}

function toggleKey(
  key: string,
  selectedRowKeys: React.Key[],
  onSelectionChange: (keys: React.Key[]) => void,
) {
  if (selectedRowKeys.includes(key)) {
    onSelectionChange(selectedRowKeys.filter((item) => item !== key))
    return
  }
  onSelectionChange([...selectedRowKeys, key])
}
