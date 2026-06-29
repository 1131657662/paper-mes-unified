import { Form } from 'antd'
import type { ProcessStepDTO } from '../../../api/processOrder'
import ProcessStepFormModal from '../../../components/processOrder/ProcessStepFormModal'
import type { ProcessOrderDetailVO } from '../../../types/processOrder'
import BackRecordAuthModal from './BackRecordAuthModal'
import BackRecordChangeModal from './BackRecordChangeModal'
import type { BackRecordAuthorization } from './backRecordUtils'
import { buildBackRecordRollOptions } from './backRecordRollOptions'
import type { BackRecordWorkItem } from './backRecordWorkbenchTypes'

interface Props {
  authForm: ReturnType<typeof Form.useForm<BackRecordAuthorization>>[0]
  authOpen: boolean
  changeItem: BackRecordWorkItem | null
  changeOpen: boolean
  detail: ProcessOrderDetailVO | null
  addingStep: boolean
  rollingBack: boolean
  stepFormOpen: boolean
  onAddExtraStep: (values: ProcessStepDTO) => Promise<void>
  onCancelAuth: () => void
  onCancelChange: () => void
  onCancelStep: () => void
  onOpenStep: () => void
  onRollbackToConfig: () => Promise<void>
  onSubmitAuth: () => Promise<void>
}

export default function BackRecordWorkspaceModals(props: Props) {
  return (
    <>
      <BackRecordAuthModal
        open={props.authOpen}
        form={props.authForm}
        onCancel={props.onCancelAuth}
        onSubmit={props.onSubmitAuth}
      />
      <BackRecordChangeModal
        open={props.changeOpen}
        detail={props.detail}
        item={props.changeItem}
        rollingBack={props.rollingBack}
        onCancel={props.onCancelChange}
        onAddExtraStep={props.onOpenStep}
        onRollbackToConfig={props.onRollbackToConfig}
      />
      <ProcessStepFormModal
        open={props.stepFormOpen}
        originalRolls={buildBackRecordRollOptions(props.detail?.originalRolls)}
        defaultOriginalUuid={props.changeItem?.roll?.uuid}
        extraOnly
        confirmLoading={props.addingStep}
        onCancel={props.onCancelStep}
        onOk={props.onAddExtraStep}
      />
    </>
  )
}
