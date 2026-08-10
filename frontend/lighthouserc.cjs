const chromePort = Number(process.env.PAPER_MES_LHCI_CHROME_PORT)
const lighthouseUrl = process.env.PAPER_MES_LHCI_URL ?? 'http://127.0.0.1:4173/login'

module.exports = {
  ci: {
    collect: {
      numberOfRuns: 3,
      settings: {
        chromeFlags: '--headless --no-sandbox --disable-gpu',
        onlyCategories: ['performance', 'accessibility', 'best-practices'],
        preset: 'desktop',
        ...(Number.isInteger(chromePort) ? { port: chromePort } : {}),
      },
      url: [lighthouseUrl],
    },
    assert: {
      assertions: {
        'categories:accessibility': ['error', { minScore: 0.9 }],
        'categories:best-practices': ['warn', { minScore: 0.9 }],
        'categories:performance': ['warn', { minScore: 0.75 }],
        'cumulative-layout-shift': ['error', { maxNumericValue: 0.1 }],
        'first-contentful-paint': ['warn', { maxNumericValue: 2_500 }],
        'largest-contentful-paint': ['warn', { maxNumericValue: 4_000 }],
        'total-blocking-time': ['warn', { maxNumericValue: 500 }],
      },
    },
    upload: {
      outputDir: './artifacts/lighthouse',
      target: 'filesystem',
    },
  },
}
