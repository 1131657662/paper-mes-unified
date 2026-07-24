import { Form, Input, Modal, Select, Switch } from 'antd'
import { useCreateReportSavedView } from '../hooks/useCreateReportSavedView'
import { useUpdateReportSavedView } from '../hooks/useUpdateReportSavedView'
import type { ReportSavedView, ReportSavedViewSaveInput } from '../types'
import type { ReportQuery } from '../../../types/report'
import { explorerDimensions, explorerMetricDefaults } from '../../../pages/report/reportExplorerModel'

interface Props {
  baseQuery: ReportQuery
  initial?: ReportSavedView | null
  defaults?: Partial<Values>
  open: boolean
  onClose: () => void
  onSaved: () => void
}
interface Values { viewName: string; reportPath: string; dimensionCode?: string; metricCodes: string[]; isDefault: boolean }

const pathOptions = [
  ['overview', '经营总览'], ['production', '生产分析'], ['quality-loss', '质量与损耗'], ['settlement', '结算与应收'],
  ['collection', '回款分析'], ['inventory', '库存流转'], ['delivery', '出库分析'], ['explorer', '多维分析'],
].map(([value, label]) => ({ value: `/reports/${value}`, label }))
const metricOptions = [
  ['order_count', '加工单数'], ['original_roll_count', '原卷数'], ['finish_roll_count', '成品卷数'],
  ['original_weight_kg', '原纸重量'], ['finish_weight_kg', '成品重量'], ['loss_weight_kg', '损耗重量'],
  ['loss_ratio_pct', '损耗率'], ['knife_count', '刀数'], ['saw_amount', '锯纸费'], ['rewind_amount', '复卷费'],
  ['process_amount', '加工费'], ['extra_amount', '附加费'], ['total_amount', '应收合计'], ['settled_amount', '已结算应收'],
  ['pending_settle_amount', '待结算应收'], ['received_amount', '已收金额'], ['cash_received_amount', '现金到账'],
].map(([value, label]) => ({ value, label: `${label} (${value})` }))

export default function ReportSavedViewModal(props: Props) {
  const [form] = Form.useForm<Values>()
  const create = useCreateReportSavedView()
  const update = useUpdateReportSavedView()
  const saving = create.isPending || update.isPending
  const submit = async (values: Values) => {
    const data: ReportSavedViewSaveInput = { ...values, isDefault: values.isDefault ? 1 : 0, reportQuery: props.initial?.reportQuery ?? props.baseQuery,
      dimensionCode: values.reportPath.endsWith('/explorer') ? values.dimensionCode as ReportSavedViewSaveInput['dimensionCode'] : undefined,
      metricCodes: values.metricCodes }
    if (props.initial) await update.mutateAsync({ uuid: props.initial.uuid, data: { ...data, version: props.initial.version } })
    else await create.mutateAsync(data)
    props.onSaved()
  }
  return <Modal open={props.open} destroyOnHidden title={props.initial ? '编辑保存视图' : '新建保存视图'} okText="保存"
    confirmLoading={saving} onCancel={props.onClose} onOk={() => form.submit()}>
    <Form form={form} layout="vertical" initialValues={initialValues(props.initial, props.defaults)} onFinish={submit}>
      <Form.Item label="视图名称" name="viewName" rules={[{ required: true }, { max: 100 }]}><Input aria-label="视图名称" placeholder="例如：重点客户月度损耗" /></Form.Item>
      <Form.Item label="打开页面" name="reportPath" rules={[{ required: true }]}><Select aria-label="打开页面" options={pathOptions} /></Form.Item>
      <Form.Item label="多维分组" name="dimensionCode"><Select aria-label="多维分组" allowClear options={explorerDimensions} /></Form.Item>
      <Form.Item label="展示指标" name="metricCodes" rules={[{ required: true, type: 'array', min: 1, max: 8 }]}>
        <Select aria-label="展示指标" mode="multiple" maxCount={8} maxTagCount="responsive" options={metricOptions} />
      </Form.Item>
      <Form.Item label="设为默认视图" name="isDefault" valuePropName="checked"><Switch aria-label="设为默认视图" /></Form.Item>
    </Form>
  </Modal>
}

function initialValues(item?: ReportSavedView | null, defaults?: Partial<Values>): Values {
  return {
    viewName: item?.viewName ?? defaults?.viewName ?? '',
    reportPath: item?.reportPath ?? defaults?.reportPath ?? '/reports/explorer',
    dimensionCode: item?.dimensionCode ?? defaults?.dimensionCode ?? 'customer',
    metricCodes: item?.metricCodes ?? defaults?.metricCodes ?? explorerMetricDefaults,
    isDefault: item?.isDefault === 1 || defaults?.isDefault === true,
  }
}
