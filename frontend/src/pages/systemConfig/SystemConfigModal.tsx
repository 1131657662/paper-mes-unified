import { Form, Input, InputNumber, Modal, Select, type FormInstance } from 'antd'
import type { ConfigItem, ConfigItemSaveDTO, DictItem, DictItemSaveDTO } from '../../types/systemConfig'
import { statusOptions, valueTypeOptions } from './systemConfigDisplay'
import { getConfigValueError } from '../../features/systemConfig/configValueValidation'

interface DictModalProps {
  item?: DictItem
  open: boolean
  submitting: boolean
  onCancel: () => void
  onSubmit: (values: DictItemSaveDTO) => Promise<void>
  onDirtyChange?: (dirty: boolean) => void
}

interface ConfigModalProps {
  item?: ConfigItem
  open: boolean
  submitting: boolean
  onCancel: () => void
  onSubmit: (values: ConfigItemSaveDTO) => Promise<void>
  onDirtyChange?: (dirty: boolean) => void
}

export function DictItemModal({ item, onCancel, onSubmit, onDirtyChange, open, submitting }: DictModalProps) {
  const [form] = Form.useForm<DictItemSaveDTO>()
  return (
    <Modal
      title={item ? '编辑字典项' : '新增字典项'}
      open={open}
      width={680}
      destroyOnHidden
      confirmLoading={submitting}
      onCancel={onCancel}
      onOk={() => form.submit()}
    >
      <Form className="mes-modal-form" form={form} initialValues={item ? toDictValues(item) : dictDefaults} layout="vertical" onFieldsChange={() => onDirtyChange?.(form.isFieldsTouched())} onFinish={onSubmit}>
        <div className="mes-form-grid">
          <Form.Item name="dictType" label="字典分类" rules={[{ required: true, message: '请输入字典分类' }, { max: 50, message: '字典分类不能超过50个字符' }]}>
            <Input maxLength={50} placeholder="如 settle_type" />
          </Form.Item>
          <Form.Item name="dictName" label="分类名称" rules={[{ required: true, message: '请输入分类名称' }, { max: 80, message: '分类名称不能超过80个字符' }]}>
            <Input maxLength={80} placeholder="如 结算方式" />
          </Form.Item>
          <Form.Item name="itemCode" label="字典编码" rules={[{ required: true, message: '请输入字典编码' }, { max: 50, message: '字典编码不能超过50个字符' }]}>
            <Input maxLength={50} placeholder="如 monthly" />
          </Form.Item>
          <Form.Item name="itemName" label="字典名称" rules={[{ required: true, message: '请输入字典名称' }, { max: 80, message: '字典名称不能超过80个字符' }]}>
            <Input maxLength={80} placeholder="如 月结" />
          </Form.Item>
          <Form.Item name="itemValue" label="兼容枚举值">
            <InputNumber placeholder="旧业务数字值" />
          </Form.Item>
          <Form.Item name="sortNo" label="排序">
            <InputNumber min={0} />
          </Form.Item>
          <Form.Item name="status" label="状态" rules={[{ required: true, message: '请选择状态' }]}>
            <Select options={statusOptions} />
          </Form.Item>
          <Form.Item className="mes-form-grid__full" name="remark" label="备注" rules={[{ max: 255, message: '备注不能超过255个字符' }]}>
            <Input.TextArea maxLength={255} rows={3} placeholder="说明该字典项的业务含义" showCount />
          </Form.Item>
        </div>
      </Form>
    </Modal>
  )
}

export function ConfigItemModal({ item, onCancel, onSubmit, onDirtyChange, open, submitting }: ConfigModalProps) {
  const [form] = Form.useForm<ConfigItemSaveDTO>()
  const metadataLocked = item?.builtIn === 1
  return (
    <Modal
      title={item ? '编辑系统参数' : '新增系统参数'}
      open={open}
      width={720}
      destroyOnHidden
      confirmLoading={submitting}
      onCancel={onCancel}
      onOk={() => form.submit()}
    >
      <Form className="mes-modal-form" form={form} initialValues={item ? toConfigValues(item) : configDefaults} layout="vertical" onFieldsChange={() => onDirtyChange?.(form.isFieldsTouched())} onFinish={onSubmit}>
        <div className="mes-form-grid">
          <Form.Item name="configGroup" label="参数分组" rules={[{ required: true, message: '请输入参数分组' }, { max: 50, message: '参数分组不能超过50个字符' }]}>
            <Input disabled={metadataLocked} maxLength={50} placeholder="如 process" />
          </Form.Item>
          <Form.Item name="configKey" label="参数键" rules={[{ required: true, message: '请输入参数键' }, { max: 80, message: '参数键不能超过80个字符' }]}>
            <Input disabled={metadataLocked} maxLength={80} placeholder="如 process.weightTolerancePercent" />
          </Form.Item>
          <Form.Item name="configName" label="参数名称" rules={[{ required: true, message: '请输入参数名称' }, { max: 80, message: '参数名称不能超过80个字符' }]}>
            <Input disabled={metadataLocked} maxLength={80} placeholder="如 回录重量误差阈值" />
          </Form.Item>
          <ConfigValueFields form={form} metadataLocked={metadataLocked} />
          <Form.Item name="unit" label="单位">
            <Input disabled={metadataLocked} maxLength={20} placeholder="如 %, 个, 条" />
          </Form.Item>
          <Form.Item name="sortNo" label="排序">
            <InputNumber disabled={metadataLocked} min={0} />
          </Form.Item>
          <Form.Item name="status" label="状态" rules={[{ required: true, message: '请选择状态' }]}>
            <Select options={statusOptions} />
          </Form.Item>
          <Form.Item className="mes-form-grid__full" name="remark" label="备注" rules={[{ max: 255, message: '备注不能超过255个字符' }]}>
            <Input.TextArea disabled={metadataLocked} maxLength={255} rows={3} placeholder="说明参数影响的业务流程" showCount />
          </Form.Item>
        </div>
      </Form>
    </Modal>
  )
}

function ConfigValueFields({ form, metadataLocked }: { form: FormInstance<ConfigItemSaveDTO>; metadataLocked: boolean }) {
  const valueType = Form.useWatch('valueType', form)
  const configKey = Form.useWatch('configKey', form)
  return (
    <>
      <Form.Item
        name="configValue"
        label="参数值"
        rules={[
          { required: true, message: '请输入参数值' },
          { max: 255, message: '参数值不能超过255个字符' },
          { validator: (_, value) => validateConfigValue(value, valueType, configKey) },
        ]}
      >
        <Input maxLength={255} placeholder={configValuePlaceholder(valueType)} />
      </Form.Item>
      <Form.Item name="valueType" label="值类型" rules={[{ required: true, message: '请选择值类型' }]}>
        <Select disabled={metadataLocked} options={valueTypeOptions} />
      </Form.Item>
    </>
  )
}

function validateConfigValue(
  value: unknown,
  valueType?: ConfigItemSaveDTO['valueType'],
  configKey?: string,
) {
  const error = getConfigValueError({ configKey, value, valueType })
  return error ? Promise.reject(new Error(error)) : Promise.resolve()
}

function configValuePlaceholder(valueType?: ConfigItemSaveDTO['valueType']) {
  if (valueType === 'number') return '请输入数字，如 3 或 2.5'
  if (valueType === 'boolean') return '请输入 true 或 false'
  return '请输入参数值'
}

const dictDefaults: DictItemSaveDTO = {
  dictName: '',
  dictType: '',
  itemCode: '',
  itemName: '',
  sortNo: 100,
  status: 1,
}

const configDefaults: ConfigItemSaveDTO = {
  configGroup: 'process',
  configKey: '',
  configName: '',
  configValue: '',
  sortNo: 100,
  status: 1,
  valueType: 'string',
}

function toDictValues(item: DictItem): DictItemSaveDTO {
  return {
    dictName: item.dictName,
    dictType: item.dictType,
    itemCode: item.itemCode,
    itemName: item.itemName,
    itemValue: item.itemValue,
    remark: item.remark,
    sortNo: item.sortNo,
    status: item.status,
  }
}

function toConfigValues(item: ConfigItem): ConfigItemSaveDTO {
  return {
    configGroup: item.configGroup,
    configKey: item.configKey,
    configName: item.configName,
    configValue: item.configValue,
    remark: item.remark,
    sortNo: item.sortNo,
    status: item.status,
    unit: item.unit,
    valueType: item.valueType,
  }
}
