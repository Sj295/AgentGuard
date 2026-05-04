<template>
  <div class="section-header">
    <div class="section-header-left">
      <div class="section-header-title">{{ title }}</div>
      <div v-if="desc" class="section-header-desc">{{ desc }}</div>
    </div>
    <div class="section-header-right">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  title: string
  desc?: string
}

defineProps<Props>()
</script>

<style scoped>
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.section-header-title {
  font-size: 15px;
  font-weight: 600;
  color: #111827;
}

.section-header-desc {
  font-size: 12px;
  color: #94A3B8;
  margin-top: 2px;
}

.section-header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
