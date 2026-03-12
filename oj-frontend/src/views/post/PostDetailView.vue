<template>
  <div id="postDetailView">
    <a-spin :loading="loading" style="width: 100%">
      <a-result
        v-if="loadError"
        status="404"
        title="帖子不存在或已删除"
        :subtitle="loadErrorMessage"
      >
        <template #extra>
          <a-button type="primary" @click="goDiscussion">返回讨论页</a-button>
        </template>
      </a-result>

      <template v-else-if="postDetail">
        <a-card class="post-card">
          <template #title>
            <div class="post-title-row">
              <span>{{ postDetail.title || "无标题帖子" }}</span>
              <a-button
                v-if="canDeletePost"
                status="danger"
                type="outline"
                size="small"
                @click="confirmDeletePost"
              >
                删除帖子
              </a-button>
            </div>
          </template>

          <div class="post-header">
            <a-avatar
              :size="42"
              :image-url="postDetail.user?.userAvatar"
              class="clickable-user"
              @click="goUserProfile(postDetail.userId, $event)"
              >{{ postDetail.user?.userName?.[0] }}</a-avatar
            >
            <div>
              <div
                class="user-name clickable-user"
                @click="goUserProfile(postDetail.userId, $event)"
              >
                {{ postDetail.user?.userName || "匿名用户" }}
              </div>
              <div class="time-text">
                {{ formatTime(postDetail.createTime) }}
              </div>
            </div>
          </div>

          <a-tag v-if="firstTag" color="arcoblue" class="first-tag">{{
            firstTag
          }}</a-tag>
          <div class="post-content">{{ postDetail.content }}</div>

          <div v-if="postDetail.images?.length" class="post-images">
            <a-image
              v-for="image in postDetail.images"
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
            <a-button type="text" size="small" @click="doThumb"
              >👍 {{ postDetail.thumbNum || 0 }}</a-button
            >
            <span>评论 {{ commentTotal }}</span>
            <a-button type="text" size="small" @click="doFavour"
              >⭐ {{ postDetail.favourNum || 0 }}</a-button
            >
          </a-space>
        </a-card>

        <a-card class="comment-card" :title="`评论（${commentTotal}）`">
          <a-textarea
            v-model="commentInput"
            :auto-size="{ minRows: 2, maxRows: 4 }"
            placeholder="写下你的评论..."
            allow-clear
          />
          <div class="comment-actions">
            <a-button
              type="primary"
              :loading="submittingComment"
              @click="submitComment"
              >提交评论</a-button
            >
          </div>

          <a-spin :loading="commentLoading">
            <div v-if="commentList.length" class="comment-list">
              <div
                v-for="comment in commentList"
                :key="comment.id"
                class="comment-item"
              >
                <a-avatar :size="32" :image-url="comment.user?.userAvatar">{{
                  comment.user?.userName?.[0]
                }}</a-avatar>
                <div class="comment-main">
                  <div class="comment-meta">
                    <span class="comment-user">{{
                      comment.user?.userName || "匿名用户"
                    }}</span>
                    <span class="comment-time">{{
                      formatTime(comment.createTime)
                    }}</span>
                  </div>
                  <div class="comment-content">{{ comment.content }}</div>
                </div>
              </div>
            </div>
            <a-empty v-else description="暂无评论" />
          </a-spin>
        </a-card>
      </template>
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { Modal } from "@arco-design/web-vue";
import message from "@arco-design/web-vue/es/message";
import moment from "moment";
import store from "@/store";
import ACCESS_ENUM from "@/access/accessEnum";
import {
  PostFavourControllerService,
  PostThumbControllerService,
} from "../../../generated";
import { normalizePost } from "@/utils/postAdapter";
import {
  addPostComment,
  deletePost,
  getErrorMessage,
  getPostDetail,
  listPostComments,
} from "@/services/postApi";
import { openPage, shouldOpenInNewTab } from "@/utils/navigation";

const route = useRoute();
const router = useRouter();

const loading = ref(false);
const loadError = ref(false);
const postDetail = ref<any>();
const loadErrorMessage = ref("请返回讨论页查看其他内容");

const commentLoading = ref(false);
const submittingComment = ref(false);
const commentList = ref<any[]>([]);
const commentTotal = ref(0);
const commentInput = ref("");

const postId = computed(() => String(route.params.id ?? "").trim());

const isValidPostId = computed(() => /^\d+$/.test(postId.value));

const isNotLogin = () => {
  const loginUser = store.state.user.loginUser;
  return !loginUser || loginUser.userRole === ACCESS_ENUM.NOT_LOGIN;
};

const canDeletePost = computed(() => {
  const loginUser = store.state.user.loginUser;
  if (!loginUser || !postDetail.value) {
    return false;
  }
  return (
    Number(loginUser.id) === Number(postDetail.value.userId) ||
    loginUser.userRole === ACCESS_ENUM.ADMIN
  );
});

const firstTag = computed(() => {
  const tags = postDetail.value?.tags || postDetail.value?.tagList || [];
  return tags.length ? tags[0] : "";
});

const formatTime = (time?: string) => {
  if (!time) {
    return "";
  }
  return moment(time).format("YYYY-MM-DD HH:mm");
};

const goDiscussion = () => {
  router.push("/discussion");
};

const goUserProfile = (userId: string | number, event?: MouseEvent) => {
  if (!userId) {
    return;
  }
  return openPage(router, `/user/${userId}`, {
    newTab: shouldOpenInNewTab(event),
  });
};

const loadPostDetail = async () => {
  if (!isValidPostId.value) {
    loadError.value = true;
    loadErrorMessage.value = "帖子 id 无效";
    return;
  }
  loading.value = true;
  loadError.value = false;
  try {
    const detail = await getPostDetail(Number(postId.value));
    postDetail.value = normalizePost(detail);
    await loadComments();
  } catch (error) {
    const errorMessage = getErrorMessage(error, "帖子加载失败，请稍后重试");
    loadError.value = true;
    loadErrorMessage.value = errorMessage;
    message.error(errorMessage);
  } finally {
    loading.value = false;
  }
};

const loadComments = async () => {
  commentLoading.value = true;
  try {
    const pageData = await listPostComments(Number(postId.value));
    commentList.value = pageData.records || [];
    commentTotal.value = Number(pageData.total) || commentList.value.length;
  } catch (error) {
    message.error(getErrorMessage(error, "评论加载失败"));
  } finally {
    commentLoading.value = false;
  }
};

const submitComment = async () => {
  const content = commentInput.value.trim();
  if (!content) {
    message.warning("评论不能为空");
    return;
  }
  if (isNotLogin()) {
    message.warning("请先登录");
    return;
  }
  submittingComment.value = true;
  try {
    await addPostComment(Number(postId.value), content);
    commentInput.value = "";
    message.success("评论成功");
    await loadComments();
  } catch (error) {
    message.error(getErrorMessage(error, "评论失败"));
  } finally {
    submittingComment.value = false;
  }
};

const doThumb = async () => {
  if (isNotLogin()) {
    message.warning("请先登录");
    return;
  }
  const res = await PostThumbControllerService.doThumbUsingPost({
    postId: postId.value as any,
  });
  if (res.code === 0) {
    postDetail.value.thumbNum =
      (postDetail.value.thumbNum || 0) + (res.data || 0);
  } else {
    message.error(res.message || "操作失败");
  }
};

const doFavour = async () => {
  if (isNotLogin()) {
    message.warning("请先登录");
    return;
  }
  const res = await PostFavourControllerService.doPostFavourUsingPost({
    postId: postId.value as any,
  });
  if (res.code === 0) {
    postDetail.value.favourNum =
      (postDetail.value.favourNum || 0) + (res.data || 0);
  } else {
    message.error(res.message || "操作失败");
  }
};

const confirmDeletePost = () => {
  Modal.confirm({
    title: "确认删除",
    content: "确定删除这条帖子吗？删除后不可恢复",
    okText: "确认删除",
    cancelText: "取消",
    okButtonProps: {
      status: "danger",
    },
    onOk: async () => {
      try {
        await deletePost(Number(postId.value));
        message.success("删除成功");
        await router.push("/discussion");
      } catch (error) {
        message.error(getErrorMessage(error, "删除失败"));
      }
    },
  });
};

onMounted(async () => {
  await store.dispatch("user/getLoginUser");
  await loadPostDetail();
});

watch(
  () => route.params.id,
  async () => {
    await loadPostDetail();
  }
);
</script>

<style scoped>
#postDetailView {
  max-width: 960px;
  margin: 0 auto;
}
.post-card,
.comment-card {
  margin-bottom: 16px;
}
.post-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
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
.clickable-user {
  cursor: pointer;
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
.comment-actions {
  margin: 10px 0 12px;
  text-align: right;
}
.comment-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.comment-item {
  display: flex;
  gap: 8px;
}
.comment-main {
  flex: 1;
  background: #f7f8fa;
  border-radius: 8px;
  padding: 8px 10px;
}
.comment-meta {
  font-size: 12px;
  color: #86909c;
  display: flex;
  justify-content: space-between;
}
.comment-user {
  font-weight: 600;
  color: #1d2129;
}
.comment-content {
  margin-top: 4px;
  white-space: pre-wrap;
  word-break: break-word;
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
