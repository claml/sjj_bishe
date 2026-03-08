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
        <div
          class="avatar-edit-wrapper avatar-large"
          @click="triggerAvatarUpload"
        >
          <a-tooltip content="编辑头像" position="top">
            <div class="avatar-clickable">
              <img class="avatar-image" :src="displayAvatar" alt="用户头像" />
              <div class="avatar-overlay">
                <icon-edit class="edit-icon" />
              </div>
            </div>
          </a-tooltip>
        </div>
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
            <div
              class="avatar-edit-wrapper avatar-small"
              @click="triggerAvatarUpload"
            >
              <a-tooltip content="编辑头像" position="top">
                <div class="avatar-clickable">
                  <img
                    class="avatar-image"
                    :src="displayAvatar"
                    alt="用户头像"
                  />
                  <div class="avatar-overlay">
                    <icon-edit class="edit-icon" />
                  </div>
                </div>
              </a-tooltip>
            </div>
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
                @click="openPasswordDialog"
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

    <input
      ref="avatarInputRef"
      class="hidden-file-input"
      type="file"
      accept=".jpg,.jpeg,.png,.webp,image/jpeg,image/png,image/webp"
      @change="handleAvatarFileChange"
    />

    <transition name="fade">
      <div
        v-if="passwordDialogVisible"
        class="password-dialog-mask"
        @click="closePasswordDialog"
      >
        <div class="password-dialog" @click.stop>
          <button
            class="dialog-close"
            type="button"
            @click="closePasswordDialog"
          >
            <icon-close />
          </button>
          <h3 class="dialog-title">更新密码</h3>

          <div class="dialog-form-item">
            <label class="dialog-label">当前密码</label>
            <a-input-password
              v-model="passwordForm.oldPassword"
              class="dialog-input"
              placeholder="请输入当前密码"
              allow-clear
            />
          </div>

          <div class="dialog-form-item">
            <label class="dialog-label">新密码</label>
            <a-input-password
              v-model="passwordForm.newPassword"
              class="dialog-input"
              placeholder="请输入新密码"
              allow-clear
            />
            <p class="password-hint">
              密码必须至少包含 8 个字符，包含字母和数字，且不允许特殊字符或非
              ASCII 字符。
            </p>
          </div>

          <div class="dialog-actions">
            <a-button @click="closePasswordDialog">取消</a-button>
            <a-button
              type="primary"
              :loading="passwordSubmitting"
              @click="handleChangePassword"
            >
              保存
            </a-button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { Message } from "@arco-design/web-vue";
import { IconClose, IconEdit, IconLeft } from "@arco-design/web-vue/es/icon";
import { useRouter } from "vue-router";
import { UserControllerService } from "../../../generated";

const defaultAvatar =
  "https://p3-passport.byteimg.com/img/user-avatar-placeholder.jpeg~180x180.awebp";
const allowedAvatarTypes = ["image/jpeg", "image/png", "image/webp"];
const maxAvatarSize = 2 * 1024 * 1024;

const router = useRouter();
const avatarInputRef = ref<HTMLInputElement | null>(null);

const userInfo = reactive({
  userAvatar: "",
  userName: "",
  userAccount: "",
  userPassword: "",
  userProfile: "",
  userRole: "user",
  createTime: "",
});

const displayAvatar = computed(() => {
  const avatar = userInfo.userAvatar?.trim();
  return avatar ? avatar : defaultAvatar;
});

const maskedPassword = "••••••••";
const displayedPassword = computed(
  () => userInfo.userPassword || maskedPassword
);
const showPassword = ref(false);

const passwordDialogVisible = ref(false);
const passwordSubmitting = ref(false);
const passwordForm = reactive({
  oldPassword: "",
  newPassword: "",
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
  // 预留获取用户信息接口位置
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
  userInfo.userPassword = "";
};

const triggerAvatarUpload = () => {
  avatarInputRef.value?.click();
};

const validateAvatarFile = (file: File) => {
  if (!allowedAvatarTypes.includes(file.type)) {
    Message.warning("头像仅支持 jpg、jpeg、png、webp 格式");
    return false;
  }
  if (file.size > maxAvatarSize) {
    Message.warning("头像大小不能超过 2MB");
    return false;
  }
  return true;
};

const handleAvatarFileChange = async (event: Event) => {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) {
    return;
  }
  if (!validateAvatarFile(file)) {
    input.value = "";
    return;
  }

  try {
    // 预留上传头像接口位置，可替换为真实上传逻辑
    // 示例：const uploadRes = await UserControllerService.uploadUserAvatarUsingPost(file)
    const localPreviewUrl = URL.createObjectURL(file);
    userInfo.userAvatar = localPreviewUrl;

    // 预留更新头像接口位置
    // await UserControllerService.updateMyUserUsingPost({ userAvatar: uploadRes.data.url })

    Message.success("头像上传成功");
  } catch (error: unknown) {
    const errorMessage =
      error instanceof Error ? error.message : "头像上传失败";
    Message.error(errorMessage);
  } finally {
    input.value = "";
  }
};

const togglePassword = () => {
  showPassword.value = !showPassword.value;
};

const openPasswordDialog = () => {
  passwordDialogVisible.value = true;
};

const resetPasswordForm = () => {
  passwordForm.oldPassword = "";
  passwordForm.newPassword = "";
};

const closePasswordDialog = () => {
  if (passwordSubmitting.value) {
    return;
  }
  passwordDialogVisible.value = false;
  resetPasswordForm();
};

const validatePasswordForm = () => {
  if (!passwordForm.oldPassword) {
    Message.warning("当前密码不能为空");
    return false;
  }
  if (!passwordForm.newPassword) {
    Message.warning("新密码不能为空");
    return false;
  }
  if (passwordForm.newPassword.length < 8) {
    Message.warning("新密码长度至少 8 位");
    return false;
  }
  const isAsciiOnly = passwordForm.newPassword
    .split("")
    .every((char) => char.charCodeAt(0) <= 127);
  if (!isAsciiOnly) {
    Message.warning("新密码只能使用 ASCII 字符");
    return false;
  }
  if (
    !/[A-Za-z]/.test(passwordForm.newPassword) ||
    !/[0-9]/.test(passwordForm.newPassword)
  ) {
    Message.warning("新密码必须同时包含字母和数字");
    return false;
  }
  if (!/^[A-Za-z0-9]+$/.test(passwordForm.newPassword)) {
    Message.warning("新密码不允许包含特殊字符");
    return false;
  }
  return true;
};

const handleChangePassword = async () => {
  if (!validatePasswordForm()) {
    return;
  }

  try {
    passwordSubmitting.value = true;
    // 预留修改密码接口位置
    await UserControllerService.updateMyUserUsingPost({
      userPassword: passwordForm.newPassword,
    });
    Message.success("密码修改成功");
    closePasswordDialog();
  } catch (error: unknown) {
    const errorMessage =
      error instanceof Error ? error.message : "密码修改失败";
    Message.error(errorMessage);
  } finally {
    passwordSubmitting.value = false;
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

.avatar-edit-wrapper {
  position: relative;
  border-radius: 50%;
}

.avatar-clickable {
  position: relative;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  border: 2px solid #edf1fb;
}

.avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.avatar-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(15, 23, 42, 0.35);
  color: #fff;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.avatar-edit-wrapper:hover .avatar-overlay {
  opacity: 1;
}

.edit-icon {
  font-size: 20px;
}

.avatar-large {
  width: 80px;
  height: 80px;
}

.avatar-small {
  width: 44px;
  height: 44px;
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

.hidden-file-input {
  display: none;
}

.password-dialog-mask {
  position: fixed;
  inset: 0;
  z-index: 1200;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(15, 23, 42, 0.28);
}

.password-dialog {
  position: relative;
  width: min(100%, 580px);
  padding: 28px;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.16);
}

.dialog-close {
  position: absolute;
  top: 16px;
  right: 16px;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 8px;
  background: #f2f4f7;
  color: #475467;
  cursor: pointer;
}

.dialog-close:hover {
  background: #e7ebf3;
}

.dialog-title {
  margin: 0 0 20px;
  font-size: 22px;
  color: #1f2937;
}

.dialog-form-item {
  margin-bottom: 16px;
}

.dialog-label {
  display: inline-block;
  margin-bottom: 8px;
  color: #344054;
  font-weight: 500;
}

.password-hint {
  margin: 8px 0 0;
  color: #667085;
  font-size: 13px;
  line-height: 1.5;
}

:deep(.dialog-input .arco-input-wrapper),
:deep(.dialog-input .arco-input-password) {
  border-radius: 10px;
  background: #fafbfd;
  border: 1px solid #d0d5dd;
}

:deep(.dialog-input .arco-input-wrapper:hover),
:deep(.dialog-input .arco-input-password:hover) {
  border-color: #98a2b3;
}

:deep(.dialog-input .arco-input-wrapper.arco-input-focus),
:deep(.dialog-input .arco-input-password.arco-input-focus) {
  border-color: #5b8ff9;
  box-shadow: 0 0 0 3px rgba(91, 143, 249, 0.18);
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@media (max-width: 768px) {
  .info-item {
    flex-direction: column;
    gap: 8px;
  }

  .label {
    flex-basis: auto;
  }

  .password-dialog {
    padding: 20px;
  }
}
</style>
