import { Collapse, Form, Input, Select } from 'antd'
import ProcessCatalogCapabilityStrip from './ProcessCatalogCapabilityStrip'
import ProcessStepPricingFields from './ProcessStepPricingFields'
import ProcessStepMachineField from './ProcessStepMachineField'
import type { ProcessStepFormValues } from './processStepCatalogModel'
import type { useProcessStepFormState } from './useProcessStepFormState'

interface Props {
  state: ReturnType<typeof useProcessStepFormState>
  originalRolls: Array<{ uuid: string; rollName: string }>
  editMode: boolean
  extraOnly: boolean
  batchMode?: boolean
  compact?: boolean
  onValuesChange?: (changed: Partial<ProcessStepFormValues>) => void
}

export default function ProcessStepFormFields({
  state, originalRolls, editMode, extraOnly, batchMode, compact, onValuesChange,
}: Props) {
  const singleRoll = originalRolls.length === 1 ? originalRolls[0] : undefined
  return (
    <Form<ProcessStepFormValues>
      form={state.form}
      layout="vertical"
      preserve={false}
      initialValues={state.initialValues}
      onValuesChange={onValuesChange ?? state.change}
      className={compact ? 'process-step-form process-step-form--compact' : 'process-step-form'}
    >
      {batchMode
        ? <BatchRollField originalRolls={originalRolls} />
        : singleRoll
          ? <SingleRollField roll={singleRoll} compact={compact} />
          : <RollSelectField editMode={editMode} originalRolls={originalRolls} />}
      <Form.Item label="工序类型" name="stepType" rules={[{ required: true, message: '请选择工序类型' }]}>
        <Select
          showSearch
          loading={state.isLoading}
          disabled={state.isLoading || state.isError}
          optionFilterProp="label"
          placeholder={processTypePlaceholder(state)}
          options={state.catalogs?.map((catalog) => ({ label: catalog.name, value: catalog.stepType }))}
        />
      </Form.Item>
      {state.selectedCatalog && (
        <ProcessStepMachineField
          required={state.selectedCatalog.producesInventoryOutput}
          stepType={state.selectedCatalog.stepType}
          machines={state.machines}
          loading={state.isLoadingMachines}
        />
      )}
      {state.selectedCatalog && <div className="process-step-form__capabilities">
        <ProcessCatalogCapabilityStrip catalog={state.selectedCatalog} />
      </div>}
      <ProcessStepPricingFields
        catalog={state.selectedCatalog}
        extraOnly={extraOnly}
        billingMode={state.billingMode}
        billingBasis={state.billingBasis}
        batchMode={batchMode}
        compact={compact}
      />
      {compact ? <OptionalFields /> : <ExpandedOptionalFields />}
    </Form>
  )
}

function SingleRollField({ roll, compact }: {
  roll: Props['originalRolls'][number]
  compact?: boolean
}) {
  return (
    <>
      <Form.Item name="originalUuid" hidden rules={[{ required: true, message: '请选择原纸卷' }]}>
        <Input />
      </Form.Item>
      {!compact && <Form.Item label="当前母卷">
        <Input readOnly tabIndex={-1} value={roll.rollName} />
      </Form.Item>}
    </>
  )
}

function OptionalFields() {
  return (
    <Collapse ghost size="small" className="process-step-form__more" items={[{
      key: 'optional',
      label: '更多设置（名称、备注）',
      children: <div className="process-step-form__optional"><ExpandedOptionalFields /></div>,
    }]} />
  )
}

function ExpandedOptionalFields() {
  return (
    <>
      <Form.Item label="工序名称" name="stepName">
        <Input placeholder="自定义名称（可选）" maxLength={50} />
      </Form.Item>
      <Form.Item label="备注" name="remark">
        <Input.TextArea placeholder="工序备注、异常说明" maxLength={255} rows={2} showCount />
      </Form.Item>
    </>
  )
}

function RollSelectField({ editMode, originalRolls }: Pick<Props, 'editMode' | 'originalRolls'>) {
  return (
    <Form.Item label="原纸卷" name="originalUuid" rules={[{ required: true, message: '请选择原纸卷' }]}>
      <Select placeholder="选择原纸卷" disabled={editMode}
        options={originalRolls.map((roll) => ({ label: roll.rollName, value: roll.uuid }))} />
    </Form.Item>
  )
}

function BatchRollField({ originalRolls }: Pick<Props, 'originalRolls'>) {
  return (
    <Form.Item label="应用母卷" name="originalUuids" extra="可一次选择多卷；每卷生成独立工序，后续仍可单独调整。"
      rules={[{ required: true, type: 'array', min: 1, message: '至少选择一卷母卷' }]}>
      <Select mode="multiple" maxTagCount="responsive" optionFilterProp="label" placeholder="选择要应用的母卷"
        options={originalRolls.map((roll) => ({ label: roll.rollName, value: roll.uuid }))} />
    </Form.Item>
  )
}

function processTypePlaceholder(state: ReturnType<typeof useProcessStepFormState>) {
  if (state.isError) return '目录加载失败'
  if (!state.isLoading && state.catalogs?.length === 0) return '暂无可用工艺'
  return '选择工艺'
}
