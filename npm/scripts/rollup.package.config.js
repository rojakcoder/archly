import commonjs from '@rollup/plugin-commonjs';
import minify from 'rollup-plugin-minify-es';

export default {
  input: './src/main.js',
  output: [
    {
      file: 'dist/archly.common.min.js',
      format: 'cjs',
      exports: 'named'
    },
    {
      file: 'dist/archly.esm.min.js',
      format: 'esm',
      exports: 'named'
    },
    {
      file: 'dist/archly.browser.min.js',
      name: 'Archly',
      format: 'iife',
      exports: 'named'
    }
  ],
  plugins: [
    commonjs(),
    minify()
  ]
}

