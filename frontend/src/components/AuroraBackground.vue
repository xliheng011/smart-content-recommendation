<template>
  <div
    class="aurora"
    aria-hidden="true"
  >
    <!--
      黑白灰版本的"极光"：
      不再靠色相区分，而是用四团不同明度的灰斑制造层次。
      玻璃面板要透出有明暗变化的东西才显得出质感，
      如果背景是一块纯色，磨砂就会糊成一片。
    -->
    <span class="blob blob-deep"></span>
    <span class="blob blob-light"></span>
    <span class="blob blob-mid"></span>
    <span class="blob blob-shadow"></span>

    <!-- 极细噪点，让玻璃透出的背景更有质感，避免大面积渐变发"假" -->
    <div class="grain"></div>
  </div>
</template>

<style scoped>
.aurora {
  position: fixed;
  inset: 0;
  z-index: 0;
  overflow: hidden;
  pointer-events: none;
  background: linear-gradient(160deg, #fbfbfc 0%, #f1f1f2 45%, #e6e6ea 100%);
}

.blob {
  position: absolute;
  display: block;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.9;
  will-change: transform;
}

/* 左上：最重的一块灰，定住画面的"暗角" */
.blob-deep {
  width: 46vw;
  height: 46vw;
  min-width: 420px;
  min-height: 420px;
  top: -12vw;
  left: -8vw;
  background: radial-gradient(
    circle at 35% 35%,
    var(--tint-1),
    rgba(201, 201, 207, 0) 70%
  );
  animation: float-slow 26s ease-in-out infinite;
}

/* 右上：提亮，和左上形成对角明暗关系 */
.blob-light {
  width: 42vw;
  height: 42vw;
  min-width: 380px;
  min-height: 380px;
  top: 4vh;
  right: -12vw;
  background: radial-gradient(
    circle at 50% 50%,
    #ffffff,
    rgba(255, 255, 255, 0) 68%
  );
  animation: float-slow 32s ease-in-out infinite reverse;
}

/* 左下：中间调，负责过渡 */
.blob-mid {
  width: 38vw;
  height: 38vw;
  min-width: 320px;
  min-height: 320px;
  bottom: -14vw;
  left: 16vw;
  background: radial-gradient(
    circle at 50% 50%,
    var(--tint-3),
    rgba(212, 212, 218, 0) 70%
  );
  animation: float-slow 38s ease-in-out infinite;
}

/* 右下：稍深的灰，压住底部 */
.blob-shadow {
  width: 34vw;
  height: 34vw;
  min-width: 300px;
  min-height: 300px;
  bottom: -8vw;
  right: 2vw;
  background: radial-gradient(
    circle at 50% 50%,
    var(--tint-4),
    rgba(191, 191, 198, 0) 70%
  );
  animation: float-slow 30s ease-in-out infinite reverse;
}

.grain {
  position: absolute;
  inset: 0;
  opacity: 0.34;
  mix-blend-mode: soft-light;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='140' height='140'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='3'/%3E%3C/filter%3E%3Crect width='140' height='140' filter='url(%23n)' opacity='0.5'/%3E%3C/svg%3E");
}
</style>
