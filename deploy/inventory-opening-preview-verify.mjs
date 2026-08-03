import { readFileSync, statSync } from "node:fs";

const MAX_RESPONSE_BYTES = 10 * 1024 * 1024;

function fail(message) {
  process.stderr.write(`opening preview verification failed: ${message}\n`);
  process.exit(1);
}

function decimal(value, label) {
  const parsed = Number(value);
  if (!Number.isFinite(parsed)) fail(`${label} is not numeric`);
  return parsed.toFixed(3);
}

function quantity(value, label) {
  const parsed = Number(value);
  if (!Number.isFinite(parsed)) fail(`${label} is not numeric`);
  return String(parsed);
}

function readPreview(path) {
  if (statSync(path).size > MAX_RESPONSE_BYTES) fail("response exceeds 10 MiB");
  try {
    return JSON.parse(readFileSync(path, "utf8"));
  } catch {
    fail("response is not valid JSON");
  }
}

function readProjection(path) {
  const rows = readFileSync(path, "utf8").trimEnd().split(/\r?\n/).slice(1);
  const projection = new Map();
  for (const row of rows) {
    if (!row) continue;
    const fields = row.split("\t");
    if (fields.length !== 7 || fields[6] !== "OK") fail("preflight TSV contains an invalid row");
    if (projection.has(fields[0])) fail("preflight TSV contains a duplicate finish roll");
    projection.set(fields[0], {
      quantity: Number(fields[4]) > 0 ? "1" : "0",
      weight: decimal(fields[2], "projected weight"),
    });
  }
  return projection;
}

function validateLine(line, expected) {
  if (quantity(line.projectedQuantity, "projected quantity") !== expected.quantity) {
    fail("preview projected quantity differs from database projection");
  }
  if (quantity(line.openingQuantity, "opening quantity") !== expected.quantity) {
    fail("preview opening quantity differs from database projection");
  }
  if (decimal(line.projectedWeight, "projected weight") !== expected.weight
      || decimal(line.openingWeight, "opening weight") !== expected.weight) {
    fail("preview weight differs from database projection");
  }
  if (Number(line.quantityDifference) !== 0 || Number(line.weightDifference) !== 0) {
    fail("preview line reports a non-zero difference");
  }
}

function validateLines(data, projection) {
  if (!Array.isArray(data.lines)) fail("preview lines are missing");
  const seen = new Set();
  for (const line of data.lines) {
    if (!line || typeof line.finishRollUuid !== "string") fail("preview line UUID is missing");
    if (seen.has(line.finishRollUuid)) fail("preview contains a duplicate finish roll");
    const expected = projection.get(line.finishRollUuid);
    if (!expected) fail("preview contains an unknown finish roll");
    validateLine(line, expected);
    seen.add(line.finishRollUuid);
  }
  if (seen.size !== projection.size) fail("preview is missing a finish roll");
}

function validateTotals(data, projection) {
  let quantityTotal = 0;
  let weightTotal = 0;
  for (const expected of projection.values()) {
    quantityTotal += Number(expected.quantity);
    weightTotal += Number(expected.weight);
  }
  const quantityText = String(quantityTotal);
  const weightText = weightTotal.toFixed(3);
  if (quantity(data.projectedQuantityTotal, "projected quantity total") !== quantityText
      || quantity(data.openingQuantityTotal, "opening quantity total") !== quantityText) {
    fail("preview quantity totals differ from database projection");
  }
  if (decimal(data.projectedWeightTotal, "projected weight total") !== weightText
      || decimal(data.openingWeightTotal, "opening weight total") !== weightText) {
    fail("preview weight totals differ from database projection");
  }
}

function main() {
  const [, , previewPath, projectionPath, switchUuid] = process.argv;
  if (!previewPath || !projectionPath || !switchUuid) fail("expected preview, projection, and switch UUID arguments");
  const response = readPreview(previewPath);
  if (response?.code !== 200 || response?.message !== "success") fail("preview API response is not successful");
  const data = response.data;
  if (!data || data.switchUuid !== switchUuid) fail("preview switch UUID mismatch");
  if (data.preview !== true || data.matched !== true) fail("preview is not a matched read-only result");
  const projection = readProjection(projectionPath);
  validateLines(data, projection);
  validateTotals(data, projection);
  process.stdout.write(`opening preview verification passed: rolls=${projection.size}\n`);
}

main();
