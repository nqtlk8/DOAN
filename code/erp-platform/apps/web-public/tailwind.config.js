/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: "#ed1c24", // SCG Red
        accent: "#1E293B", // Dark Slate
        background: "#F8FAFC",
        foreground: "#0f172a",
        destructive: "#ef4444",
      },
      fontFamily: {
        inter: ['Inter', 'sans-serif'],
      },
    },
  },
  plugins: [],
}
