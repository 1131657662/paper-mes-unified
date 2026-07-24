import { Descriptions, Tag } from 'antd'
import type { PrintResultVO } from '../../../types/processOrder'

export default function PrintResultSummary({ result }: { result: PrintResultVO }) {
  return (
    <Descriptions size="small" column={4} className="print-issue__result">
      <Descriptions.Item label="打印状态">{result.printStatus === 1 ? '已确认打印' : '已下发，未打印'}</Descriptions.Item>
      <Descriptions.Item label="打印次数">{result.printCount ?? '-'}</Descriptions.Item>
      <Descriptions.Item label="操作时间">{result.printTime ?? '-'}</Descriptions.Item>
      <Descriptions.Item label="正式号">
        <Tag color="blue">{result.finishRollNos?.length ?? 0} 个</Tag>
      </Descriptions.Item>
    </Descriptions>
  )
}
