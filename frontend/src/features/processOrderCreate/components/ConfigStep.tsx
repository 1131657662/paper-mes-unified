import { useState } from 'react'
import { createConfigStepWorkspaceActions } from '../configStepCommands'
import { useConfigStepBatchSelections } from '../hooks/useConfigStepBatchSelections'
import { useConfigStepWorkspaceModel } from '../hooks/useConfigStepWorkspaceModel'
import ConfigStepLight from './ConfigStepLight'
import ConfigStepWorkspace from './ConfigStepWorkspace'
import './DraftAdditionalProcesses.css'
import './ServiceOnlyConfigEditor.css'
import './ConfigStep.css'
import type { ConfigEditorTab } from './configStepWorkspaceTypes'
import type { ConfigStepProps } from './configStepTypes'

export default function ConfigStep(props: ConfigStepProps) {
  const selections = useConfigStepBatchSelections()
  const [preferredEditor, setPreferredEditor] = useState<ConfigEditorTab>('plan')
  const model = useConfigStepWorkspaceModel({ preferredEditor, props, selections })

  if (props.rolls.length > 0 && model.configurableRolls.length === 0) {
    return (
      <ConfigStepLight
        lockedRolls={model.data.lockedRolls}
        onNext={props.onNext}
        onPrev={props.onPrev}
        rolls={props.rolls}
      />
    )
  }

  const actions = createConfigStepWorkspaceActions({
    model,
    props,
    selections,
    setPreferredEditor,
  })

  return (
    <ConfigStepWorkspace
      actions={actions}
      data={model.data}
    />
  )
}
