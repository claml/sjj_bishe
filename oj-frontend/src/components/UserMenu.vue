<template>
  <a-dropdown trigger="click" position="br">
    <div class="user-menu-trigger">
      <img
        class="user-avatar"
        :src="displayAvatar"
        :alt="`${displayName} 的头像`"
      />
      <span class="user-name">{{ displayName }}</span>
      <span class="arrow-down">▼</span>
    </div>
    <template #content>
      <a-doption @click="goToAccountInfo">账号信息</a-doption>
      <a-doption @click="logout">退出登录</a-doption>
    </template>
  </a-dropdown>
</template>

<script setup lang="ts">
import { computed, onMounted } from "vue";
import { useStore } from "vuex";
import { useRouter } from "vue-router";
import { UserControllerService } from "../../generated";
import ACCESS_ENUM from "@/access/accessEnum";
import defaultAvatar from "@/assets/default-user-avatar.svg";

const store = useStore();
const router = useRouter();

onMounted(() => {
  // 获取当前用户信息接口（真实接口）
  store.dispatch("user/getLoginUser");
});

const loginUser = computed(() => store.state.user?.loginUser ?? {});

const displayName = computed(() => loginUser.value.userName || "未登录");

const displayAvatar = computed(
  () => loginUser.value.userAvatar || defaultAvatar
);

const goToAccountInfo = async () => {
  // 跳转账号信息页接口
  await router.push({ path: "/user/account" });
};

const logout = async () => {
  // 退出登录方法接口
  await UserControllerService.userLogoutUsingPost();
  store.commit("user/updateUser", {
    userName: "未登录",
    userRole: ACCESS_ENUM.NOT_LOGIN,
    userAvatar: "",
  });
  await router.push({ path: "/user/login" });
};
</script>

<style scoped>
.user-menu-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.user-menu-trigger:hover {
  background: #f5f5f5;
}

.user-avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid #e9e9e9;
  background: #fff;
}

.user-name {
  color: #4e5969;
  font-size: 14px;
  line-height: 1;
  max-width: 110px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.arrow-down {
  color: #86909c;
  font-size: 11px;
  line-height: 1;
}

:deep(.arco-dropdown-open .user-menu-trigger) {
  background: #f5f5f5;
}

:deep(.arco-dropdown-list) {
  border: 1px solid #e5e6eb;
  border-radius: 10px;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
  padding: 4px;
}

:deep(.arco-dropdown-option) {
  border-radius: 6px;
}

:deep(.arco-dropdown-option:hover) {
  background: #f5f5f5;
}
</style>
