<template>
  <div id="app">
    <template v-if="isAuthPage">
      <router-view />
    </template>
    <template v-else>
      <BasicLayout />
    </template>
  </div>
</template>

<style>
#app {
}
</style>
<script setup lang="ts">
import BasicLayout from "@/layouts/BasicLayout";
import { computed, onMounted } from "vue";
import { useRoute } from "vue-router";

const route = useRoute();
const isAuthPage = computed(() => {
  return route.path === "/user/login" || route.path === "/user/register";
});

/**
 * 全局初始化函数，有全局单次调用的代码，都可以写到这里
 */
const doInit = () => {
  console.log("hello 欢迎来到我的项目");
};

onMounted(() => {
  doInit();
});
</script>
