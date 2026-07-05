import {
  CheckCircleOutlined,
  CopyOutlined,
  FileTextOutlined,
  PlusOutlined,
  UnorderedListOutlined,
} from '@ant-design/icons'
import { Button, Space, Statistic, Tag, Typography, message } from 'antd'
import { ORDER_STATUS } from '../../../constants/processOrder'
import type { ProcessOrderSubmitVO } from '../../../types/processOrder'
import { formatNumber } from '../../../utils/numberFormatters'
import './SubmitSuccessPanel.css'

interface Props {
  result: ProcessOrderSubmitVO
  onBackToList: () => void
  onCreateAnother: () => void
  onViewDetail: (orderUuid: string) => void
}

export default function SubmitSuccessPanel({
  result,
  onBackToList,
  onCreateAnother,
  onViewDetail,
}: Props) {
  const finishNos = result.finishRollNos ?? []
  const spareNos = result.spareRollNos ?? []
  const status = ORDER_STATUS[result.orderStatus ?? 1]

  return (
    <section className="submit-success-panel">
      <div className="submit-success-panel__header">
        <CheckCircleOutlined className="submit-success-panel__icon" />
        <div className="submit-success-panel__title">
          <Typography.Title level={4}>加工单已提交</Typography.Title>
          <Typography.Text type="secondary">真实成品卷号已由后端生成，可进入详情打印或继续开下一单。</Typography.Text>
        </div>
        <Tag color={status?.color}>{status?.text ?? '待下发'}</Tag>
      </div>

      <div className="submit-success-panel__stats">
        <Statistic title="加工单号" value={result.orderNo ?? '-'} />
        <Statistic title="正式卷号" value={formatNumber(finishNos.length)} suffix="个" />
        <Statistic title="备用卷号" value={formatNumber(spareNos.length)} suffix="个" />
      </div>

      <div className="submit-success-panel__rolls">
        <RollNumberGroup title="正式卷号" numbers={finishNos} color="blue" />
        <RollNumberGroup title="备用卷号" numbers={spareNos} color="default" />
      </div>

      <div className="submit-success-panel__actions">
        <Space wrap>
          <Button
            type="primary"
            icon={<FileTextOutlined />}
            disabled={!result.orderUuid}
            onClick={() => result.orderUuid && onViewDetail(result.orderUuid)}
          >
            查看加工单详情
          </Button>
          <Button icon={<UnorderedListOutlined />} onClick={onBackToList}>
            返回列表
          </Button>
          <Button icon={<PlusOutlined />} onClick={onCreateAnother}>
            继续新建
          </Button>
          <Button icon={<CopyOutlined />} onClick={() => copyRollNumbers([...finishNos, ...spareNos])}>
            复制卷号
          </Button>
        </Space>
      </div>
    </section>
  )
}

function RollNumberGroup({ title, numbers, color }: RollNumberGroupProps) {
  return (
    <div className="submit-success-panel__roll-group">
      <div className="submit-success-panel__roll-title">
        <Typography.Text strong>{title}</Typography.Text>
        <Tag>{numbers.length}</Tag>
      </div>
      {numbers.length ? (
        <div className="submit-success-panel__tag-list">
          {numbers.map((number) => (
            <Tag key={number} color={color}>
              {number}
            </Tag>
          ))}
        </div>
      ) : (
        <Typography.Text type="secondary">无</Typography.Text>
      )}
    </div>
  )
}

async function copyRollNumbers(numbers: string[]) {
  if (!numbers.length) {
    message.info('暂无卷号可复制')
    return
  }
  try {
    await navigator.clipboard.writeText(numbers.join('\n'))
    message.success('卷号已复制')
  } catch {
    message.warning('浏览器暂不允许复制，请在卷号区手动选择')
  }
}

interface RollNumberGroupProps {
  title: string
  numbers: string[]
  color: string
}
