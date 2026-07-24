import { Alert, Button, Form, Input, Segmented, Select, Spin } from 'antd'
import type { FormInstance } from 'antd'
import AutoCodeInput from '../../components/biz/AutoCodeInput'
import { useProcessCatalog } from '../../features/processCatalog/hooks/useProcessCatalog'
import type { MachineSaveDTO } from '../../types/machine'
import MachineCapabilityEditor from './MachineCapabilityEditor'
import { clearCapabilityDefaults } from './machineCapabilityModel'

interface Props {
  editing: boolean
  form: FormInstance<MachineSaveDTO>
  onFinish?: (values: MachineSaveDTO) => void
  onValuesChange?: () => void
}

const machineFormDefaults: Partial<MachineSaveDTO> = {
  resourceKind: 'MACHINE',
  status: 1,
}

export default function MachineProfileForm({ editing, form, onFinish, onValuesChange }: Props) {
  const {
    data: catalogs = [], isError: isCatalogError,
    isLoading: isLoadingCatalogs, refetch: refetchCatalogs,
  } = useProcessCatalog()
  const status = Form.useWatch('status', form) ?? 1
  const handleValuesChange = (changed: Partial<MachineSaveDTO>) => {
    if (changed.status === 2) {
      form.setFieldValue('capabilities', clearCapabilityDefaults(form.getFieldValue('capabilities')))
    }
    onValuesChange?.()
  }
  return (
    <Form
      className="mes-modal-form machine-profile-form"
      form={form}
      initialValues={machineFormDefaults}
      layout="vertical"
      onFinish={onFinish}
      onValuesChange={handleValuesChange}
    >
      <section className="machine-profile-form__section">
        <h3>基础信息</h3>
        <div className="mes-form-grid">
          <Form.Item name="machineCode" label="机台编码">
            <AutoCodeInput editing={editing} />
          </Form.Item>
          <Form.Item
            name="machineName"
            label="机台名称"
            rules={[{ required: true, message: '请输入机台名称' }]}
          >
            <Input placeholder="请输入机台名称" />
          </Form.Item>
          <Form.Item name="resourceKind" label="资源类型" rules={[{ required: true }]}>
            <Segmented block options={resourceKindOptions} />
          </Form.Item>
          <Form.Item name="status" label="状态">
            <Select options={statusOptions} />
          </Form.Item>
        </div>
      </section>

      <section className="machine-profile-form__section">
        <h3>工艺能力</h3>
        {isCatalogError && <Alert type="error" showIcon message="工艺目录加载失败"
          action={<Button size="small" onClick={() => refetchCatalogs()}>重试</Button>} />}
        <Spin spinning={isLoadingCatalogs}>
          <Form.Item name="capabilities" rules={[{
            required: true, type: 'array', min: 1, message: '请至少选择一项工艺能力',
          }]}>
            <MachineCapabilityEditor catalogs={catalogs} enabled={status === 1} />
          </Form.Item>
        </Spin>
      </section>

      <section className="machine-profile-form__section">
        <h3>备注</h3>
        <Form.Item name="remark" label="备注说明">
          <Input.TextArea rows={4} placeholder="记录机台能力、使用限制或维护说明" />
        </Form.Item>
      </section>
    </Form>
  )
}

const resourceKindOptions = [
  { value: 'MACHINE', label: '设备' },
  { value: 'WORKSTATION', label: '工位' },
]

const statusOptions = [
  { value: 1, label: '启用' },
  { value: 2, label: '停用' },
]
