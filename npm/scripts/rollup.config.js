import commonjs from '@rollup/plugin-commonjs';

export default {
  input: './src/main.js',
  output: [
    {
      file: 'dist/archly.common.js',
      format: 'cjs',
      exports: 'named'
    },
    {
      file: 'dist/archly.esm.js',
      format: 'esm',
      exports: 'named'
    },
    {
      file: 'dist/archly.browser.js',
      name: 'Archly',
      format: 'iife',
      exports: 'named'
    }
  ],
  plugins: [
    commonjs()
  ]
}

