import { Form, Input, InputNumber, Modal, Radio, Select } from 'antd'
import { useEffect } from 'react'
import type { ProcessStepDTO } from '../../api/processOrder'
import { STEP_TYPE } from '../../constants/processOrder'

interface ProcessStepFormModalProps {
  open: boolean
  originalRolls: Array<{ uuid: string; rollName: string }>
  initialValues?: ProcessStepDTO & { uuid?: string }
  defaultOriginalUuid?: string
  extraOnly?: boolean
  confirmLoading?: boolean
  onCancel: () => void
  onOk: (values: ProcessStepDTO, stepUuid?: string) => Promise<void>
}

export default function ProcessStepFormModal({
  open,
  originalRolls,
  initialValues,
  defaultOriginalUuid,
  extraOnly = false,
  confirmLoading = false,
  onCancel,
  onOk,
}: ProcessStepFormModalProps) {
  const [form] = Form.useForm<ProcessStepDTO>()
  const stepType = Form.useWatch('stepType', form)
  const isEditMode = !!initialValues?.uuid

  useEffect(() => {
    if (open) {
      if (initialValues) {
        // 编辑模式：填充现有数据
        form.setFieldsValue(initialValues)
      } else {
        // 新增模式：重置并设置默认值
        form.resetFields()
        form.setFieldsValue({
          originalUuid: defaultOriginalUuid,
          isMain: 0, // 默认追加工序
          stepType: 1, // 默认锯纸
        })
      }
    }
  }, [open, initialValues, defaultOriginalUuid, form])

  const handleOk = async () => {
    try {
      const values = await form.validateFields()
      await onOk(extraOnly ? { ...values, isMain: 0 } : values, initialValues?.uuid)
      onCancel()
    } catch {
      // 表单验证失败或API错误
    }
  }

  return (
    <Modal
      title={extraOnly ? '记录追加工序' : isEditMode ? '编辑工序' : '新增工序'}
      open={open}
      onCancel={onCancel}
      onOk={handleOk}
      confirmLoading={confirmLoading}
      width={640}
      destroyOnHidden
    >
      <Form form={form} layout="vertical" preserve={false}>
        <Form.Item
          label="原纸卷"
          name="originalUuid"
          rules={[{ required: true, message: '请选择原纸卷' }]}
        >
          <Select
            placeholder="请选择原纸卷"
            disabled={isEditMode}
            options={originalRolls.map((r) => ({
              label: r.rollName,
              value: r.uuid,
            }))}
          />
        </Form.Item>

        <Form.Item
          label="工序类型"
          name="stepType"
          rules={[{ required: true, message: '请选择工序类型' }]}
        >
          <Radio.Group>
            <Radio value={1}>{STEP_TYPE[1]}</Radio>
            <Radio value={2}>{STEP_TYPE[2]}</Radio>
          </Radio.Group>
        </Form.Item>

        <Form.Item label="工序名称" name="stepName">
          <Input placeholder="工序自定义名称（可选）" maxLength={50} />
        </Form.Item>

        {!extraOnly && (
          <Form.Item
            label="工序标识"
            name="isMain"
            rules={[{ required: true, message: '请选择工序标识' }]}
          >
            <Radio.Group>
              <Radio value={1}>主工艺</Radio>
              <Radio value={0}>追加工序</Radio>
            </Radio.Group>
          </Form.Item>
        )}

        {stepType === 1 && (
          <>
            <Form.Item label="锯纸刀数" name="knifeCount">
              <InputNumber
                placeholder="实际加工刀数"
                min={0}
                precision={0}
                style={{ width: '100%' }}
              />
            </Form.Item>
            <Form.Item label="锯纸单价（元/刀）" name="unitPrice">
              <InputNumber
                placeholder="本工序单价"
                min={0}
                precision={2}
                style={{ width: '100%' }}
              />
            </Form.Item>
          </>
        )}

        {stepType === 2 && (
          <>
            <Form.Item label="加工吨位（吨）" name="processWeight">
              <InputNumber
                placeholder="复卷加工吨位"
                min={0}
                precision={3}
                style={{ width: '100%' }}
              />
            </Form.Item>
            <Form.Item label="复卷单价（元/吨）" name="unitPrice">
              <InputNumber
                placeholder="本工序单价"
                min={0}
                precision={2}
                style={{ width: '100%' }}
              />
            </Form.Item>
          </>
        )}

        <Form.Item label="备注" name="remark">
          <Input.TextArea
            placeholder="工序备注、异常说明"
            maxLength={255}
            rows={3}
            showCount
          />
        </Form.Item>
      </Form>
    </Modal>
  )
}
