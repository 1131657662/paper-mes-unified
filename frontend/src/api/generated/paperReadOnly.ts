import { orvalRequest } from '../orvalRequest'
export interface Paper {
  createBy?: string
  createTime?: string
  extNum1?: number
  extNum2?: number
  extStr1?: string
  extStr2?: string
  gramWeight?: number
  isDeleted?: number
  paperCode?: string
  paperName: string
  paperType?: string
  remark?: string
  updateBy?: string
  updateTime?: string
  uuid: string
  version?: number
}

export interface PageResultPaper {
  current: number
  records: Paper[]
  size: number
  total: number
}

export type ListPapersParams = {
  keyword?: string
  current?: number
  size?: number
}

type SecondParameter<T extends (...args: never) => unknown> = Parameters<T>[1]

/**
 * @summary List papers
 */
export const listPapers = (
  params?: ListPapersParams,
  options?: SecondParameter<typeof orvalRequest<PageResultPaper>>,
) => {
  return orvalRequest<PageResultPaper>(
    { url: `/api/papers`, method: 'GET', params },
    options,
  )
}

/**
 * @summary Get a paper profile
 */
export const getPaper = (
  uuid: string,
  options?: SecondParameter<typeof orvalRequest<Paper>>,
) => {
  return orvalRequest<Paper>(
    { url: `/api/papers/${uuid}`, method: 'GET' },
    options,
  )
}

export type ListPapersResult = NonNullable<
  Awaited<ReturnType<typeof listPapers>>
>
export type GetPaperResult = NonNullable<Awaited<ReturnType<typeof getPaper>>>
