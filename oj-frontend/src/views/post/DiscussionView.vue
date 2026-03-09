<template>
  <div id="discussionView">
    <a-card class="publish-card" title="发布讨论">
      <a-space direction="vertical" fill>
        <a-textarea
          v-model="publishForm.content"
          :max-length="2000"
          :auto-size="{ minRows: 4, maxRows: 8 }"
          allow-clear
          placeholder="分享你的想法..."
        />
        <a-space wrap>
          <a-input
            v-model="publishForm.title"
            style="width: 260px"
            placeholder="标题（可选）"
            allow-clear
          />
          <a-input-tag
            v-model="publishForm.tags"
            placeholder="标签（可选）"
            allow-clear
          />
          <a-button
            type="primary"
            :loading="publishing"
            :disabled="!publishForm.content?.trim()"
            @click="doPublish"
          >
            发布
          </a-button>
        </a-space>
      </a-space>
    </a-card>

    <a-list
      class="post-list"
      :loading="loading"
      :data="postList"
      :bordered="false"
      :pagination-props="{
        current: searchParams.current,
        pageSize: searchParams.pageSize,
        total,
        showTotal: true,
      }"
      @page-change="onPageChange"
    >
      <template #empty>
        <a-empty description="暂无帖子，快来发布第一条讨论吧" />
      </template>
      <template #item="{ item }">
        <a-list-item class="post-item">
          <a-list-item-meta>
            <template #avatar>
              <a-avatar :size="40" :image-url="item.user?.userAvatar">
                {{ item.user?.userName?.[0] }}
              </a-avatar>
            </template>
            <template #title>
              <a-space>
                <span class="user-name">{{ item.user?.userName || "匿名用户" }}</span>
                <span class="time-text">{{ formatTime(item.createTime) }}</span>
              </a-space>
            </template>
            <template #description>
              <div class="post-content">{{ item.content }}</div>
              <a-space v-if="item.tags?.length || item.tagList?.length" wrap class="tag-space">
                <a-tag v-for="tag in item.tags || item.tagList" :key="tag" color="arcoblue">
                  {{ tag }}
                </a-tag>
              </a-space>
              <a-space class="action-text" size="large">
                <span>👍 {{ item.thumbNum || 0 }}</span>
                <span>⭐ {{ item.favourNum || 0 }}</span>
              </a-space>
            </template>
          </a-list-item-meta>
        </a-list-item>
      </template>
    </a-list>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { PostControllerService, PostQueryRequest } from "../../../generated";
import message from "@arco-design/web-vue/es/message";
import moment from "moment";
import store from "@/store";
import ACCESS_ENUM from "@/access/accessEnum";

const publishForm = ref({
  title: "",
  content: "",
  tags: [] as string[],
});
const publishing = ref(false);
const loading = ref(false);
const postList = ref<any[]>([]);
const total = ref(0);
const searchParams = ref<PostQueryRequest>({
  current: 1,
  pageSize: 10,
});

const loadData = async () => {
  loading.value = true;
  try {
    const res = await PostControllerService.listPostVoByPageUsingPost(searchParams.value);
    if (res.code === 0) {
      postList.value = res.data.records || [];
      total.value = Number(res.data.total) || 0;
    } else {
      message.error(`加载失败，${res.message}`);
    }
  } finally {
    loading.value = false;
  }
};

const doPublish = async () => {
  if (!publishForm.value.content?.trim()) {
    message.warning("内容不能为空");
    return;
  }
  const loginUser = store.state.user.loginUser;
  if (!loginUser || loginUser.userRole === ACCESS_ENUM.NOT_LOGIN) {
    message.warning("请先登录");
    return;
  }
  publishing.value = true;
  try {
    const res = await PostControllerService.addPostUsingPost({
      title: publishForm.value.title?.trim(),
      content: publishForm.value.content?.trim(),
      tags: publishForm.value.tags,
    });
    if (res.code === 0) {
      message.success("发布成功");
      publishForm.value = { title: "", content: "", tags: [] };
      searchParams.value.current = 1;
      await loadData();
      return;
    }
    message.error(`发布失败，${res.message}`);
  } finally {
    publishing.value = false;
  }
};

const onPageChange = (page: number) => {
  searchParams.value.current = page;
  loadData();
};

const formatTime = (time?: string) => {
  if (!time) {
    return "";
  }
  return moment(time).format("YYYY-MM-DD HH:mm");
};

onMounted(async () => {
  await store.dispatch("user/getLoginUser");
  await loadData();
});
</script>

<style scoped>
#discussionView {
  max-width: 960px;
  margin: 0 auto;
}

.publish-card {
  margin-bottom: 16px;
}

.post-item {
  background: #fff;
  border-radius: 8px;
  margin-bottom: 12px;
  padding: 16px !important;
}

.user-name {
  font-weight: 600;
}

.time-text {
  color: #86909c;
  font-size: 12px;
}

.post-content {
  margin-top: 8px;
  white-space: pre-wrap;
  word-break: break-word;
  color: #1d2129;
}

.tag-space {
  margin-top: 10px;
}

.action-text {
  margin-top: 10px;
  color: #4e5969;
}
</style>
