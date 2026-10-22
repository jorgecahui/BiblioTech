import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    // Avoid child-process startup timeouts on Windows and keep CI execution consistent.
    pool: 'threads',
    maxWorkers: 1
  }
});
