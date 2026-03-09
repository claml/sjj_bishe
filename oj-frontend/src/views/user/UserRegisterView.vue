<template>
  <div id="userRegisterView" class="auth-page">
    <div class="auth-container">
      <h1 class="auth-main-title">在线编程学习平台</h1>
      <div class="auth-card">
        <h2 class="auth-title">用户注册</h2>
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

          <a-form-item field="userName" label="用户名">
            <a-input v-model="form.userName" placeholder="请输入用户名" />
          </a-form-item>

          <a-form-item
            field="userPassword"
            :tooltip="PASSWORD_RULE_HINT"
            label="密码"
          >
            <a-input-password
              v-model="form.userPassword"
              placeholder="请输入密码"
            />
          </a-form-item>

          <a-form-item field="checkPassword" label="确认密码">
            <a-input-password
              v-model="form.checkPassword"
              placeholder="请再次输入密码"
            />
          </a-form-item>

          <a-form-item class="auth-submit-item">
            <div class="button-container">
              <a-button
                type="primary"
                html-type="submit"
                class="auth-submit-btn"
              >
                注册
              </a-button>
            </div>
          </a-form-item>
        </a-form>

        <div class="auth-switch">
          已有账号？
          <a-button type="text" class="auth-switch-link" @click="goLogin">
            立即登录
          </a-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive } from "vue";
import { UserControllerService, UserRegisterRequest } from "../../../generated";
import message from "@arco-design/web-vue/es/message";
import { useRouter } from "vue-router";
import {
  PASSWORD_RULE_HINT,
  validatePasswordByRegisterRule,
} from "@/utils/passwordRules";

/**
 * 表单信息
 */
const form = reactive({
  userAccount: "",
  userName: "",
  userPassword: "",
  checkPassword: "",
} as UserRegisterRequest);

const router = useRouter();

const goLogin = () => {
  router.push("/user/login");
};

/**
 * 提交表单
 */
const handleSubmit = async () => {
  if (
    !form.userAccount ||
    !form.userName ||
    !form.userPassword ||
    !form.checkPassword
  ) {
    message.error("请填写完整信息");
    return;
  }

  const passwordError = validatePasswordByRegisterRule(form.userPassword);
  if (passwordError) {
    message.error(passwordError);
    return;
  }

  if (form.userPassword !== form.checkPassword) {
    message.error("两次输入的密码不一致");
    return;
  }

  const res = await UserControllerService.userRegisterUsingPost(form);

  if (res.code === 0) {
    message.success("注册成功，请登录");
    router.push({
      path: "/user/login",
      replace: true,
    });
  } else {
    message.error("注册失败，" + res.message);
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
