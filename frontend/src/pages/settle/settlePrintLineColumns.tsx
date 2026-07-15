import { Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { formatKg, formatMoney } from '../../features/settle/utils/settleFormatters'
import type { SettlePrintLine } from '../../types/settle'
import {
  SettleFeeBasisCell,
  SettleFinishResultCell,
  SettleOriginalCell,
  SettleOriginalSpecCell,
  SettleProcessNameCell,
  SettleTrimCell,
} from './SettleCustomerLineCells'

export const settlePrintLineColumns: ColumnsType<SettlePrintLine> = [
  { title: '原纸', fixed: 'left', width: 180, render: (_, record) => <SettleOriginalCell line={record} /> },
  { title: '品名/规格', width: 210, render: (_, record) => <SettleOriginalSpecCell line={record} /> },
  { title: '原纸重量', dataIndex: 'originalWeight', align: 'right', width: 115, render: formatKg },
  { title: '加工项目', width: 140, render: (_, record) => <SettleProcessNameCell line={record} /> },
  { title: '计费依据', width: 310, render: (_, record) => <SettleFeeBasisCell line={record} /> },
  { title: '成品结果', width: 190, render: (_, record) => <SettleFinishResultCell line={record} /> },
  { title: '切边', width: 135, render: (_, record) => <SettleTrimCell line={record} /> },
  {
    title: '加工费',
    dataIndex: 'processAmount',
    align: 'right',
    width: 115,
    render: (value) => <Typography.Text strong>{formatMoney(value)}</Typography.Text>,
  },
]
