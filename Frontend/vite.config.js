// import { defineConfig } from 'vite'
// import react from '@vitejs/plugin-react-swc'

// // https://vite.dev/config/
// export default defineConfig({
//   plugins: [react()],
// })



import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react-swc'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    react({
      jsx: 'react', // Ensure JSX is handled correctly
      swc: {
        jsc: {
          parser: {
            syntax: 'ecmascript',
            jsx: true,  // Enable JSX parsing
          },
        },
      },
    }),
  ],
})
