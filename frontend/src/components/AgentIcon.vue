<template>
  <div class="agent-icon" :style="{ width: size + 'px', height: size + 'px' }">
    <img
      v-if="type === 'CODEX'"
      src="/icons/codex.webp"
      alt="Codex"
      class="agent-img"
      :style="{ width: size + 'px', height: size + 'px', borderRadius: size * 0.22 + 'px' }"
    />
    <img
      v-else-if="type === 'CLAUDE'"
      src="/icons/claude.webp"
      alt="Claude"
      class="agent-img"
      :style="{ width: size + 'px', height: size + 'px', borderRadius: size * 0.22 + 'px' }"
    />
    <img
      v-else-if="type === 'CURSOR'"
      src="/icons/cursor.png"
      alt="Cursor"
      class="agent-img"
      :style="{ width: size + 'px', height: size + 'px', borderRadius: size * 0.22 + 'px' }"
    />
    <svg
      v-else
      :width="size"
      :height="size"
      viewBox="0 0 48 48"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      <rect width="48" height="48" rx="10" fill="#64748B"/>
      <text x="24" y="30" text-anchor="middle" fill="white" font-size="20" font-weight="bold">?</text>
    </svg>
  </div>
</template>

<script setup lang="ts">
interface Props {
  type: string
  size?: number
}

withDefaults(defineProps<Props>(), {
  size: 32
})
</script>

<style scoped>
.agent-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
}

.agent-img {
  object-fit: cover;
  display: block;
}
</style>
