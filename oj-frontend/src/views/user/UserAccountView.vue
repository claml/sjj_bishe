<template>
  <div id="userAccountView">
    <h2 style="margin-bottom: 20px">账号信息</h2>
    <a-form :model="form" layout="vertical" @submit="handleSubmit">
      <a-form-item field="userAccount" label="账号">
        <a-input
          v-model="form.userAccount"
          disabled
          placeholder="当前登录账号"
        />
      </a-form-item>
      <a-form-item field="userName" label="用户名">
        <a-input v-model="form.userName" placeholder="请输入用户名" />
      </a-form-item>
      <a-form-item field="userProfile" label="个人简介">
        <a-textarea v-model="form.userProfile" placeholder="请输入个人简介" />
      </a-form-item>
      <a-form-item
        field="userPassword"
        tooltip="不修改密码可留空，密码不少于 8 位"
        label="新密码"
      >
        <a-input
          v-model="form.userPassword"
          :type="showPassword ? 'text' : 'password'"
          placeholder="请输入新密码"
        >
          <template #append>
            <a-button type="text" @click="togglePassword">
              {{ showPassword ? "隐藏" : "显示" }}
            </a-button>
          </template>
        </a-input>
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit" style="min-width: 100px">
          保存修改
        </a-button>
      </a-form-item>
    </a-form>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from "vue";
import { Message } from "@arco-design/web-vue";
import { UserControllerService } from "../../../generated";
import { useStore } from "vuex";

const store = useStore();
const showPassword = ref(false);

const form = reactive({
  userAccount: "",
  userName: "",
  userProfile: "",
  userPassword: "",
});

const initForm = async () => {
  const res = await UserControllerService.getLoginUserUsingGet();
  if (res.code !== 0 || !res.data) {
    Message.error("获取用户信息失败");
    return;
  }
  form.userAccount = res.data.userAccount ?? "";
  form.userName = res.data.userName ?? "";
  form.userProfile = res.data.userProfile ?? "";
};

onMounted(async () => {
  await initForm();
});

const togglePassword = () => {
  showPassword.value = !showPassword.value;
};

const handleSubmit = async () => {
  if (form.userPassword && form.userPassword.length < 8) {
    Message.error("密码长度不能少于 8 位");
    return;
  }
  const res = await UserControllerService.updateMyUserUsingPost({
    userName: form.userName,
    userProfile: form.userProfile,
    userPassword: form.userPassword || undefined,
  });
  if (res.code === 0) {
    Message.success("保存成功");
    form.userPassword = "";
    showPassword.value = false;
    await store.dispatch("user/getLoginUser");
    return;
  }
  Message.error("保存失败，" + res.message);
};
</script>

<style scoped>
#userAccountView {
  max-width: 720px;
  margin: 0 auto;
}
</style>
