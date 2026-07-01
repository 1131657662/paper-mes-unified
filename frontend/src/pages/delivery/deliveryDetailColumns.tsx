import { Button, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { SOURCE_TYPE } from '../../constants/delivery'
import TooltipText from '../../components/biz/TooltipText'
import {
  deliveryDetailSpecText,
  deliveryOriginalSnapshotText,
  deliveryProcessSnapshotText,
  formatKg,
} from '../../features/delivery/utils/deliveryFormatters'
import type { DeliveryDetail } from '../../types/delivery'

export function buildDeliveryDetailColumns(options: {
  deliveryStatus?: number
  onRemove: (record: DeliveryDetail) => void
}): ColumnsType<DeliveryDetail> {
  const columns: ColumnsType<DeliveryDetail> = [
    { title: '加工单', dataIndex: 'orderNo', width: 145, render: textCell },
    {
      title: '卷号',
      dataIndex: 'finishRollNo',
      fixed: 'left',
      width: 125,
      render: (value) => <Typography.Text strong>{value || '-'}</Typography.Text>,
    },
    { title: '品名', dataIndex: 'paperName', width: 130, render: textCell },
    { title: '克重', dataIndex: 'gramWeight', width: 78, render: (value) => value ? `${value}g` : '-' },
    { title: '规格', key: 'spec', width: 155, render: (_, record) => deliveryDetailSpecText(record) },
    { title: '件重', dataIndex: 'actualWeight', align: 'right', width: 110, render: formatKg },
    { title: '出库重量', dataIndex: 'outWeight', align: 'right', width: 110, render: formatKg },
    {
      title: '原纸信息',
      dataIndex: 'originalSummary',
      width: 300,
      render: (_, record) => textCell(deliveryOriginalSnapshotText(record)),
    },
    { title: '加工方式', dataIndex: 'processModeText', width: 150, render: textCell },
    {
      title: '工艺摘要',
      dataIndex: 'processSummary',
      width: 240,
      render: (_, record) => textCell(deliveryProcessSnapshotText(record)),
    },
    {
      title: '来源',
      dataIndex: 'sourceType',
      width: 110,
      render: (value) => {
        const item = value ? SOURCE_TYPE[value] : undefined
        return item ? <Tag className="mes-status-tag" color={item.color}>{item.text}</Tag> : '-'
      },
    },
    { title: '原纸卷号', dataIndex: 'originalRollNos', width: 150, render: textCell },
    { title: '备注', dataIndex: 'remark', width: 150, render: textCell },
    { title: '回录备注', dataIndex: 'actualRemark', width: 150, render: textCell },
  ]

  if (options.deliveryStatus === 1) {
    columns.push({
      title: '操作',
      key: 'actions',
      fixed: 'right',
      width: 86,
      render: (_, record) => (
        <Button type="link" size="small" danger onClick={() => options.onRemove(record)}>
          移出
        </Button>
      ),
    })
  }
  return columns
}

function textCell(value?: string | number) {
  return <TooltipText value={value} />
}
