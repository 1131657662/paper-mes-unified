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
import { reconcileProcessConfigAfterModeChange } from '../processConfigReconciliation'
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
    let version = state.orderUuid ? state.getDraftVersion() : 1
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
    const expectedVersion = state.getDraftVersion()
    if (state.rolls.some((roll) => !isRollReadyForSave(roll))) {
      message.warning('请补全品名、克重、门幅、件数和单重；直发卷还必须填写母卷号')
      return false
    }
    const rollsWithMachines = applyDefaultMachinesToRolls(state.rolls, machines)
    const uuids = await replaceRolls({
      uuid: state.orderUuid,
      rolls: rollsWithMachines.map(toRollDto),
      expectedVersion,
    })
    const nextVersion = expectedVersion + 1
    state.setDraftVersion(nextVersion)
    resetPlanStateAfterRollSave(
      state,
      attachSavedUuids(rollsWithMachines, uuids),
      defaultPlanOptions,
      machines,
    )
    await moveToStep(2, state.orderUuid, nextVersion)
    return true
  }

  const handleProcessNext = async () => {
    if (!state.orderUuid) return false
    const expectedVersion = state.getDraftVersion()
    const expectedRollsRevision = state.getRollsRevision()
    const rollsWithMachines = applyDefaultMachinesToRolls(state.rolls, machines)
    const nextConfig = reconcileProcessConfigAfterModeChange({
      configuredPlanIds: state.configuredPlanIds,
      defaultPlanOptions,
      machines,
      plans: state.plans,
      previews: state.previews,
      routePreviews: state.routePreviews,
      routes: state.routes,
      rolls: rollsWithMachines,
    })
    const saveResult = await saveRollProcesses({
      orderUuid: state.orderUuid,
      dto: {
        expectedVersion,
        rolls: rollsWithMachines.filter((roll) => roll.uuid).map((roll) => ({
          originalUuid: roll.uuid!,
          processMode: roll.processMode ?? 1,
          mainStepType: roll.mainStepType,
          machineUuid: roll.machineUuid,
        })),
      },
    })
    const nextVersion = saveResult.version
    state.setDraftVersion(nextVersion)
    if (state.getRollsRevision() !== expectedRollsRevision) {
      message.warning('保存期间检测到新的加工方式修改，已保留当前内容，请再次点击下一步保存')
      return false
    }
    state.setRolls(rollsWithMachines)
    state.setPlans(nextConfig.plans)
    state.setConfiguredPlanIds(nextConfig.configuredPlanIds)
    state.setPreviews(nextConfig.previews)
    state.setRoutes(nextConfig.routes)
    state.setRoutePreviews(nextConfig.routePreviews)
    state.setSelectedId(rollsWithMachines.find((roll) => roll.processMode !== 3)?.localId
      ?? rollsWithMachines[0]?.localId)
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
  machines: Machine[],
) {
  state.setRolls(savedRolls)
  state.setPlans(plansForRolls(savedRolls, {}, defaultPlanOptions, machines))
  state.setConfiguredPlanIds([])
  state.setPreviews({})
  state.setRoutes({})
  state.setRoutePreviews({})
  state.setSelectedId(savedRolls[0]?.localId)
}
