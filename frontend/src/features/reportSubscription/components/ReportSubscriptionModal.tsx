import { Alert, Form, Input, Modal, Select, Switch, TimePicker } from 'antd'
import dayjs, { type Dayjs } from 'dayjs'
import type { ReportMetricReleaseSummaryVO, ReportQuery, ReportSourcePath } from '../../../types/report'
import { useCreateReportSubscription } from '../hooks/useCreateReportSubscription'
import { useUpdateReportSubscription } from '../hooks/useUpdateReportSubscription'
import type {
  ReportPeriodPolicy,
  ReportScheduleType,
  ReportSubscription,
  ReportSubscriptionRecipient,
  ReportSubscriptionSaveInput,
} from '../types'
import { periodOptions, scheduleOptions, weekDayOptions } from './reportSubscriptionLabels'

interface FormValues {
  subscriptionName: string
  scheduleType: ReportScheduleType
  executionTime: Dayjs
  weekDay?: number
  monthDay?: number
  periodPolicy: ReportPeriodPolicy
  releasePolicy: 1 | 2
  pinnedReleaseUuid?: string
  recipientUuids: string[]
  isEnabled: boolean
  useCurrentQuery: boolean
}

interface Props {
  candidates: ReportSubscriptionRecipient[]
  currentQuery: ReportQuery
  initial?: ReportSubscription | null
  onClose: () => void
  onSaved: () => void
  open: boolean
  reportPath: ReportSourcePath
  releases: ReportMetricReleaseSummaryVO[]
}

export default function ReportSubscriptionModal(props: Props) {
  const [form] = Form.useForm<FormValues>()
  const scheduleType = Form.useWatch('scheduleType', form)
  const releasePolicy = Form.useWatch('releasePolicy', form)
  const createMutation = useCreateReportSubscription()
  const updateMutation = useUpdateReportSubscription()
  const saving = createMutation.isPending || updateMutation.isPending
  const submit = async (values: FormValues) => {
    const data = toInput(values, props)
    if (props.initial) await updateMutation.mutateAsync({ uuid: props.initial.uuid, data })
    else await createMutation.mutateAsync(data)
    props.onSaved()
  }
  return (
    <Modal destroyOnHidden open={props.open} title={props.initial ? '编辑报表订阅' : '新建报表订阅'}
      width={640} confirmLoading={saving} okText="保存订阅" onCancel={props.onClose}
      onOk={() => form.submit()}>
      <Form<FormValues> form={form} layout="vertical" initialValues={initialValues(props)} onFinish={submit}>
        <Alert className="report-subscription-snapshot" type="info" showIcon
          message={snapshotText(props.currentQuery)} />
        <div className="report-subscription-form-grid">
          <Form.Item className="report-subscription-form-span" label="订阅名称" name="subscriptionName"
            rules={[{ required: true, message: '请输入订阅名称' }, { max: 100 }]}>
            <Input placeholder="例如：昨日加工日报" />
          </Form.Item>
          <Form.Item label="执行周期" name="scheduleType" rules={[{ required: true }]}>
            <Select options={scheduleOptions} />
          </Form.Item>
          <Form.Item label="执行时间" name="executionTime" rules={[{ required: true }]}>
            <TimePicker format="HH:mm" minuteStep={5} />
          </Form.Item>
          {scheduleType === 2 && <Form.Item label="执行星期" name="weekDay" rules={[{ required: true }]}>
            <Select options={weekDayOptions} />
          </Form.Item>}
          {scheduleType === 3 && <Form.Item label="每月执行日" name="monthDay" rules={[{ required: true }]}>
            <Select options={Array.from({ length: 28 }, (_, index) => ({ label: `${index + 1} 日`, value: index + 1 }))} />
          </Form.Item>}
          <Form.Item label="数据周期" name="periodPolicy" rules={[{ required: true }]}>
            <Select options={periodOptions} />
          </Form.Item>
          <Form.Item label="指标口径" name="releasePolicy" rules={[{ required: true }]}>
            <Select options={releasePolicyOptions} />
          </Form.Item>
          <PinnedReleaseField policy={releasePolicy} releases={props.releases} />
          <Form.Item className="report-subscription-form-span" label="接收人" name="recipientUuids"
            rules={[{ required: true, message: '请选择接收人' }]}>
            <Select mode="multiple" optionFilterProp="label" options={recipientOptions(props.candidates)} />
          </Form.Item>
          {props.initial && <Form.Item label="筛选范围" name="useCurrentQuery" valuePropName="checked">
            <Switch checkedChildren="当前筛选" unCheckedChildren="原筛选" />
          </Form.Item>}
          <Form.Item label="订阅状态" name="isEnabled" valuePropName="checked">
            <Switch checkedChildren="启用" unCheckedChildren="停用" />
          </Form.Item>
        </div>
      </Form>
    </Modal>
  )
}

function initialValues(props: Props): FormValues {
  const item = props.initial
  return {
    subscriptionName: item?.subscriptionName ?? '',
    scheduleType: item?.scheduleType ?? 1,
    executionTime: dayjs(`2000-01-01T${item?.executionTime ?? '08:00:00'}`),
    weekDay: item?.weekDay,
    monthDay: item?.monthDay,
    periodPolicy: item?.periodPolicy ?? 1,
    releasePolicy: item?.releasePolicy ?? 1,
    pinnedReleaseUuid: item?.pinnedReleaseUuid,
    recipientUuids: item?.recipients.map((recipient) => recipient.uuid)
      ?? (props.candidates.length === 1 ? [props.candidates[0]!.uuid] : []),
    isEnabled: item?.isEnabled !== 0,
    useCurrentQuery: false,
  }
}

function toInput(values: FormValues, props: Props): ReportSubscriptionSaveInput {
  const query = props.initial && !values.useCurrentQuery ? props.initial.reportQuery : props.currentQuery
  return {
    subscriptionName: values.subscriptionName.trim(),
    reportPath: props.initial && !values.useCurrentQuery ? props.initial.reportPath : props.reportPath,
    scheduleType: values.scheduleType,
    executionTime: values.executionTime.format('HH:mm:ss'),
    weekDay: values.scheduleType === 2 ? values.weekDay : undefined,
    monthDay: values.scheduleType === 3 ? values.monthDay : undefined,
    timezone: 'Asia/Shanghai',
    reportQuery: query,
    periodPolicy: values.periodPolicy,
    releasePolicy: values.releasePolicy,
    pinnedReleaseUuid: values.releasePolicy === 2 ? values.pinnedReleaseUuid : undefined,
    isEnabled: values.isEnabled ? 1 : 0,
    recipientUuids: values.recipientUuids,
    version: props.initial?.version,
  }
}

const releasePolicyOptions = [
  { label: '跟随最新已发布口径', value: 1 },
  { label: '固定历史发布包', value: 2 },
] satisfies Array<{ label: string; value: 1 | 2 }>

function PinnedReleaseField(props: { policy?: 1 | 2; releases: ReportMetricReleaseSummaryVO[] }) {
  if (props.policy !== 2) return null
  const options = props.releases.filter((item) => item.releaseStatus !== 1).map((item) => ({
    label: `${item.releaseName} · ${item.releaseCode}${item.releaseStatus === 3 ? '（已停用）' : ''}`,
    value: item.releaseUuid,
  }))
  return <Form.Item label="固定发布包" name="pinnedReleaseUuid"
    rules={[{ required: true, message: '请选择固定发布包' }]}>
    <Select showSearch optionFilterProp="label" options={options} placeholder="选择已发布或历史发布包" />
  </Form.Item>
}

function recipientOptions(candidates: ReportSubscriptionRecipient[]) {
  return candidates.map((item) => ({ label: `${item.displayName} (${item.username})`, value: item.uuid }))
}

function snapshotText(query: ReportQuery) {
  const dates = query.dateFrom && query.dateTo ? `${query.dateFrom} 至 ${query.dateTo}` : '全部日期'
  return `筛选快照：${dates}`
}
