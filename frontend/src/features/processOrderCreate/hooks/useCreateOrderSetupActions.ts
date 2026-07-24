import { message } from 'antd'
import type { DraftOrderBaseDTO, OriginalRollImportPreviewVO } from '../../../types/processOrder'
import type { Machine } from '../../../types/machine'
import { isRollReadyForSave, plansForRolls } from '../createOrderState'
import {
  attachSavedUuids,
  normalizeBaseInfo,
  toRollDto,
  type DefaultPlanOptions,
} from '../draftMappers'
import { applyDefaultMachinesToRolls } from '../machineDefaults'
import { useCreateDraft } from './useCreateDraft'
import type { CreateOrderDraftState } from './useCreateOrderDraftState'
import { useImportPreview } from './useImportPreview'
import { useReplaceRolls } from './useReplaceRolls'
import { useSaveBaseInfo } from './useSaveBaseInfo'
import { useSaveRollProcesses } from './useSaveRollProcesses'
import type { MoveToCreateOrderStep } from './useCreateOrderStepNavigation'

interface UseCreateOrderSetupActionsOptions {
  defaultPlanOptions: DefaultPlanOptions
  machines: Machine[]
  moveToStep: MoveToCreateOrderStep
  state: CreateOrderDraftState
}

export function useCreateOrderSetupActions(options: UseCreateOrderSetupActionsOptions) {
  const { defaultPlanOptions, machines, moveToStep, state } = options
  const { mutateAsync: createDraft, isPending: creatingDraft } = useCreateDraft()
  const { mutateAsync: saveBaseInfo, isPending: savingBase } = useSaveBaseInfo()
  const { mutateAsync: replaceRolls, isPending: savingRolls } = useReplaceRolls()
  const { mutateAsync: saveRollProcesses, isPending: updatingRolls } = useSaveRollProcesses()
  const { mutateAsync: importPreview, isPending: importingRolls } = useImportPreview()

  const handleBaseNext = async (value: DraftOrderBaseDTO) => {
    const dto = normalizeBaseInfo(value)
    const uuid = state.orderUuid ?? await createDraft(dto)
    let version = state.orderUuid ? state.draftVersion : 1
    if (state.orderUuid) {
      await saveBaseInfo({ uuid: state.orderUuid, dto: { ...dto, expectedVersion: version } })
      version += 1
      state.setDraftVersion(version)
    }
    state.setOrderUuid(uuid)
    state.setBaseInfo(dto)
    await moveToStep(1, uuid, version)
    return true
  }

  const handleRollsNext = async () => {
    if (!state.orderUuid) return false
    if (state.rolls.some((roll) => !isRollReadyForSave(roll))) {
      message.warning('请补全品名和单重')
      return false
    }
    const rollsWithMachines = applyDefaultMachinesToRolls(state.rolls, machines)
    const uuids = await replaceRolls({
      uuid: state.orderUuid,
      rolls: rollsWithMachines.map(toRollDto),
      expectedVersion: state.draftVersion,
    })
    const nextVersion = state.draftVersion + 1
    state.setDraftVersion(nextVersion)
    resetPlanStateAfterRollSave(state, attachSavedUuids(rollsWithMachines, uuids), defaultPlanOptions)
    await moveToStep(2, state.orderUuid, nextVersion)
    return true
  }

  const handleProcessNext = async () => {
    if (!state.orderUuid) return false
    const rollsWithMachines = applyDefaultMachinesToRolls(state.rolls, machines)
    state.setRolls(rollsWithMachines)
    await saveRollProcesses({
      orderUuid: state.orderUuid,
      dto: {
        expectedVersion: state.draftVersion,
        rolls: rollsWithMachines.filter((roll) => roll.uuid).map((roll) => ({
          originalUuid: roll.uuid!,
          processMode: roll.processMode ?? 1,
          mainStepType: roll.mainStepType,
          machineUuid: roll.machineUuid,
        })),
      },
    })
    const nextVersion = state.draftVersion + 1
    state.setDraftVersion(nextVersion)
    state.setPlans(plansForRolls(rollsWithMachines, state.plans, defaultPlanOptions))
    state.setConfiguredPlanIds([])
    state.setPreviews({})
    state.setRoutes({})
    state.setRoutePreviews({})
    await moveToStep(3, state.orderUuid, nextVersion)
    return true
  }

  const handleImportPreview = async (file: File): Promise<OriginalRollImportPreviewVO> => {
    if (state.orderUuid) return importPreview({ uuid: state.orderUuid, file })
    message.warning('请先保存基础信息')
    return { validRows: [], errors: [{ rowNumber: 0, message: '请先保存基础信息' }] }
  }

  return {
    creatingDraft,
    savingBase,
    savingRolls: savingRolls || importingRolls,
    updatingRolls,
    handleBaseInfoChange: (value: DraftOrderBaseDTO) => state.setBaseInfo(normalizeBaseInfo(value)),
    handleBaseNext,
    handleImportPreview,
    handleProcessNext,
    handleRollsNext,
  }
}

function resetPlanStateAfterRollSave(
  state: CreateOrderDraftState,
  savedRolls: CreateOrderDraftState['rolls'],
  defaultPlanOptions: DefaultPlanOptions,
) {
  state.setRolls(savedRolls)
  state.setPlans(plansForRolls(savedRolls, {}, defaultPlanOptions))
  state.setConfiguredPlanIds([])
  state.setPreviews({})
  state.setRoutes({})
  state.setRoutePreviews({})
  state.setSelectedId(savedRolls[0]?.localId)
}
