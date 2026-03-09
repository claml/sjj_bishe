<template>
  <div id="userLoginView" class="auth-page">
    <div class="auth-container">
      <h1 class="auth-main-title">在线编程学习平台</h1>
      <div class="auth-card">
        <h2 class="auth-title">用户登录</h2>
        <a-form
          class="auth-form"
          label-align="left"
          auto-label-width
          :model="form"
          @submit="handleSubmit"
        >
          <a-form-item field="userAccount" label="账号">
            <a-input v-model="form.userAccount" placeholder="请输入账号" />
          </a-form-item>
          <a-form-item
            field="userPassword"
            tooltip="密码不少于 8 位"
            label="密码"
          >
            <a-input-password
              v-model="form.userPassword"
              placeholder="请输入密码"
            />
          </a-form-item>
          <a-form-item class="auth-submit-item">
            <div class="button-container">
              <a-button
                type="primary"
                html-type="submit"
                class="auth-submit-btn"
              >
                登录
              </a-button>
            </div>
          </a-form-item>
        </a-form>

        <div class="auth-switch">
          没有账号？
          <a-button type="text" class="auth-switch-link" @click="goRegister">
            立即注册
          </a-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive } from "vue";
import { UserControllerService, UserLoginRequest } from "../../../generated";
import message from "@arco-design/web-vue/es/message";
import { useRouter } from "vue-router";
import { useStore } from "vuex";

/**
 * 表单信息
 */
const form = reactive({
  userAccount: "",
  userPassword: "",
} as UserLoginRequest);

const router = useRouter();
const store = useStore();

const goRegister = () => {
  router.push("/user/register");
};

/**
 * 提交表单
 * @param data
 */
const handleSubmit = async () => {
  const res = await UserControllerService.userLoginUsingPost(form);
  // 登录成功，跳转到主页
  if (res.code === 0) {
    await store.dispatch("user/getLoginUser");
    router.push({
      path: "/",
      replace: true,
    });
  } else {
    message.error("登陆失败，" + res.message);
  }
};
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px 16px;
  box-sizing: border-box;
}

.auth-container {
  width: min(420px, 100%);
}

.auth-main-title {
  margin: 0 0 28px;
  text-align: center;
  font-weight: 700;
  font-size: clamp(28px, 4vw, 34px);
  line-height: 1.25;
  color: #1d2129;
}

.auth-card {
  background: #fff;
  border-radius: 14px;
  padding: 36px;
  box-shadow: 0 10px 32px rgba(0, 0, 0, 0.12);
}

.auth-title {
  margin: 0 0 16px;
  text-align: center;
}

.auth-form :deep(.arco-input-wrapper),
.auth-form :deep(.arco-input-password) {
  height: 40px;
  border-radius: 6px;
}

.auth-form :deep(.arco-form-item) {
  margin-bottom: 14px;
}

.auth-submit-item {
  margin-top: 8px;
  margin-bottom: 0;
}

.auth-submit-btn {
  width: 200px;
  height: 44px;
  border-radius: 8px;
}

.button-container {
  display: flex;
  justify-content: center;
}

.auth-switch {
  margin-top: 14px;
  text-align: center;
  color: var(--color-text-2);
}

.auth-switch-link {
  padding: 0 4px;
}

@media (max-width: 480px) {
  .auth-card {
    padding: 28px 20px;
  }
}
</style>
