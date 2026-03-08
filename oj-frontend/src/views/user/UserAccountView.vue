<template>
  <div id="userAccountView" class="light-page">
    <a-button class="back-home-button" type="text" @click="goHome">
      <template #icon>
        <icon-left />
      </template>
      返回主页
    </a-button>

    <a-card class="profile-header" :bordered="false">
      <div class="profile-main">
        <a-avatar :size="76" class="avatar">
          <img :src="userInfo.userAvatar || defaultAvatar" alt="用户头像" />
        </a-avatar>
        <div class="profile-text">
          <h2 class="nickname">{{ userInfo.userName || "未设置昵称" }}</h2>
          <p class="account">账号：{{ userInfo.userAccount || "--" }}</p>
        </div>
      </div>
    </a-card>

    <a-card class="info-card" title="个人中心" :bordered="false">
      <div class="info-list">
        <div class="info-item">
          <span class="label">用户头像</span>
          <div class="value avatar-value">
            <a-avatar :size="48">
              <img :src="userInfo.userAvatar || defaultAvatar" alt="用户头像" />
            </a-avatar>
            <span class="value-text">{{
              userInfo.userAvatar || "未设置"
            }}</span>
          </div>
        </div>

        <div class="info-item">
          <span class="label">昵称</span>
          <span class="value">{{ userInfo.userName || "未设置" }}</span>
        </div>

        <div class="info-item">
          <span class="label">账号</span>
          <span class="value">{{ userInfo.userAccount || "--" }}</span>
        </div>

        <div class="info-item">
          <span class="label">密码</span>
          <div class="value password-area">
            <span>{{ showPassword ? displayedPassword : maskedPassword }}</span>
            <a-space>
              <a-button size="small" @click="togglePassword">
                {{ showPassword ? "隐藏密码" : "显示密码" }}
              </a-button>
              <a-button
                size="small"
                type="outline"
                status="warning"
                @click="openPasswordModal"
              >
                修改密码
              </a-button>
            </a-space>
          </div>
        </div>

        <div class="info-item">
          <span class="label">简介</span>
          <span class="value">{{
            userInfo.userProfile || "这个人很懒，还没有填写简介"
          }}</span>
        </div>

        <div class="info-item">
          <span class="label">角色</span>
          <div class="value">
            <a-tag :color="roleTag.color">{{ roleTag.text }}</a-tag>
          </div>
        </div>

        <div class="info-item">
          <span class="label">注册时间</span>
          <span class="value">{{
            formatRegisterTime(userInfo.createTime)
          }}</span>
        </div>
      </div>
    </a-card>

    <a-modal
      v-model:visible="passwordModalVisible"
      title="修改密码"
      @before-ok="handleChangePassword"
      @cancel="resetPasswordForm"
      ok-text="确认修改"
      cancel-text="取消"
      :ok-loading="savingPassword"
    >
      <a-form :model="passwordForm" layout="vertical">
        <a-form-item field="oldPassword" label="原密码" required>
          <a-input-password
            v-model="passwordForm.oldPassword"
            placeholder="请输入原密码"
            allow-clear
          />
        </a-form-item>
        <a-form-item field="newPassword" label="新密码" required>
          <a-input-password
            v-model="passwordForm.newPassword"
            placeholder="请输入新密码"
            allow-clear
          />
        </a-form-item>
        <a-form-item field="confirmPassword" label="确认新密码" required>
          <a-input-password
            v-model="passwordForm.confirmPassword"
            placeholder="请再次输入新密码"
            allow-clear
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { Message } from "@arco-design/web-vue";
import { IconLeft } from "@arco-design/web-vue/es/icon";
import { useRouter } from "vue-router";
import { UserControllerService } from "../../../generated";

const defaultAvatar =
  "https://p3-passport.byteimg.com/img/user-avatar-placeholder.jpeg~180x180.awebp";

const router = useRouter();

const userInfo = reactive({
  userAvatar: "",
  userName: "",
  userAccount: "",
  userPassword: "",
  userProfile: "",
  userRole: "user",
  createTime: "",
});

const maskedPassword = "••••••••";
const displayedPassword = computed(
  () => userInfo.userPassword || maskedPassword
);
const showPassword = ref(false);

const passwordModalVisible = ref(false);
const savingPassword = ref(false);
const passwordForm = reactive({
  oldPassword: "",
  newPassword: "",
  confirmPassword: "",
});

const roleTag = computed(() => {
  switch (userInfo.userRole) {
    case "admin":
      return { text: "admin", color: "arcoblue" };
    case "ban":
      return { text: "ban", color: "orangered" };
    default:
      return { text: "user", color: "green" };
  }
});

const formatRegisterTime = (createTime?: string) => {
  if (!createTime) {
    return "--";
  }
  const date = new Date(createTime);
  if (Number.isNaN(date.getTime())) {
    return createTime;
  }
  return date.toLocaleString("zh-CN", { hour12: false });
};

const goHome = () => {
  router.push("/");
};

const loadUserInfo = async () => {
  // 这里预留对接后端：真实项目中建议在页面进入时调用当前登录用户信息接口
  const res = await UserControllerService.getLoginUserUsingGet();
  if (res.code !== 0 || !res.data) {
    Message.error("获取用户信息失败");
    return;
  }
  userInfo.userAvatar = res.data.userAvatar || "";
  userInfo.userName = res.data.userName || "";
  userInfo.userAccount = res.data.userAccount || "";
  userInfo.userProfile = res.data.userProfile || "";
  userInfo.userRole = res.data.userRole || "user";
  userInfo.createTime = res.data.createTime || "";

  // 注意：后端通常不会返回真实密码，这里仅做页面展示与交互占位
  userInfo.userPassword = "";
};

const togglePassword = () => {
  showPassword.value = !showPassword.value;
};

const openPasswordModal = () => {
  passwordModalVisible.value = true;
};

const resetPasswordForm = () => {
  passwordForm.oldPassword = "";
  passwordForm.newPassword = "";
  passwordForm.confirmPassword = "";
};

const validatePasswordForm = () => {
  if (!passwordForm.oldPassword) {
    Message.warning("原密码不能为空");
    return false;
  }
  if (!passwordForm.newPassword) {
    Message.warning("新密码不能为空");
    return false;
  }
  if (!passwordForm.confirmPassword) {
    Message.warning("确认密码不能为空");
    return false;
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    Message.warning("两次输入的新密码不一致");
    return false;
  }
  return true;
};

const handleChangePassword = async () => {
  if (!validatePasswordForm()) {
    return false;
  }
  try {
    savingPassword.value = true;
    // 预留后端接口位置：推荐新增独立接口 /user/update/password
    // 入参建议：oldPassword, newPassword, confirmPassword
    await UserControllerService.updateMyUserUsingPost({
      userPassword: passwordForm.newPassword,
    });
    Message.success("密码修改成功");
    resetPasswordForm();
    return true;
  } catch (error: unknown) {
    const errorMessage =
      error instanceof Error ? error.message : "密码修改失败";
    Message.error(errorMessage);
    return false;
  } finally {
    savingPassword.value = false;
  }
};

onMounted(() => {
  loadUserInfo();
});
</script>

<style scoped>
.light-page {
  min-height: 100%;
  padding: 20px;
  background: #f5f7fb;
}

.back-home-button {
  margin-bottom: 16px;
  padding: 0;
  color: #667085;
}

.back-home-button:hover {
  color: #4b5565;
}

.profile-header,
.info-card {
  max-width: 980px;
  margin: 0 auto;
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 6px 20px rgba(15, 35, 95, 0.06);
}

.info-card {
  margin-top: 16px;
}

.profile-main {
  display: flex;
  align-items: center;
  gap: 16px;
}

.avatar {
  border: 2px solid #edf1fb;
}

.nickname {
  margin: 0;
  font-size: 24px;
  color: #1f2937;
}

.account {
  margin: 6px 0 0;
  color: #6b7280;
}

.info-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  padding: 14px 4px;
  border-bottom: 1px solid #eef1f6;
}

.info-item:last-child {
  border-bottom: none;
}

.label {
  flex: 0 0 100px;
  color: #6b7280;
  font-weight: 500;
}

.value {
  flex: 1;
  color: #1f2937;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  line-height: 1.6;
}

.avatar-value {
  justify-content: flex-start;
}

.value-text {
  color: #667085;
  word-break: break-all;
}

.password-area {
  align-items: center;
}

@media (max-width: 768px) {
  .info-item {
    flex-direction: column;
    gap: 8px;
  }

  .label {
    flex-basis: auto;
  }
}
</style>
