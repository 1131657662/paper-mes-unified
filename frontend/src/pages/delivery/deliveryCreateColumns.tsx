import { Input, Tag, Typography } from 'antd'
import type { ColumnType, ColumnsType } from 'antd/es/table'
import { SOURCE_TYPE } from '../../constants/delivery'
import {
  availableFinishWeight,
  finishSpecText,
  formatKg,
  settleText,
} from '../../features/delivery/utils/deliveryFormatters'
import DeliveryMotherRollCell from './DeliveryMotherRollCell'
import DeliveryFinishIdentityCell from './DeliveryFinishIdentityCell'
import DeliveryOutWeightInput from './DeliveryOutWeightInput'
import {
  isDeliveryGroupRow,
  type DeliveryOrderGroupRow,
  type DeliverySelectionTableRow,
} from './deliveryFinishGrouping'
import type { DeliveryLineEdit } from './deliverySelectionModel'

interface ColumnOptions {
  edits: Record<string, DeliveryLineEdit>
  selectedRowKeys: React.Key[]
  onClearGroup: (group: DeliveryOrderGroupRow) => void
  onEditChange: (finishUuid: string, value: DeliveryLineEdit) => void
  onSelectGroup: (group: DeliveryOrderGroupRow) => void
}

export function createDeliverySelectionColumns(
  options: ColumnOptions,
): ColumnsType<DeliverySelectionTableRow> {
  const columns = [
    finishIdentityColumn(options),
    motherRollColumn(),
    specificationColumn(),
    availableWeightColumn(),
    outWeightColumn(options),
    sourceTypeColumn(),
    settlementColumn(),
    riskColumn(),
    remarkColumn(options),
  ]
  return withGroupCellSpans(columns)
}

function finishIdentityColumn(options: ColumnOptions): ColumnType<DeliverySelectionTableRow> {
  return {
    key: 'finishRollNo',
    title: '成品卷号',
    dataIndex: 'finishRollNo',
    width: 156,
    minWidth: 132,
    render: (value, row) => (
      <DeliveryFinishIdentityCell
        row={row}
        value={value}
        onClearGroup={options.onClearGroup}
        onSelectGroup={options.onSelectGroup}
      />
    ),
  }
}

function motherRollColumn(): ColumnType<DeliverySelectionTableRow> {
  return {
    key: 'sourceMotherRolls',
    title: '来源母卷',
    width: 210,
    minWidth: 160,
    render: (_, row) => isDeliveryGroupRow(row) ? null : <DeliveryMotherRollCell finish={row} />,
  }
}

function specificationColumn(): ColumnType<DeliverySelectionTableRow> {
  return {
    key: 'specification',
    title: '品名 / 规格',
    width: 240,
    minWidth: 180,
    render: (_, row) => isDeliveryGroupRow(row) ? null : (
      <div className="delivery-cell-stack mes-cell-stack">
        <Typography.Text>{row.paperName}</Typography.Text>
        <span>{finishSpecText(row)}</span>
      </div>
    ),
  }
}

function availableWeightColumn(): ColumnType<DeliverySelectionTableRow> {
  return {
    key: 'availableWeight',
    title: '可出库重量',
    align: 'right',
    width: 142,
    minWidth: 120,
    render: (_, row) => isDeliveryGroupRow(row) ? null : (
      <div className="delivery-cell-stack mes-cell-stack">
        <Typography.Text>{formatKg(availableFinishWeight(row))}</Typography.Text>
        <span>原重 {formatKg(row.actualWeight)}</span>
      </div>
    ),
  }
}

function outWeightColumn(options: ColumnOptions): ColumnType<DeliverySelectionTableRow> {
  return {
    key: 'outWeight',
    title: '本次出库',
    width: 136,
    minWidth: 120,
    render: (_, row) => isDeliveryGroupRow(row) ? null : (
      <DeliveryOutWeightInput
        edit={options.edits[row.finishUuid]}
        finish={row}
        selected={options.selectedRowKeys.includes(row.finishUuid)}
        onChange={(value) => options.onEditChange(row.finishUuid, value)}
      />
    ),
  }
}

function sourceTypeColumn(): ColumnType<DeliverySelectionTableRow> {
  return {
    key: 'sourceType',
    title: '出库类型',
    width: 108,
    minWidth: 96,
    render: (_, row) => {
      if (isDeliveryGroupRow(row)) return null
      const source = SOURCE_TYPE[row.sourceType]
      return source ? <Tag className="mes-status-tag" color={source.color}>{source.text}</Tag> : '-'
    },
  }
}

function settlementColumn(): ColumnType<DeliverySelectionTableRow> {
  return {
    key: 'settlement',
    title: '结算 / 开票',
    width: 146,
    minWidth: 126,
    render: (_, row) => isDeliveryGroupRow(row) ? null : (
      <div className="delivery-cell-stack mes-cell-stack">
        <span>{settleText(row.settleType, row.settleDay)}</span>
        <span>{row.isInvoice === 1 ? '开票' : '不开票'}</span>
      </div>
    ),
  }
}

function riskColumn(): ColumnType<DeliverySelectionTableRow> {
  return {
    key: 'settlementRisk',
    title: '风险',
    width: 116,
    minWidth: 104,
    render: (_, row) => isDeliveryGroupRow(row) ? null : row.settlementRisk
      ? <Tag className="mes-status-tag" color="orange">待收款确认</Tag>
      : <Tag className="mes-status-tag">正常</Tag>,
  }
}

function remarkColumn(options: ColumnOptions): ColumnType<DeliverySelectionTableRow> {
  return {
    key: 'remark',
    title: '单卷备注',
    width: 190,
    minWidth: 150,
    render: (_, row) => isDeliveryGroupRow(row) ? null : (
      <Input
        disabled={!options.selectedRowKeys.includes(row.finishUuid)}
        placeholder="本次出库备注"
        value={options.edits[row.finishUuid]?.remark}
        onChange={(event) => options.onEditChange(row.finishUuid, { remark: event.target.value })}
        onClick={(event) => event.stopPropagation()}
      />
    ),
  }
}

function withGroupCellSpans(
  columns: ColumnsType<DeliverySelectionTableRow>,
): ColumnsType<DeliverySelectionTableRow> {
  return columns.map((column, index) => ({
    ...column,
    onCell: (row, rowIndex) => {
      const baseCell = column.onCell?.(row, rowIndex) ?? {}
      if (!isDeliveryGroupRow(row)) return baseCell
      return { ...baseCell, colSpan: index === 0 ? columns.length : 0 }
    },
  }))
}
