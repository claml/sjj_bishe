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
        <a-space wrap>
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
          <div class="post-header">
            <a-avatar :size="40" :image-url="item.user?.userAvatar">{{
              item.user?.userName?.[0]
            }}</a-avatar>
            <div>
              <div class="user-name">
                {{ item.user?.userName || "匿名用户" }}
              </div>
              <div class="time-text">{{ formatTime(item.createTime) }}</div>
            </div>
          </div>
          <a-tag v-if="firstTag(item)" color="arcoblue" class="first-tag">{{
            firstTag(item)
          }}</a-tag>
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
          <a-space class="action-text" size="large">
            <a-button type="text" size="small" @click="doThumb(item)"
              >👍 {{ item.thumbNum || 0 }}</a-button
            >
            <a-button type="text" size="small" @click="goPostDetail(item.id)"
              >查看详情</a-button
            >
            <a-button type="text" size="small" @click="doFavour(item)"
              >⭐ {{ item.favourNum || 0 }}</a-button
            >
          </a-space>
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

const router = useRouter();

const parsePostImages = (rawImages: unknown): string[] => {
  if (!rawImages) {
    return [];
  }
  if (Array.isArray(rawImages)) {
    return rawImages.map((item) => String(item || "").trim()).filter(Boolean);
  }
  if (typeof rawImages === "string") {
    const value = rawImages.trim();
    if (!value) {
      return [];
    }
    if (value.startsWith("[")) {
      try {
        const parsed = JSON.parse(value);
        if (Array.isArray(parsed)) {
          return parsed
            .map((item) => String(item || "").trim())
            .filter(Boolean);
        }
      } catch (error) {
        console.warn("帖子图片 JSON 解析失败", error);
      }
    }
    return [value];
  }
  return [];
};

const loadData = async () => {
  loading.value = true;
  try {
    const res = await PostControllerService.listPostVoByPageUsingPost(
      searchParams.value
    );
    if (res.code === 0) {
      postList.value = (res.data.records || []).map((item: any) => ({
        ...item,
        images: parsePostImages(
          item.images ?? item.imageUrls ?? item.picture ?? item.cover
        ),
      }));
      total.value = Number(res.data.total) || 0;
    } else {
      message.error(`加载失败，${res.message}`);
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
    message.error(`发布失败，${res.message}`);
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

const goPostDetail = (postId: string | number) => {
  router.push(`/post/${postId}`);
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

const firstTag = (item: any) => {
  const tags = item.tags || item.tagList || [];
  return tags.length ? tags[0] : "";
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
  background: #fff;
  border-radius: 8px;
  margin-bottom: 12px;
  padding: 16px !important;
  display: block;
}
.post-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}
.user-name {
  font-weight: 600;
}
.time-text {
  color: #86909c;
  font-size: 12px;
}
.first-tag {
  margin-bottom: 8px;
}
.post-content {
  white-space: pre-wrap;
  word-break: break-word;
  color: #1d2129;
  line-height: 1.7;
}
.post-images {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.post-image {
  width: 220px;
  height: 160px;
  border-radius: 8px;
  overflow: hidden;
}
.action-text {
  margin-top: 10px;
  color: #4e5969;
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
</style>
