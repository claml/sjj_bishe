<template>
  <a-row id="globalHeader" align="center" :wrap="false">
    <a-col flex="300px" class="brand-col">
      <div class="title-bar">
        <div class="title">在线编程学习平台</div>
      </div>
    </a-col>
    <a-col flex="auto" class="menu-col">
      <a-menu
        class="header-menu"
        mode="horizontal"
        :selected-keys="selectedKeys"
        @menu-item-click="doMenuClick"
      >
        <a-menu-item key="/">主页</a-menu-item>
        <a-menu-item v-for="item in visibleRoutes" :key="item.path">
          {{ item.name }}
        </a-menu-item>
      </a-menu>
    </a-col>
    <a-col flex="200px" class="user-menu-col">
      <UserMenu />
    </a-col>
  </a-row>
</template>

<script setup lang="ts">
import { routes } from "../router/routes";
import { useRouter } from "vue-router";
import { computed, ref } from "vue";
import { useStore } from "vuex";
import checkAccess from "@/access/checkAccess";
import UserMenu from "@/components/UserMenu.vue";

const router = useRouter();
const store = useStore();

const visibleRoutes = computed(() => {
  return routes.filter((item) => {
    if (item.path === "/") {
      return false;
    }
    if (item.meta?.hideInMenu) {
      return false;
    }
    if (
      !checkAccess(store.state.user.loginUser, item?.meta?.access as string)
    ) {
      return false;
    }
    return true;
  });
});

const selectedKeys = ref([router.currentRoute.value.path || "/"]);

router.afterEach((to) => {
  selectedKeys.value = [to.path];
});

const doMenuClick = (key: string) => {
  router.push({
    path: key,
  });
};
</script>

<style scoped>
#globalHeader {
  width: 100%;
  min-width: 0;
  height: 64px;
  overflow: hidden;
}

.brand-col {
  min-width: 240px;
}

.menu-col {
  min-width: 0;
  overflow: hidden;
}

.title-bar {
  display: flex;
  align-items: center;
  height: 64px;
  padding-left: 8px;
}

.title {
  color: #1d2129;
  margin-left: 8px;
  font-size: 20px;
  font-weight: 700;
  line-height: 1.2;
  letter-spacing: 0.3px;
  white-space: nowrap;
}

.user-menu-col {
  display: flex;
  justify-content: flex-end;
  min-width: 180px;
}

:deep(.header-menu) {
  height: 64px;
  background: transparent;
  overflow: hidden;
}

:deep(.header-menu .arco-menu-inner),
:deep(.header-menu .arco-menu-overflow),
:deep(.header-menu .arco-menu-overflow-wrap) {
  height: 64px;
  display: flex;
  align-items: center;
  flex-wrap: nowrap;
  white-space: nowrap;
  overflow-y: hidden;
}

:deep(.header-menu .arco-menu-overflow) {
  scrollbar-width: none;
}

:deep(.header-menu .arco-menu-overflow::-webkit-scrollbar) {
  display: none;
}

:deep(.header-menu .arco-menu-item) {
  height: 64px;
  display: flex;
  align-items: center;
  line-height: 1.2;
  font-size: 16px;
  font-weight: 600;
  color: #4e5969;
  border-radius: 0;
  margin: 0 4px;
  padding: 0 14px !important;
  position: relative;
  background: transparent !important;
  transition: all 0.2s ease;
}

:deep(.header-menu .arco-menu-item::after) {
  content: "";
  position: absolute;
  left: 14px;
  right: 14px;
  bottom: 8px;
  height: 0;
  border-radius: 3px;
  background-color: #165dff;
  opacity: 0;
  transition: all 0.2s ease;
}

:deep(.header-menu .arco-menu-item:hover) {
  color: #1d2129;
  background: transparent !important;
}

:deep(
    .header-menu .arco-menu-item.arco-menu-selected,
    .header-menu .arco-menu-item:active,
    .header-menu .arco-menu-item:focus
  ) {
  background: transparent !important;
}

:deep(.header-menu .arco-menu-selected) {
  color: #165dff !important;
  background: transparent !important;
}

:deep(.header-menu .arco-menu-selected::after) {
  height: 3px;
  opacity: 1;
}
</style>
