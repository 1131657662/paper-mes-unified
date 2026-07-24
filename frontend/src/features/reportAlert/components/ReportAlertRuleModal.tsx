import { Alert, Form, Input, InputNumber, Modal, Select, Switch } from 'antd'
import type { Customer } from '../../../types/customer'
import type { Paper } from '../../../types/paper'
import { useCreateReportAlertRule } from '../hooks/useCreateReportAlertRule'
import { useUpdateReportAlertRule } from '../hooks/useUpdateReportAlertRule'
import type { ReportAlertRule, ReportAlertRuleSaveInput } from '../types'
import {
  operatorOptions,
  processOptions,
  scopeOptions,
  severityOptions,
  signalOptions,
} from './reportAlertRuleLabels'

interface FormValues extends Omit<ReportAlertRuleSaveInput, 'isEnabled'> {
  isEnabled: boolean
}

interface Props {
  customers: Customer[]
  initial?: ReportAlertRule | null
  onClose: () => void
  onSaved: () => void
  open: boolean
  papers: Paper[]
}

export default function ReportAlertRuleModal(props: Props) {
  const [form] = Form.useForm<FormValues>()
  const scopeType = Form.useWatch('scopeType', form)
  const createMutation = useCreateReportAlertRule()
  const updateMutation = useUpdateReportAlertRule()
  const saving = createMutation.isPending || updateMutation.isPending
  const submit = async (values: FormValues) => {
    const data = toInput(values, props.initial)
    if (props.initial) await updateMutation.mutateAsync({ uuid: props.initial.uuid, data })
    else await createMutation.mutateAsync(data)
    props.onSaved()
  }
  return <Modal destroyOnHidden open={props.open} width={620}
    title={props.initial ? '编辑阈值规则' : '新建阈值规则'} okText="保存规则"
    confirmLoading={saving} onCancel={props.onClose} onOk={() => form.submit()}>
    <Alert className="report-alert-rule-hint" type="info" showIcon
      message="匹配优先级：客户 > 纸张 > 工艺 > 全局默认" />
    <Form<FormValues> form={form} layout="vertical" initialValues={initialValues(props.initial)}
      onFinish={submit}>
      <div className="report-alert-rule-form-grid">
        <Form.Item className="report-alert-rule-form-span" label="规则名称" name="ruleName"
          rules={[{ required: true, message: '请输入规则名称' }, { max: 120 }]}>
          <Input placeholder="例如：重点客户损耗率预警" />
        </Form.Item>
        <Form.Item label="监控指标" name="signalCode" rules={[{ required: true }]}>
          <Select options={signalOptions} />
        </Form.Item>
        <Form.Item label="生效范围" name="scopeType" rules={[{ required: true }]}>
          <Select options={scopeOptions} />
        </Form.Item>
        <ScopeTargetField customers={props.customers} papers={props.papers} scopeType={scopeType} />
        <Form.Item label="比较方式" name="comparisonOperator" rules={[{ required: true }]}>
          <Select options={operatorOptions} />
        </Form.Item>
        <Form.Item label="阈值" name="thresholdValue" rules={[{ required: true, message: '请输入阈值' }]}>
          <InputNumber aria-label="阈值" min={0} max={100} precision={2} suffix="%"
            className="report-alert-rule-number" />
        </Form.Item>
        <Form.Item label="告警级别" name="severity" rules={[{ required: true }]}>
          <Select options={severityOptions} />
        </Form.Item>
        <Form.Item label="规则状态" name="isEnabled" valuePropName="checked">
          <Switch checkedChildren="启用" unCheckedChildren="停用" />
        </Form.Item>
      </div>
    </Form>
  </Modal>
}

function ScopeTargetField(props: Pick<Props, 'customers' | 'papers'> & { scopeType?: number }) {
  if (props.scopeType === 2) return <Form.Item label="指定客户" name="customerUuid"
    rules={[{ required: true, message: '请选择客户' }]}><Select showSearch optionFilterProp="label"
      placeholder="选择客户" options={customerOptions(props.customers)} /></Form.Item>
  if (props.scopeType === 3) return <Form.Item label="指定纸张" name="paperUuid"
    rules={[{ required: true, message: '请选择纸张' }]}><Select showSearch optionFilterProp="label"
      placeholder="选择纸张" options={paperOptions(props.papers)} /></Form.Item>
  if (props.scopeType === 4) return <Form.Item label="指定工艺" name="processType"
    rules={[{ required: true, message: '请选择工艺' }]}><Select options={processOptions} /></Form.Item>
  return null
}

function initialValues(item?: ReportAlertRule | null): FormValues {
  return {
    comparisonOperator: item?.comparisonOperator ?? 'GTE',
    customerUuid: item?.customerUuid,
    isEnabled: item?.isEnabled !== 0,
    paperUuid: item?.paperUuid,
    processType: item?.processType,
    ruleName: item?.ruleName ?? '',
    scopeType: item?.scopeType ?? 1,
    severity: item?.severity ?? 1,
    signalCode: item?.signalCode ?? 'LOSS_RATIO',
    thresholdValue: item?.thresholdValue ?? 5,
    version: item?.version,
  }
}

function toInput(values: FormValues, item?: ReportAlertRule | null): ReportAlertRuleSaveInput {
  return {
    comparisonOperator: values.comparisonOperator,
    customerUuid: values.scopeType === 2 ? values.customerUuid : undefined,
    isEnabled: values.isEnabled ? 1 : 0,
    paperUuid: values.scopeType === 3 ? values.paperUuid : undefined,
    processType: values.scopeType === 4 ? values.processType : undefined,
    ruleName: values.ruleName.trim(),
    scopeType: values.scopeType,
    severity: values.severity,
    signalCode: values.signalCode,
    thresholdValue: values.thresholdValue,
    version: item?.version,
  }
}

function customerOptions(items: Customer[]) {
  return items.map((item) => ({ label: item.customerName, value: item.uuid }))
}

function paperOptions(items: Paper[]) {
  return items.map((item) => ({ label: item.paperName, value: item.uuid }))
}
