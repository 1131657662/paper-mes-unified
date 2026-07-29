export type PlanOperationKind = 'preview' | 'save'

export interface PlanOperationToken {
  editVersion: number
  kind: PlanOperationKind
  localId: string
  requestVersion: number
}

export class PlanOperationTracker {
  private readonly editVersions = new Map<string, number>()
  private readonly requestVersions = new Map<string, number>()

  markEdited(localId: string): void {
    this.editVersions.set(localId, this.editVersion(localId) + 1)
  }

  begin(kind: PlanOperationKind, localId: string): PlanOperationToken {
    const key = requestKey(kind, localId)
    const requestVersion = (this.requestVersions.get(key) ?? 0) + 1
    this.requestVersions.set(key, requestVersion)
    return { editVersion: this.editVersion(localId), kind, localId, requestVersion }
  }

  isCurrent(token: PlanOperationToken): boolean {
    return this.editVersion(token.localId) === token.editVersion
      && this.requestVersions.get(requestKey(token.kind, token.localId)) === token.requestVersion
  }

  private editVersion(localId: string): number {
    return this.editVersions.get(localId) ?? 0
  }
}

function requestKey(kind: PlanOperationKind, localId: string): string {
  return `${kind}:${localId}`
}
