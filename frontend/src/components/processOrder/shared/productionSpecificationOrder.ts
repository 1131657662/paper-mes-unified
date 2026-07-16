import type {
  FinishProductionVO,
  FinishSourceVO,
  OriginalRoll,
  RollProductionVO,
} from '../../../types/processOrder'

export interface ProductionSpecificationOrderValue {
  gramWeight?: number
  paperName?: string
  pieceWeight?: number
  width?: number
}

const paperNameCollator = new Intl.Collator('zh-CN', {
  numeric: true,
  sensitivity: 'base',
})

export function compareProductionSpecifications(
  left: ProductionSpecificationOrderValue,
  right: ProductionSpecificationOrderValue,
): number {
  return compareText(left.paperName, right.paperName)
    || compareNumber(left.gramWeight, right.gramWeight)
    || compareNumber(left.width, right.width)
    || compareNumber(left.pieceWeight, right.pieceWeight)
}

export function compareRollProductions(left: RollProductionVO, right: RollProductionVO): number {
  return compareProductionSpecifications(rollOrderValue(left), rollOrderValue(right))
    || compareNumber(left.pieceNum, right.pieceNum)
    || compareText(rollIdentifier(left), rollIdentifier(right))
}

export function compareOriginalRolls(left: OriginalRoll, right: OriginalRoll): number {
  return compareProductionSpecifications(originalRollOrderValue(left), originalRollOrderValue(right))
    || compareNumber(left.pieceNum, right.pieceNum)
    || compareText(left.rollNo ?? left.extraNo ?? left.uuid, right.rollNo ?? right.extraNo ?? right.uuid)
}

export function compareFinishProductions(left: FinishProductionVO, right: FinishProductionVO): number {
  return trimRank(left) - trimRank(right)
    || compareProductionSpecifications(finishOrderValue(left), finishOrderValue(right))
    || compareNumber(left.rowSort, right.rowSort)
    || compareText(left.finishRollNo ?? left.uuid, right.finishRollNo ?? right.uuid)
}

export function compareFinishSources(left: FinishSourceVO, right: FinishSourceVO): number {
  return compareProductionSpecifications(sourceOrderValue(left), sourceOrderValue(right))
    || compareNumber(left.rowSort, right.rowSort)
    || compareText(sourceIdentifier(left), sourceIdentifier(right))
}

function rollOrderValue(roll: RollProductionVO): ProductionSpecificationOrderValue {
  return {
    paperName: roll.paperName,
    gramWeight: roll.actualGramWeight ?? roll.gramWeight,
    width: roll.actualWidth ?? roll.originalWidth,
    pieceWeight: roll.rollWeight ?? actualPieceWeight(roll.actualWeight, roll.pieceNum),
  }
}

function originalRollOrderValue(roll: OriginalRoll): ProductionSpecificationOrderValue {
  return {
    paperName: roll.paperName,
    gramWeight: roll.actualGramWeight ?? roll.gramWeight,
    width: roll.actualWidth ?? roll.originalWidth,
    pieceWeight: roll.rollWeight ?? actualPieceWeight(roll.actualWeight, roll.pieceNum),
  }
}

function finishOrderValue(finish: FinishProductionVO): ProductionSpecificationOrderValue {
  return {
    paperName: finish.paperName,
    gramWeight: finish.gramWeight,
    width: finish.finishWidth,
    pieceWeight: finish.actualWeight ?? finish.estimateWeight,
  }
}

function sourceOrderValue(source: FinishSourceVO): ProductionSpecificationOrderValue {
  return {
    paperName: source.paperName,
    gramWeight: source.actualGramWeight ?? source.gramWeight,
    width: source.actualWidth ?? source.originalWidth,
    pieceWeight: source.rollWeight ?? actualPieceWeight(source.actualWeight, source.pieceNum),
  }
}

function actualPieceWeight(weight?: number, pieceNum?: number) {
  if (weight == null) return undefined
  return weight / Math.max(pieceNum ?? 1, 1)
}

function trimRank(finish: FinishProductionVO) {
  return finish.isRemain === 1 ? 1 : 0
}

function rollIdentifier(roll: RollProductionVO) {
  return roll.rollNo ?? roll.extraNo ?? roll.originalUuid
}

function sourceIdentifier(source: FinishSourceVO) {
  return source.rollNo ?? source.extraNo ?? source.originalUuid
}

function compareText(left?: string, right?: string) {
  if (!left) return right ? 1 : 0
  if (!right) return -1
  return paperNameCollator.compare(left, right)
}

function compareNumber(left?: number, right?: number) {
  if (left == null) return right == null ? 0 : 1
  if (right == null) return -1
  return left - right
}
