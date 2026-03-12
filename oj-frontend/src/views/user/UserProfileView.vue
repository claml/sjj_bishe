<template>
  <div id="userProfileView" class="profile-page">
    <a-spin :loading="pageLoading" style="width: 100%">
      <a-result
        v-if="loadFailed"
        status="error"
        title="个人主页加载失败"
        :subtitle="errorMessage"
      >
        <template #extra>
          <a-button type="primary" @click="loadAllData">重试</a-button>
        </template>
      </a-result>

      <div v-else class="profile-layout">
        <section class="left-panel">
          <a-card :bordered="false" class="user-card">
            <div class="user-main">
              <a-avatar :size="88" :image-url="userInfo.userAvatar">
                {{ (userInfo.userName || "匿")[0] }}
              </a-avatar>
              <h2 class="user-name">{{ userInfo.userName || "未设置昵称" }}</h2>
              <p class="user-profile">
                {{ userInfo.userProfile || "这个人很懒，还没有填写简介" }}
              </p>
            </div>
            <a-divider style="margin: 14px 0" />
            <div class="stat-list">
              <div class="stat-item">
                <span class="label">发帖数</span>
                <span class="value">{{ postTotal }}</span>
              </div>
              <div class="stat-item">
                <span class="label">上传题目数</span>
                <span class="value">{{ questionTotal }}</span>
              </div>
              <div v-if="isSelf" class="stat-item">
                <span class="label">收藏帖子数</span>
                <span class="value">{{ favourTotal }}</span>
              </div>
              <div class="stat-item">
                <span class="label">注册时间</span>
                <span class="value">{{ formatTime(userInfo.createTime) }}</span>
              </div>
            </div>
          </a-card>
        </section>

        <section class="right-panel">
          <a-card :bordered="false" class="content-card">
            <a-tabs v-model:active-key="activeTab" lazy-load>
              <a-tab-pane key="post" title="用户帖子">
                <a-list
                  :data="postList"
                  :loading="postLoading"
                  :pagination-props="{
                    current: postQuery.current,
                    pageSize: postQuery.pageSize,
                    total: postTotal,
                    showTotal: true,
                  }"
                  @page-change="onPostPageChange"
                >
                  <template #empty>
                    <a-empty description="暂无帖子" />
                  </template>
                  <template #item="{ item }">
                    <a-list-item class="list-item">
                      <div
                        class="list-title"
                        @click="goPostDetail(item.id, $event)"
                      >
                        {{ item.title || "无标题帖子" }}
                      </div>
                      <div class="list-content">{{ item.content }}</div>
                    </a-list-item>
                  </template>
                </a-list>
              </a-tab-pane>

              <a-tab-pane key="question" title="上传题目">
                <a-table
                  :loading="questionLoading"
                  :data="questionList"
                  :columns="questionColumns"
                  :pagination="{
                    current: questionQuery.current,
                    pageSize: questionQuery.pageSize,
                    total: questionTotal,
                    showTotal: true,
                  }"
                  @page-change="onQuestionPageChange"
                >
                  <template #createTime="{ record }">
                    {{ formatTime(record.createTime) }}
                  </template>
                  <template #optional="{ record }">
                    <a-button
                      type="text"
                      size="small"
                      @click="goQuestion(record.id, $event)"
                    >
                      去做题
                    </a-button>
                  </template>
                </a-table>
              </a-tab-pane>

              <a-tab-pane v-if="isSelf" key="favour" title="收藏帖子">
                <a-list
                  :data="favourList"
                  :loading="favourLoading"
                  :pagination-props="{
                    current: favourQuery.current,
                    pageSize: favourQuery.pageSize,
                    total: favourTotal,
                    showTotal: true,
                  }"
                  @page-change="onFavourPageChange"
                >
                  <template #empty>
                    <a-empty description="暂无收藏帖子" />
                  </template>
                  <template #item="{ item }">
                    <a-list-item class="list-item">
                      <div
                        class="list-title"
                        @click="goPostDetail(item.id, $event)"
                      >
                        {{ item.title || "无标题帖子" }}
                      </div>
                      <div class="list-content">{{ item.content }}</div>
                    </a-list-item>
                  </template>
                </a-list>
              </a-tab-pane>
            </a-tabs>
          </a-card>
        </section>
      </div>
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from "vue";
import { Message } from "@arco-design/web-vue";
import { useRoute, useRouter } from "vue-router";
import store from "@/store";
import {
  PostControllerService,
  PostFavourControllerService,
  QuestionControllerService,
  UserControllerService,
} from "../../../generated";
import { openPage, shouldOpenInNewTab } from "@/utils/navigation";

const route = useRoute();
const router = useRouter();

const pageLoading = ref(false);
const loadFailed = ref(false);
const errorMessage = ref("请稍后重试");
const activeTab = ref("post");

const postLoading = ref(false);
const questionLoading = ref(false);
const favourLoading = ref(false);

const userInfo = reactive<any>({
  id: "",
  userAvatar: "",
  userName: "",
  userProfile: "",
  createTime: "",
});

const postList = ref<any[]>([]);
const questionList = ref<any[]>([]);
const favourList = ref<any[]>([]);

const postTotal = ref(0);
const questionTotal = ref(0);
const favourTotal = ref(0);

const postQuery = reactive({
  current: 1,
  pageSize: 5,
});

const questionQuery = reactive({
  current: 1,
  pageSize: 5,
});

const favourQuery = reactive({
  current: 1,
  pageSize: 5,
});

const profileUserId = computed(() => String(route.params.id || "").trim());

const loginUser = computed(() => store.state.user.loginUser || {});

const isSelf = computed(() => {
  return String(loginUser.value?.id || "") === profileUserId.value;
});

const questionColumns = [
  {
    title: "题号",
    dataIndex: "id",
  },
  {
    title: "题目",
    dataIndex: "title",
  },
  {
    title: "创建时间",
    slotName: "createTime",
  },
  {
    title: "操作",
    slotName: "optional",
  },
];

const formatTime = (time?: string) => {
  if (!time) {
    return "--";
  }
  const date = new Date(time);
  if (Number.isNaN(date.getTime())) {
    return time;
  }
  return date.toLocaleString("zh-CN", { hour12: false });
};

const goPostDetail = (postId: string | number, event?: MouseEvent) => {
  return openPage(router, `/post/${postId}`, {
    newTab: shouldOpenInNewTab(event),
  });
};

const goQuestion = (questionId: string | number, event?: MouseEvent) => {
  return openPage(router, `/view/question/${questionId}`, {
    newTab: shouldOpenInNewTab(event),
  });
};

const loadUserInfo = async () => {
  const userId = Number(profileUserId.value);
  if (!userId) {
    throw new Error("用户 id 无效");
  }
  const res = await UserControllerService.getUserVoByIdUsingGet(userId);
  if (res.code !== 0 || !res.data) {
    throw new Error(res.message || "用户信息获取失败");
  }
  Object.assign(userInfo, res.data);
};

const loadPostList = async () => {
  postLoading.value = true;
  try {
    const res = await PostControllerService.listPostVoByPageUsingPost({
      userId: Number(profileUserId.value),
      current: postQuery.current,
      pageSize: postQuery.pageSize,
    } as any);
    if (res.code !== 0) {
      throw new Error(res.message || "帖子加载失败");
    }
    postList.value = res.data.records || [];
    postTotal.value = Number(res.data.total) || 0;
  } finally {
    postLoading.value = false;
  }
};

const loadQuestionList = async () => {
  questionLoading.value = true;
  try {
    const res = await QuestionControllerService.listQuestionVoByPageUsingPost({
      userId: Number(profileUserId.value),
      current: questionQuery.current,
      pageSize: questionQuery.pageSize,
    } as any);
    if (res.code !== 0) {
      throw new Error(res.message || "题目加载失败");
    }
    questionList.value = res.data.records || [];
    questionTotal.value = Number(res.data.total) || 0;
  } finally {
    questionLoading.value = false;
  }
};

const loadFavourList = async () => {
  if (!isSelf.value) {
    favourList.value = [];
    favourTotal.value = 0;
    return;
  }
  favourLoading.value = true;
  try {
    const res =
      await PostFavourControllerService.listMyFavourPostByPageUsingPost({
        current: favourQuery.current,
        pageSize: favourQuery.pageSize,
      } as any);
    if (res.code !== 0) {
      throw new Error(res.message || "收藏加载失败");
    }
    favourList.value = res.data.records || [];
    favourTotal.value = Number(res.data.total) || 0;
  } finally {
    favourLoading.value = false;
  }
};

const loadAllData = async () => {
  pageLoading.value = true;
  loadFailed.value = false;
  try {
    await Promise.all([loadUserInfo(), loadPostList(), loadQuestionList()]);
    await loadFavourList();
  } catch (error: any) {
    loadFailed.value = true;
    errorMessage.value = error?.message || "加载失败";
    Message.error(errorMessage.value);
  } finally {
    pageLoading.value = false;
  }
};

const onPostPageChange = async (page: number) => {
  postQuery.current = page;
  await loadPostList();
};

const onQuestionPageChange = async (page: number) => {
  questionQuery.current = page;
  await loadQuestionList();
};

const onFavourPageChange = async (page: number) => {
  favourQuery.current = page;
  await loadFavourList();
};

onMounted(async () => {
  await store.dispatch("user/getLoginUser");
  await loadAllData();
});

watch(
  () => route.params.id,
  async () => {
    postQuery.current = 1;
    questionQuery.current = 1;
    favourQuery.current = 1;
    activeTab.value = "post";
    await loadAllData();
  }
);
</script>

<style scoped>
.profile-page {
  max-width: 1200px;
  margin: 0 auto;
}

.profile-layout {
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: 16px;
}

.user-card,
.content-card {
  border-radius: 12px;
}

.user-main {
  text-align: center;
}

.user-name {
  margin: 10px 0 6px;
}

.user-profile {
  margin: 0;
  color: #86909c;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.stat-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.stat-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #4e5969;
}

.list-item {
  display: block;
}

.list-title {
  font-weight: 600;
  color: #1d2129;
  cursor: pointer;
}

.list-title:hover {
  color: rgb(var(--arcoblue-6));
}

.list-content {
  margin-top: 6px;
  color: #4e5969;
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 900px) {
  .profile-layout {
    grid-template-columns: 1fr;
  }
}
</style>
