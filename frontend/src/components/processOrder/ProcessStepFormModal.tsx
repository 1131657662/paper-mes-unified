import { Alert, Button, Modal } from 'antd'
import type { ProcessStepDTO } from '../../api/processOrder'
import type { CustomerProcessPrice } from '../../types/customer'
import ProcessStepFormFields from './ProcessStepFormFields'
import { useProcessStepFormState } from './useProcessStepFormState'

interface Props {
  open: boolean
  originalRolls: Array<{ uuid: string; rollName: string }>
  initialValues?: ProcessStepDTO & { uuid?: string }
  defaultOriginalUuid?: string
  defaultOriginalUuids?: string[]
  batchMode?: boolean
  extraOnly?: boolean
  confirmLoading?: boolean
  customerPrices?: CustomerProcessPrice[]
  onCancel: () => void
  onOk: (values: ProcessStepDTO, stepUuid?: string) => Promise<void>
  onBatchOk?: (values: ProcessStepDTO, originalUuids: string[]) => Promise<void>
}

export default function ProcessStepFormModal(props: Props) {
  const extraOnly = props.extraOnly ?? false
  const editMode = !!props.initialValues?.uuid
  const state = useProcessStepFormState({
    initialValues: props.initialValues,
    defaultOriginalUuid: props.defaultOriginalUuid,
    defaultOriginalUuids: props.defaultOriginalUuids,
    batchMode: props.batchMode,
    extraOnly,
    customerPrices: props.customerPrices,
    onCancel: props.onCancel,
    onOk: props.onOk,
    onBatchOk: props.onBatchOk,
  })
  return (
    <Modal
      title={modalTitle({ batchMode: props.batchMode, editMode, extraOnly })}
      open={props.open}
      onCancel={props.onCancel}
      onOk={state.submit}
      afterClose={() => state.form.resetFields()}
      confirmLoading={props.confirmLoading}
      okButtonProps={{ disabled: state.isLoading || !state.selectedCatalog }}
      styles={{ body: { maxHeight: 'calc(100vh - 200px)', overflowY: 'auto', paddingRight: 8 } }}
      width={640}
      centered
      forceRender
    >
      {state.isError && (
        <Alert type="error" showIcon message="工艺目录加载失败"
          action={<Button size="small" onClick={() => state.refetch()}>重试</Button>} />
      )}
      {!state.isLoading && !state.isError && state.catalogs?.length === 0 && (
        <Alert type="warning" showIcon message="暂无启用的工艺目录" />
      )}
      <ProcessStepFormFields
        state={state}
        originalRolls={props.originalRolls}
        editMode={editMode}
        extraOnly={extraOnly}
        batchMode={props.batchMode}
      />
    </Modal>
  )
}

function modalTitle(options: { batchMode?: boolean; editMode: boolean; extraOnly: boolean }) {
  if (options.batchMode) return '批量添加附加工艺'
  if (options.extraOnly) return options.editMode ? '编辑附加工艺' : '新增附加工艺'
  return options.editMode ? '编辑工序' : '新增费用工序'
}
