<template>
  <div class="question-discussion-panel">
    <a-card class="discussion-publish-card" title="讨论">
      <a-textarea
        v-model="discussionInput"
        :auto-size="{ minRows: 3, maxRows: 6 }"
        :max-length="2000"
        allow-clear
        placeholder="说说你对这道题的思路、疑问或优化建议"
      />
      <div class="discussion-actions">
        <a-button
          type="primary"
          :loading="submitting"
          :disabled="!discussionInput.trim()"
          @click="submitDiscussion"
        >
          发布讨论
        </a-button>
      </div>
    </a-card>

    <a-card class="discussion-list-card" :title="`讨论列表（${total}）`">
      <a-spin :loading="loading">
        <a-list
          v-if="discussionList.length"
          :data="discussionList"
          :bordered="false"
          :pagination-props="{
            current,
            pageSize,
            total,
            showTotal: true,
          }"
          @page-change="onPageChange"
        >
          <template #item="{ item }">
            <a-list-item class="discussion-item">
              <div class="discussion-item-main">
                <div class="discussion-user-line">
                  <a-avatar
                    :size="32"
                    :image-url="item.user?.userAvatar"
                    class="discussion-avatar"
                  >
                    {{ item.user?.userName?.[0] || "?" }}
                  </a-avatar>
                  <span class="discussion-user-name">
                    {{ item.user?.userName || "匿名用户" }}
                  </span>
                  <span class="discussion-time">
                    {{ formatTime(item.createTime) }}
                  </span>
                </div>
                <div class="discussion-content">{{ item.content }}</div>
                <a-space class="discussion-item-actions">
                  <a-button
                    type="text"
                    size="small"
                    :status="item.hasThumb ? 'normal' : 'default'"
                    @click="toggleThumb(item)"
                  >
                    点赞 {{ item.thumbNum || 0 }}
                  </a-button>
                  <a-button
                    v-if="canDelete(item)"
                    type="text"
                    size="small"
                    status="danger"
                    @click="confirmDelete(item)"
                  >
                    删除
                  </a-button>
                </a-space>
              </div>
            </a-list-item>
          </template>
        </a-list>
        <a-empty v-else description="暂无讨论，来发布第一条吧" />
      </a-spin>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import {
  computed,
  onMounted,
  ref,
  watch,
  withDefaults,
  defineProps,
} from "vue";
import { Modal } from "@arco-design/web-vue";
import message from "@arco-design/web-vue/es/message";
import ACCESS_ENUM from "@/access/accessEnum";
import store from "@/store";
import {
  QuestionDiscussionVO,
  addQuestionDiscussion,
  deleteQuestionDiscussion,
  doQuestionDiscussionThumb,
  getQuestionDiscussionError,
  listQuestionDiscussions,
} from "@/services/questionDiscussionApi";

interface Props {
  questionId: string | number | undefined;
}

const props = withDefaults(defineProps<Props>(), {
  questionId: "",
});

const loading = ref(false);
const submitting = ref(false);
const discussionList = ref<QuestionDiscussionVO[]>([]);
const total = ref(0);
const current = ref(1);
const pageSize = ref(20);
const discussionInput = ref("");

const normalizedQuestionId = computed(() => {
  const raw = props.questionId;
  if (raw === undefined || raw === null) {
    return "";
  }
  const normalized = String(raw).trim();
  return /^\d+$/.test(normalized) ? normalized : "";
});

const loginUser = computed(() => store.state.user.loginUser);

const isNotLogin = () => {
  return !loginUser.value || loginUser.value.userRole === ACCESS_ENUM.NOT_LOGIN;
};

const canDelete = (item: QuestionDiscussionVO) => {
  if (!loginUser.value) {
    return false;
  }
  return (
    Number(loginUser.value.id) === Number(item.userId) ||
    loginUser.value.userRole === ACCESS_ENUM.ADMIN
  );
};

const loadDiscussions = async (targetPage = current.value) => {
  if (!normalizedQuestionId.value) {
    discussionList.value = [];
    total.value = 0;
    return;
  }
  loading.value = true;
  try {
    const pageData = await listQuestionDiscussions(
      normalizedQuestionId.value,
      targetPage,
      pageSize.value
    );
    discussionList.value = pageData.records || [];
    total.value = Number(pageData.total) || discussionList.value.length;
    current.value = Number(pageData.current) || targetPage;
  } catch (error) {
    message.error(getQuestionDiscussionError(error, "加载讨论失败"));
  } finally {
    loading.value = false;
  }
};

const submitDiscussion = async () => {
  const content = discussionInput.value.trim();
  if (!content) {
    message.warning("讨论内容不能为空");
    return;
  }
  if (isNotLogin()) {
    message.warning("请先登录");
    return;
  }
  submitting.value = true;
  try {
    await addQuestionDiscussion(normalizedQuestionId.value, content);
    discussionInput.value = "";
    message.success("发布成功");
    await loadDiscussions(1);
  } catch (error) {
    message.error(getQuestionDiscussionError(error, "发布讨论失败"));
  } finally {
    submitting.value = false;
  }
};

const toggleThumb = async (item: QuestionDiscussionVO) => {
  if (isNotLogin()) {
    message.warning("请先登录");
    return;
  }
  try {
    await doQuestionDiscussionThumb(String(item.id));
    await loadDiscussions(1);
  } catch (error) {
    message.error(getQuestionDiscussionError(error, "点赞失败"));
  }
};

const confirmDelete = (item: QuestionDiscussionVO) => {
  Modal.confirm({
    title: "确认删除讨论",
    content: "删除后不可恢复，是否继续？",
    okButtonProps: {
      status: "danger",
    },
    onOk: async () => {
      try {
        await deleteQuestionDiscussion(String(item.id));
        message.success("删除成功");
        await loadDiscussions(1);
      } catch (error) {
        message.error(getQuestionDiscussionError(error, "删除失败"));
      }
    },
  });
};

const onPageChange = async (page: number) => {
  current.value = page;
  await loadDiscussions(page);
};

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

onMounted(async () => {
  await store.dispatch("user/getLoginUser");
  await loadDiscussions(1);
});

watch(
  () => normalizedQuestionId.value,
  async () => {
    current.value = 1;
    await loadDiscussions(1);
  }
);
</script>

<style scoped>
.question-discussion-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.discussion-actions {
  margin-top: 10px;
  text-align: right;
}

.discussion-item {
  padding: 10px 0;
}

.discussion-item-main {
  width: 100%;
}

.discussion-user-line {
  display: flex;
  align-items: center;
  gap: 8px;
}

.discussion-avatar {
  flex-shrink: 0;
}

.discussion-user-name {
  font-weight: 600;
  color: #1d2129;
}

.discussion-time {
  color: #86909c;
  font-size: 12px;
}

.discussion-content {
  margin-top: 8px;
  white-space: pre-wrap;
  word-break: break-word;
  color: #1d2129;
}

.discussion-item-actions {
  margin-top: 8px;
}
</style>
