<!-- 根组件: 路由视图出口 -->
<template>
  <router-view />
</template>

<script setup>
// 根组件只负责渲染当前路由对应的页面
</script>

<style>
/* 全局基础样式 */
* { margin: 0; padding: 0; box-sizing: border-box; }
html, body, #app { height: 100%; font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif; background: #f5f7fa; }
</style>
