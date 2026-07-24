import type { Machine } from '../../types/machine'
import { suggestedMachineUuid } from '../processOrderCreate/machineDefaults'
import { updateDetailRouteStagePlan, type DetailRouteFormState } from './routeConfigDetail'

export function withLastStageMachine(form: DetailRouteFormState, machines: Machine[]) {
  const stage = form.stages.at(-1)
  return stage ? withStageMachine(form, stage.id, machines) : form
}

export function withStageMachine(
  form: DetailRouteFormState,
  stageId: string,
  machines: Machine[],
) {
  const stage = form.stages.find((item) => item.id === stageId)
  if (!stage) return form
  const machineUuid = suggestedMachineUuid({ mainStepType: stage.stepType, machines })
  return updateDetailRouteStagePlan(form, stageId, { ...stage.plan, machineUuid })
}
