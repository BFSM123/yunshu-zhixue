<template>
  <div class="home">
    <section class="hero">
      <div class="hero-copy">
        <el-tag effect="dark" round>AI 赋能的智能面试训练平台</el-tag>
        <h1>把每一次练习，<br />变成面试底气。</h1>
        <p class="hero-desc">
          学科 → 阶段 → 题目，刷题、错题复习、学习路线与 AI 模拟面试闭环训练。
          让每一道错题，都成为下一次加分项。
        </p>
        <div class="hero-actions">
          <el-button type="primary" size="large" @click="router.push('/questions')">
            开始刷题
          </el-button>
          <el-button size="large" @click="router.push('/interview')">
            AI 模拟面试
          </el-button>
        </div>
      </div>

      <div class="hero-panel">
        <div class="stat-grid">
          <div v-for="item in stats" :key="item.label" class="stat-card">
            <div class="stat-value">{{ item.value }}</div>
            <div class="stat-label">{{ item.label }}</div>
          </div>
        </div>
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="平台数据接口接入后，此处展示真实统计"
        />
      </div>
    </section>

    <section class="features">
      <div v-for="item in features" :key="item.title" class="feature-card">
        <div class="feature-icon">{{ item.icon }}</div>
        <h3>{{ item.title }}</h3>
        <p>{{ item.description }}</p>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const stats = [
  { label: '注册学员', value: '—' },
  { label: '题库题目', value: '—' },
  { label: 'AI 面试次数', value: '—' },
  { label: '面试好评率', value: '—' }
]

const features = [
  {
    icon: '📚',
    title: '三级题库',
    description: '按学科、阶段、题型与难度浏览题库，顺序或随机组卷练习。'
  },
  {
    icon: '🎯',
    title: '错题闭环',
    description: '答错自动归集错题，按错误次数分级，连续答对 3 次自动掌握。'
  },
  {
    icon: '🤖',
    title: 'AI 模拟面试',
    description: '选择方向与难度，AI 逐个提问、逐题评分，生成六维能力报告。'
  }
]
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.home {
  max-width: 1200px;
  margin: 0 auto;
  padding: 48px 24px 64px;
}

.hero {
  display: grid;
  grid-template-columns: 1.15fr 0.85fr;
  gap: 48px;
  align-items: center;
}

.hero-copy h1 {
  margin: 20px 0 16px;
  font-size: 46px;
  line-height: 1.2;
  letter-spacing: -1px;
}

.hero-desc {
  max-width: 560px;
  color: $yszx-text-secondary;
  font-size: 16px;
  line-height: 1.9;
}

.hero-actions {
  display: flex;
  gap: 12px;
  margin-top: 28px;
}

.hero-panel {
  padding: 24px;
  background: $yszx-surface;
  border: 1px solid $yszx-border;
  border-radius: 20px;
  box-shadow: 0 18px 50px rgba(37, 99, 235, 0.08);
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
  margin-bottom: 16px;
}

.stat-card {
  padding: 18px;
  background: linear-gradient(180deg, #f8fbff, #eef4ff);
  border-radius: 14px;
}

.stat-value {
  font-size: 26px;
  font-weight: 700;
}

.stat-label {
  margin-top: 6px;
  color: $yszx-text-secondary;
  font-size: 13px;
}

.features {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-top: 72px;
}

.feature-card {
  padding: 26px;
  background: $yszx-surface;
  border: 1px solid $yszx-border;
  border-radius: 16px;

  h3 {
    margin: 14px 0 8px;
    font-size: 18px;
  }

  p {
    margin: 0;
    color: $yszx-text-secondary;
    font-size: 14px;
    line-height: 1.8;
  }
}

.feature-icon {
  font-size: 28px;
}

@media (max-width: 900px) {
  .hero {
    grid-template-columns: 1fr;
  }

  .hero-copy h1 {
    font-size: 34px;
  }

  .features {
    grid-template-columns: 1fr;
    margin-top: 40px;
  }
}
</style>
