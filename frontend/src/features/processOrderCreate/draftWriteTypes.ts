export interface VersionedWriteResult<T> {
  data: T
  recovered: boolean
  version: number
}
