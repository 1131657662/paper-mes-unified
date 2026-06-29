import { Alert, Button, Form, Input, InputNumber, Select, Tag, Typography } from 'antd'
import { CopyOutlined } from '@ant-design/icons'
import { IS_REMAIN } from '../../../constants/processOrder'
import type { FinishRoll } from '../../../types/processOrder'
import type { BackRecordFormValues } from './backRecordUtils'
import type { BackRecordWorkItem, WorkbenchFinish } from './backRecordWorkbenchTypes'

interface Props {
  item: BackRecordWorkItem
}

export default function BackRecordFinishEntryList({ item }: Props) {
  const form = Form.useFormInstance<BackRecordFormValues>()

  const fillFinishes = () => {
    for (const { finish } of item.finishes) {
      form.setFieldValue(['finishes', finish.uuid], {
        actualWeight: finish.actualWeight ?? (finish.isSpare === 1 ? undefined : finish.estimateWeight),
        scrapWeight: finish.scrapWeight,
        isRemain: finish.isRemain ?? 0,
        isAbnormal: finish.isAbnormal ?? 0,
        abnormalType: finish.abnormalType,
        actualRemark: finish.actualRemark,
      })
    }
  }

  return (
    <section className="back-record-panel">
      <div className="back-record-panel__head">
        <Typography.Text strong>成品实重</Typography.Text>
        <Button size="small" icon={<CopyOutlined />} onClick={fillFinishes} disabled={item.finishes.length === 0}>
          带入预估
        </Button>
      </div>
      {item.finishes.length === 0 ? (
        <Alert showIcon type="info" message="当前母卷没有已绑定成品。直发卷会在提交回录时由后端生成出库用记录。" />
      ) : (
        <div className="back-record-finish-list">
          {item.finishes.map((entry) => <FinishEntryRow key={entry.finish.uuid} entry={entry} />)}
        </div>
      )}
    </section>
  )
}

function FinishEntryRow({ entry }: { entry: WorkbenchFinish }) {
  const finish = entry.finish

  return (
    <div className="back-record-finish-row">
      <div className="back-record-finish-row__identity">
        <Typography.Text strong>{finish.finishRollNo || '-'}</Typography.Text>
        <span>{finishSpec(finish)}</span>
        <div>
          <Tag color={finish.isSpare === 1 ? 'orange' : 'blue'}>{finish.isSpare === 1 ? '备用' : '正式'}</Tag>
          {entry.bindMode === 'inferred' && <Tag color="warning">辅助匹配</Tag>}
          {entry.bindMode === 'pool' && <Tag color="warning">待核对</Tag>}
        </div>
      </div>
      <div className="back-record-finish-row__fields">
        <Form.Item
          name={['finishes', finish.uuid, 'actualWeight']}
          label="实际重量"
          rules={finish.isSpare === 1 ? undefined : [
            { required: true, message: '必填' },
            { type: 'number', min: 0.001, message: '需大于0' },
          ]}
        >
          <InputNumber min={0} placeholder={finish.isSpare === 1 ? '未用留空' : 'kg'} addonAfter="kg" />
        </Form.Item>
        <Form.Item name={['finishes', finish.uuid, 'scrapWeight']} label="报废/损耗">
          <InputNumber min={0} placeholder="kg" addonAfter="kg" />
        </Form.Item>
        <Form.Item name={['finishes', finish.uuid, 'isRemain']} label="属性">
          <Select options={remainOptions} />
        </Form.Item>
        <Form.Item name={['finishes', finish.uuid, 'isAbnormal']} label="异常">
          <Select options={abnormalOptions} />
        </Form.Item>
        <Form.Item name={['finishes', finish.uuid, 'abnormalType']} label="异常类型">
          <Input placeholder="破损/潮边" />
        </Form.Item>
        <Form.Item name={['finishes', finish.uuid, 'actualRemark']} label="备注">
          <Input placeholder="车间说明" />
        </Form.Item>
      </div>
    </div>
  )
}

function finishSpec(finish: FinishRoll) {
  const width = finish.finishWidth ? `${finish.finishWidth}mm` : '-'
  const weight = finish.estimateWeight ? `${finish.estimateWeight}kg` : '无预估'
  return `${finish.paperName || '-'} / ${width} / ${weight}`
}

const remainOptions = Object.entries(IS_REMAIN).map(([value, label]) => ({ value: Number(value), label }))
const abnormalOptions = [
  { value: 0, label: '否' },
  { value: 1, label: '是' },
]
