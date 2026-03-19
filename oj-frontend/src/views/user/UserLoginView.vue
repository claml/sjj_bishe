<template>
  <div id="userLoginView" class="auth-page">
    <div class="auth-container">
      <h1 class="auth-main-title">在线编程学习平台</h1>
      <div class="auth-card">
        <h2 class="auth-title">登录</h2>
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
          <a-form-item field="captcha" label="图形验证">
            <div class="captcha-row">
              <a-input
                v-model="captchaInput"
                placeholder="请输入图形验证码"
                class="captcha-input"
              />
              <img
                class="captcha-image"
                :src="captchaImageData"
                alt="图形验证码"
                title="点击刷新验证码"
                @click="refreshCaptcha"
              />
            </div>
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
import { reactive, ref } from "vue";
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
const captchaInput = ref("");
const captchaCode = ref("");
const captchaImageData = ref("");

const router = useRouter();
const store = useStore();

const goRegister = () => {
  router.push("/user/register");
};

const randomFrom = (chars: string) => {
  return chars[Math.floor(Math.random() * chars.length)];
};

const generateCaptchaCode = (length = 4) => {
  const chars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
  let result = "";
  for (let i = 0; i < length; i += 1) {
    result += randomFrom(chars);
  }
  return result;
};

const createCaptchaImage = (code: string) => {
  const width = 120;
  const height = 40;
  const chars = code
    .split("")
    .map((char, index) => {
      const x = 18 + index * 24;
      const y = 27 + Math.floor(Math.random() * 6) - 3;
      const rotate = Math.floor(Math.random() * 30) - 15;
      const color = `hsl(${Math.floor(Math.random() * 200) + 120}, 70%, 35%)`;
      return `<text x="${x}" y="${y}" fill="${color}" font-size="24" font-family="Verdana" transform="rotate(${rotate} ${x} ${y})">${char}</text>`;
    })
    .join("");
  const lines = Array.from({ length: 4 })
    .map(() => {
      const x1 = Math.floor(Math.random() * width);
      const y1 = Math.floor(Math.random() * height);
      const x2 = Math.floor(Math.random() * width);
      const y2 = Math.floor(Math.random() * height);
      const color = `rgba(50, 80, 120, ${Math.random() * 0.4 + 0.2})`;
      return `<line x1="${x1}" y1="${y1}" x2="${x2}" y2="${y2}" stroke="${color}" stroke-width="1.2" />`;
    })
    .join("");
  const dots = Array.from({ length: 30 })
    .map(() => {
      const cx = Math.floor(Math.random() * width);
      const cy = Math.floor(Math.random() * height);
      const r = Math.random() * 1.5 + 0.4;
      const color = `rgba(30, 60, 100, ${Math.random() * 0.6 + 0.2})`;
      return `<circle cx="${cx}" cy="${cy}" r="${r}" fill="${color}" />`;
    })
    .join("");
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}"><rect width="100%" height="100%" rx="6" ry="6" fill="#f2f5fa" />${lines}${dots}${chars}</svg>`;
  return `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`;
};

const refreshCaptcha = () => {
  const code = generateCaptchaCode();
  captchaCode.value = code;
  captchaImageData.value = createCaptchaImage(code);
};

refreshCaptcha();

/**
 * 提交表单
 * @param data
 */
const handleSubmit = async () => {
  const input = captchaInput.value.trim().toUpperCase();
  if (!input) {
    message.warning("请输入图形验证码");
    return;
  }
  if (input !== captchaCode.value) {
    message.error("图形验证码错误，请重试");
    captchaInput.value = "";
    refreshCaptcha();
    return;
  }

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
    captchaInput.value = "";
    refreshCaptcha();
  }
};
</script>

<style scoped>
.auth-page {
  width: 100%;
  display: flex;
  justify-content: center;
  padding: 0 16px;
  box-sizing: border-box;
}

.auth-container {
  width: min(420px, 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
}

.auth-main-title {
  margin: 0 0 24px;
  text-align: center;
  font-weight: 700;
  font-size: clamp(28px, 4vw, 34px);
  line-height: 1.25;
  color: #1d2129;
}

.auth-card {
  width: 100%;
  margin-bottom: 16px;
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

.captcha-row {
  display: flex;
  gap: 10px;
  width: 100%;
  align-items: center;
}

.captcha-input {
  flex: 1;
}

.captcha-image {
  width: 120px;
  height: 40px;
  border-radius: 6px;
  cursor: pointer;
  border: 1px solid var(--color-border-2);
  user-select: none;
}

.auth-submit-item {
  margin-top: 20px;
  margin-bottom: 0;
}

.auth-submit-btn {
  width: 240px;
  height: 48px;
  border-radius: 12px;
  font-size: 17px;
  font-weight: 600;
}

.button-container {
  display: flex;
  justify-content: center;
}

.auth-switch {
  margin-top: 16px;
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
