<template>
  <div id="discussionView">
    <a-card class="publish-card" title="发布讨论" :bordered="false">
      <a-space direction="vertical" fill>
        <a-textarea
          v-model="publishForm.content"
          :max-length="2000"
          :auto-size="{ minRows: 4, maxRows: 8 }"
          allow-clear
          placeholder="分享你的想法..."
        />
        <div
          v-if="publishForm.images.length"
          class="publish-image-preview-list"
        >
          <div
            v-for="(url, index) in publishForm.images"
            :key="url"
            class="preview-item"
          >
            <a-image :src="url" width="120" height="120" fit="cover">
              <template #error>
                <div class="img-error">图片加载失败</div>
              </template>
            </a-image>
            <a-button
              type="text"
              size="mini"
              status="danger"
              @click="removePublishImage(index)"
              >移除</a-button
            >
          </div>
        </div>
        <a-space wrap class="publish-actions">
          <a-input
            v-model="publishForm.title"
            style="width: 240px"
            placeholder="标题（可选）"
            allow-clear
          />
          <a-input-tag
            v-model="publishForm.tags"
            placeholder="标签（可选）"
            allow-clear
          />
          <a-button :loading="uploadingImage" @click="triggerImageSelect"
            >上传图片</a-button
          >
          <input
            ref="postImageInputRef"
            type="file"
            accept="image/png,image/jpeg,image/webp"
            style="display: none"
            @change="handlePostImageSelect"
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

    <a-card class="search-card" :bordered="false">
      <a-space wrap class="search-actions">
        <a-input
          v-model="searchForm.title"
          class="search-input"
          placeholder="搜索帖子标题"
          allow-clear
          @press-enter="doSearch"
        />
        <a-input-tag
          v-model="searchForm.tags"
          class="search-tag-input"
          placeholder="按标签搜索"
          allow-clear
        />
        <a-button type="primary" @click="doSearch">搜索</a-button>
        <a-button @click="resetSearch">重置</a-button>
      </a-space>
    </a-card>

    <div
      v-if="
        searchParams.title || (searchParams.tags && searchParams.tags.length)
      "
      class="active-filters"
    >
      <span class="active-label">当前筛选</span>
      <a-tag v-if="searchParams.title" color="arcoblue" bordered>
        标题：{{ searchParams.title }}
      </a-tag>
      <a-tag
        v-for="tag in searchParams.tags || []"
        :key="`active-${tag}`"
        color="orangered"
        bordered
      >
        标签：{{ tag }}
      </a-tag>
      <a-button type="text" size="mini" @click="resetSearch">清空筛选</a-button>
    </div>
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
          <article class="post-card">
            <header class="post-header">
              <a-avatar
                :size="42"
                :image-url="item.user?.userAvatar"
                class="clickable-user avatar-ring"
                @click="goUserProfile(item.userId, $event)"
                >{{ item.user?.userName?.[0] }}</a-avatar
              >
              <div class="post-meta">
                <div
                  class="user-name clickable-user"
                  @click="goUserProfile(item.userId, $event)"
                >
                  {{ item.user?.userName || "匿名用户" }}
                </div>
                <div class="time-text">{{ formatTime(item.createTime) }}</div>
              </div>
            </header>

            <section class="post-body">
              <div v-if="item.tags?.length" class="post-tags">
                <a-space wrap size="mini">
                  <a-tag
                    v-for="tag in item.tags"
                    :key="`${item.id}-${tag}`"
                    color="arcoblue"
                    bordered
                    class="clickable-tag"
                    @click="searchByTag(tag)"
                  >
                    {{ tag }}
                  </a-tag>
                </a-space>
              </div>
              <h3 v-if="item.title" class="post-title">{{ item.title }}</h3>
              <div class="post-content">{{ item.content }}</div>
              <div v-if="item.images?.length" class="post-images">
                <a-image
                  v-for="image in item.images"
                  :key="image"
                  :src="image"
                  class="post-image"
                  :width="220"
                  :height="160"
                  fit="cover"
                  :preview="true"
                >
                  <template #error>
                    <div class="img-error">图片加载失败</div>
                  </template>
                </a-image>
              </div>
            </section>

            <footer class="action-text">
              <a-button
                class="action-btn"
                type="text"
                size="small"
                @click="doThumb(item)"
                >点赞 {{ item.thumbNum || 0 }}</a-button
              >
              <a-button
                class="action-btn"
                type="text"
                size="small"
                @click="doFavour(item)"
                >收藏 {{ item.favourNum || 0 }}</a-button
              >
              <a-button
                class="action-btn"
                type="text"
                size="small"
                @click="goPostDetail(item.id, $event)"
                >查看详情</a-button
              >
            </footer>
          </article>
        </a-list-item>
      </template>
    </a-list>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import {
  FileControllerService,
  PostControllerService,
  PostFavourControllerService,
  PostQueryRequest,
  PostThumbControllerService,
} from "../../../generated";
import message from "@arco-design/web-vue/es/message";
import moment from "moment";
import store from "@/store";
import ACCESS_ENUM from "@/access/accessEnum";
import { useRouter } from "vue-router";
import { normalizePost } from "@/utils/postAdapter";
import { openPage, shouldOpenInNewTab } from "@/utils/navigation";

const publishForm = ref({
  title: "",
  content: "",
  tags: [] as string[],
  images: [] as string[],
});
const publishing = ref(false);
const uploadingImage = ref(false);
const postImageInputRef = ref<HTMLInputElement | null>(null);
const loading = ref(false);
const postList = ref<any[]>([]);
const total = ref(0);
const searchParams = ref<PostQueryRequest>({
  current: 1,
  pageSize: 10,
});
const searchForm = ref({
  title: "",
  tags: [] as string[],
});

const router = useRouter();

const loadData = async () => {
  loading.value = true;
  try {
    const res = await PostControllerService.listPostVoByPageUsingPost(
      searchParams.value
    );
    if (res.code === 0) {
      postList.value = (res.data.records || []).map((item: any) =>
        normalizePost(item)
      );
      total.value = Number(res.data.total) || 0;
    } else {
      message.error(`加载失败：${res.message}`);
    }
  } finally {
    loading.value = false;
  }
};

const isNotLogin = () => {
  const loginUser = store.state.user.loginUser;
  return !loginUser || loginUser.userRole === ACCESS_ENUM.NOT_LOGIN;
};

const triggerImageSelect = () => {
  postImageInputRef.value?.click();
};

const handlePostImageSelect = async (event: Event) => {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) {
    return;
  }
  uploadingImage.value = true;
  try {
    const res = await FileControllerService.uploadFileUsingPost(
      file,
      "post_image"
    );
    if (res.code !== 0 || !res.data) {
      message.error(res.message || "图片上传失败");
      return;
    }
    publishForm.value.images.push(res.data);
    message.success("图片上传成功");
  } finally {
    uploadingImage.value = false;
    input.value = "";
  }
};

const removePublishImage = (index: number) => {
  publishForm.value.images.splice(index, 1);
};

const doPublish = async () => {
  if (!publishForm.value.content?.trim()) {
    message.warning("内容不能为空");
    return;
  }
  if (isNotLogin()) {
    message.warning("请先登录");
    return;
  }
  publishing.value = true;
  try {
    const res = await PostControllerService.addPostUsingPost({
      title: publishForm.value.title?.trim(),
      content: publishForm.value.content?.trim(),
      tags: publishForm.value.tags,
      images: publishForm.value.images,
    } as any);
    if (res.code === 0) {
      message.success("发布成功");
      publishForm.value = { title: "", content: "", tags: [], images: [] };
      searchParams.value.current = 1;
      await loadData();
      return;
    }
    message.error(`发布失败：${res.message}`);
  } finally {
    publishing.value = false;
  }
};

const doThumb = async (item: any) => {
  if (isNotLogin()) {
    message.warning("请先登录");
    return;
  }
  const res = await PostThumbControllerService.doThumbUsingPost({
    postId: item.id,
  });
  if (res.code === 0) {
    item.thumbNum = (item.thumbNum || 0) + (res.data || 0);
  } else {
    message.error(res.message || "操作失败");
  }
};

const doFavour = async (item: any) => {
  if (isNotLogin()) {
    message.warning("请先登录");
    return;
  }
  const res = await PostFavourControllerService.doPostFavourUsingPost({
    postId: item.id,
  });
  if (res.code === 0) {
    item.favourNum = (item.favourNum || 0) + (res.data || 0);
  } else {
    message.error(res.message || "操作失败");
  }
};

const goPostDetail = (postId: string | number, event?: MouseEvent) => {
  return openPage(router, `/post/${postId}`, {
    newTab: shouldOpenInNewTab(event),
  });
};

const goUserProfile = (userId: string | number, event?: MouseEvent) => {
  if (!userId) {
    return;
  }
  return openPage(router, `/user/${userId}`, {
    newTab: shouldOpenInNewTab(event),
  });
};

const onPageChange = (page: number) => {
  searchParams.value.current = page;
  loadData();
};

const normalizeSearchTags = (rawTags: string[]) => {
  const safeTags = Array.isArray(rawTags) ? rawTags : [];
  const normalizedTags = safeTags
    .map((tag) => String(tag ?? "").trim())
    .filter(Boolean);
  return Array.from(new Set(normalizedTags));
};

const applySearchParamsFromForm = () => {
  const normalizedTitle = String(searchForm.value.title ?? "").trim();
  const normalizedTags = normalizeSearchTags(searchForm.value.tags);
  searchForm.value = {
    title: normalizedTitle,
    tags: normalizedTags,
  };
  searchParams.value = {
    ...searchParams.value,
    title: normalizedTitle || undefined,
    tags: normalizedTags.length ? normalizedTags : undefined,
    current: 1,
  };
};

const doSearch = async () => {
  applySearchParamsFromForm();
  await loadData();
};

const resetSearch = async () => {
  searchForm.value = {
    title: "",
    tags: [],
  };
  searchParams.value = {
    ...searchParams.value,
    title: undefined,
    tags: undefined,
    current: 1,
  };
  await loadData();
};

const searchByTag = async (tag: string) => {
  const normalizedTag = String(tag ?? "").trim();
  if (!normalizedTag) {
    return;
  }
  searchForm.value = {
    title: "",
    tags: [normalizedTag],
  };
  searchParams.value = {
    ...searchParams.value,
    title: undefined,
    tags: [normalizedTag],
    current: 1,
  };
  await loadData();
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
  position: relative;
  max-width: 980px;
  margin: 0 auto;
  padding: 8px 0 24px;
}

#discussionView::before {
  content: "";
  position: absolute;
  inset: 0;
  height: 280px;
  background: radial-gradient(
    circle at 20% 0%,
    #e9f2ff 0%,
    #f6f9ff 55%,
    transparent 75%
  );
  z-index: -1;
  pointer-events: none;
}

.publish-card,
.search-card {
  margin-bottom: 16px;
  border-radius: 16px;
  border: 1px solid #e6edf8;
  box-shadow: 0 8px 20px rgba(21, 65, 121, 0.06);
}

.publish-card :deep(.arco-card-header),
.search-card :deep(.arco-card-header) {
  border-bottom: none;
}

.publish-card :deep(.arco-card-header-title) {
  font-weight: 700;
  color: #2a4f80;
}

.publish-card :deep(.arco-card-body),
.search-card :deep(.arco-card-body) {
  padding: 16px 18px;
}

.publish-actions,
.search-actions {
  width: 100%;
}

.search-input {
  width: 280px;
}

.search-tag-input {
  min-width: 280px;
}

.active-filters {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin: -4px 0 12px;
  padding: 0 2px;
}

.active-label {
  font-size: 13px;
  font-weight: 600;
  color: #4f6686;
}

.publish-image-preview-list {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.preview-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.post-item {
  margin-bottom: 16px;
  padding: 0 !important;
  border: none;
  display: block;
}

.post-card {
  position: relative;
  overflow: hidden;
  background: linear-gradient(180deg, #ffffff 0%, #fcfdff 100%);
  border: 1px solid #e5ecf7;
  border-radius: 14px;
  box-shadow: 0 6px 18px rgba(26, 58, 102, 0.05);
  padding: 16px;
  transition: transform 0.18s ease, box-shadow 0.18s ease,
    border-color 0.18s ease;
}

.post-card::before {
  content: "";
  position: absolute;
  left: 0;
  top: 0;
  width: 100%;
  height: 3px;
  opacity: 0;
  background: linear-gradient(90deg, #5aa5ff 0%, #4bc0c8 100%);
  transition: opacity 0.18s ease;
}

.post-card:hover {
  transform: translateY(-2px);
  border-color: #cdddf5;
  box-shadow: 0 10px 26px rgba(26, 58, 102, 0.12);
}

.post-card:hover::before {
  opacity: 1;
}

.post-header {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 10px;
}

.avatar-ring {
  border: 2px solid #e8f1ff;
}

.post-meta {
  min-width: 0;
}

.user-name {
  font-size: 15px;
  font-weight: 700;
  color: #1f2937;
}

.clickable-user {
  cursor: pointer;
}

.time-text {
  color: #8b99ab;
  font-size: 12px;
}

.post-tags {
  margin-bottom: 8px;
}

.clickable-tag {
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.clickable-tag:hover {
  opacity: 0.96;
  transform: translateY(-1px);
  box-shadow: 0 4px 10px rgba(60, 118, 214, 0.2);
}

.post-title {
  margin: 0 0 8px;
  color: #1b365f;
  font-size: 18px;
  font-weight: 700;
}

.post-content {
  white-space: pre-wrap;
  word-break: break-word;
  color: #243b53;
  line-height: 1.75;
}

.post-images {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.post-image {
  width: 220px;
  height: 160px;
  border-radius: 10px;
  overflow: hidden;
}

.post-image :deep(img) {
  transition: transform 0.25s ease;
}

.post-image:hover :deep(img) {
  transform: scale(1.04);
}

.action-text {
  margin-top: 14px;
  padding-top: 10px;
  border-top: 1px dashed #e8edf5;
  color: #4e5969;
  display: flex;
  align-items: center;
  gap: 8px;
}

.action-btn {
  border-radius: 999px;
}

.action-btn:hover {
  background: #edf4ff;
  color: #1d5fbf;
}

.img-error {
  width: 100%;
  height: 100%;
  background: #f2f3f5;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #86909c;
  font-size: 12px;
}

@media (max-width: 768px) {
  #discussionView {
    padding: 8px 10px 24px;
  }

  .search-input,
  .search-tag-input {
    width: 100%;
    min-width: 100%;
  }

  .post-image {
    width: calc(50% - 5px);
    height: 130px;
  }

  .action-text {
    flex-wrap: wrap;
  }
}

@media (max-width: 520px) {
  .post-image {
    width: 100%;
    height: 170px;
  }
}
</style>
