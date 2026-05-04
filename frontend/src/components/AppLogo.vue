<template>
  <svg
    :width="size"
    :height="size"
    viewBox="0 0 32 32"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
  >
    <defs>
      <linearGradient id="logoGrad" x1="0" y1="0" x2="32" y2="32" gradientUnits="userSpaceOnUse">
        <stop offset="0%" stop-color="#4F46E5"/>
        <stop offset="100%" stop-color="#7C3AED"/>
      </linearGradient>
    </defs>
    <rect width="32" height="32" rx="8" fill="url(#logoGrad)"/>
    <!-- Shield shape -->
    <path
      d="M16 5L8 9V16C8 21.5 11.8 26.5 16 28C20.2 26.5 24 21.5 24 16V9L16 5Z"
      fill="none"
      stroke="white"
      stroke-width="1.8"
      stroke-linejoin="round"
    />
    <!-- Checkmark -->
    <path
      d="M12 16L15 19L21 13"
      stroke="white"
      stroke-width="2"
      stroke-linecap="round"
      stroke-linejoin="round"
    />
  </svg>
</template>

<script setup lang="ts">
interface Props {
  size?: number
}

withDefaults(defineProps<Props>(), {
  size: 32
})
</script>
