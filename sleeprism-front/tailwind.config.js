/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  // --- 이 부분을 추가해주세요 ---
  safelist: [
    'w-10',
    'h-10',
    // 여기에 다른 문제가 되는 클래스가 있다면 추가할 수 있습니다.
    // 예: 'md:grid-cols-2', 'lg:grid-cols-4' 등
  ],
  // --------------------------
  theme: {
    extend: {},
  },
  plugins: [],
}
