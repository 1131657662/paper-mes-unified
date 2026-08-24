export const PROCESS_AI_DEFAULT_IDS = {
  rewindFinishCore3Inch: 'REWIND_FINISH_CORE_3_INCH',
} as const

export interface ProcessAiDefaultNotice {
  id: string
  message: string
  description: string
}

const DEFAULT_NOTICES: Record<string, ProcessAiDefaultNotice> = {
  [PROCESS_AI_DEFAULT_IDS.rewindFinishCore3Inch]: {
    id: PROCESS_AI_DEFAULT_IDS.rewindFinishCore3Inch,
    message: '普通复卷未指定成品纸芯，系统默认按 3 英寸处理，请确认',
    description: '确认应用时将同时确认该默认值；如需修改，请先使用结构化修正调整成品纸芯。',
  },
}

export function buildProcessAiDefaultNotices(ids: readonly string[] | undefined) {
  return (ids ?? []).map((id) => DEFAULT_NOTICES[id]).filter(isNotice)
}

function isNotice(value: ProcessAiDefaultNotice | undefined): value is ProcessAiDefaultNotice {
  return value !== undefined
}
