import { Descriptions, Tag } from 'antd'
import type { PrintResultVO } from '../../../types/processOrder'

export default function PrintResultSummary({ result }: { result: PrintResultVO }) {
  return (
    <Descriptions size="small" column={3} className="print-issue__result">
      <Descriptions.Item label="打印次数">{result.printCount ?? '-'}</Descriptions.Item>
      <Descriptions.Item label="打印时间">{result.printTime ?? '-'}</Descriptions.Item>
      <Descriptions.Item label="正式号">
        <Tag color="blue">{result.finishRollNos?.length ?? 0} 个</Tag>
      </Descriptions.Item>
    </Descriptions>
  )
}
