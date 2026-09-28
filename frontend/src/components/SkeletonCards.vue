<script setup>
defineProps({
  count: {
    type: Number,
    default: 6
  },
  /** card：内容卡片骨架；row：列表行骨架 */
  variant: {
    type: String,
    default: 'card'
  }
})
</script>

<template>
  <div
    v-if="variant === 'card'"
    class="grid-cards"
  >
    <div
      v-for="n in count"
      :key="n"
      class="sk-card glass"
    >
      <div
        class="skeleton"
        style="width: 74px; height: 22px; border-radius: 999px"
      ></div>
      <div
        class="skeleton"
        style="width: 82%; height: 17px; margin-top: 14px"
      ></div>
      <div
        class="skeleton"
        style="width: 100%; height: 11px; margin-top: 10px"
      ></div>
      <div
        class="skeleton"
        style="width: 64%; height: 11px; margin-top: 7px"
      ></div>
      <div class="sk-foot">
        <div
          class="skeleton"
          style="width: 90px; height: 11px"
        ></div>
        <div
          class="skeleton"
          style="width: 70px; height: 11px"
        ></div>
      </div>
    </div>
  </div>

  <div
    v-else
    class="stack stack-sm"
  >
    <div
      v-for="n in count"
      :key="n"
      class="sk-row glass"
    >
      <div
        class="skeleton"
        style="width: 34px; height: 34px; border-radius: 50%"
      ></div>
      <div style="flex: 1">
        <div
          class="skeleton"
          style="width: 62%; height: 13px"
        ></div>
        <div
          class="skeleton"
          style="width: 34%; height: 10px; margin-top: 8px"
        ></div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.sk-card {
  padding: 20px;
  min-height: 190px;
}

.sk-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 20px;
  padding-top: 13px;
  border-top: 1px solid var(--line);
}

.sk-row {
  display: flex;
  align-items: center;
  gap: 13px;
  padding: 14px 16px;
}
</style>
