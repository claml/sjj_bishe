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

      <div v-if="!loadFailed" class="profile-layout">
        <section class="left-panel">
          <a-card :bordered="false" class="user-card">
            <div class="user-main">
              <a-avatar
                :size="88"
                :image-url="userInfo.userAvatar"
                class="clickable-user"
                @click="goUserProfile(userInfo.id, $event)"
              >
                {{ (userInfo.userName || "匿")[0] }}
              </a-avatar>
              <h2 class="user-name">{{ userInfo.userName || "未设置昵称" }}</h2>
              <p class="user-profile">
                {{ userInfo.userProfile || "这个人很懒，还没有填写简介" }}
              </p>
            </div>
            <a-divider style="margin: 14px 0" />
            <div class="stat-grid">
              <div class="stat-card">
                <span class="label">发帖数</span>
                <span class="value">{{ postTotal }}</span>
              </div>
              <div class="stat-card">
                <span class="label">注册时间</span>
                <span class="value">{{ formatTime(userInfo.createTime) }}</span>
              </div>
            </div>
          </a-card>
        </section>

        <section class="right-panel">
          <a-card :bordered="false" class="heatmap-card">
            <template #title>做题记录</template>
            <a-spin :loading="heatmapLoading" style="width: 100%">
              <div class="heatmap-header">
                <div class="heatmap-title">
                  过去一年共通过
                  <span class="heatmap-highlight">{{
                    yearlyAcceptedCount
                  }}</span>
                  题
                </div>
                <div class="heatmap-stats">
                  活跃天数：{{ activeDays }} 天
                  <span class="stat-divider">·</span>
                  连续通过：{{ longestStreak }} 天
                </div>
              </div>
              <div
                class="heatmap-body"
                :style="{
                  '--week-count': String(heatmapWeekCount),
                }"
              >
                <div class="heatmap-grid">
                  <div
                    v-for="cell in heatmapCells"
                    :key="cell.key"
                    class="heatmap-cell"
                    :class="{
                      empty: cell.empty,
                      level0: !cell.empty && cell.level === 0,
                      level1: !cell.empty && cell.level === 1,
                      level2: !cell.empty && cell.level === 2,
                      level3: !cell.empty && cell.level === 3,
                      level4: !cell.empty && cell.level === 4,
                    }"
                    :title="
                      cell.empty ? '' : `${cell.date}：${cell.count} 次通过`
                    "
                  />
                </div>
                <div class="heatmap-months">
                  <span
                    v-for="item in heatmapMonthLabels"
                    :key="item.key"
                    class="month-label"
                    :style="{ gridColumnStart: String(item.week + 1) }"
                  >
                    {{ item.label }}
                  </span>
                </div>
              </div>
            </a-spin>
          </a-card>

          <a-card :bordered="false" class="content-card">
            <a-tabs
              v-model:active-key="activeTab"
              lazy-load
              class="profile-tabs"
            >
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

              <a-tab-pane key="question" title="提交题目">
                <a-table
                  class="question-table"
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
                  <template #questionNo="{ record }">
                    {{ record.questionNo ?? "--" }}
                  </template>
                  <template #questionTitle="{ record }">
                    <a-button
                      type="text"
                      size="small"
                      @click="goQuestion(record.questionId, $event)"
                    >
                      {{ record.questionTitle || `题目 ${record.questionId}` }}
                    </a-button>
                  </template>
                  <template #submitTime="{ record }">
                    {{ formatTime(record.submitTime) }}
                  </template>
                  <template #optional="{ record }">
                    <a-button
                      type="text"
                      size="small"
                      @click="showSubmitCode(record)"
                    >
                      查看代码
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

          <a-modal
            v-model:visible="codeModalVisible"
            title="最后一次成功代码"
            :width="900"
            unmount-on-close
          >
            <div class="submit-meta">
              <span>题目：{{ selectedSubmit?.questionTitle || "--" }}</span>
              <span>语言：{{ selectedSubmit?.language || "--" }}</span>
              <span>时间：{{ formatTime(selectedSubmit?.submitTime) }}</span>
            </div>
            <pre class="submit-code">{{
              selectedSubmit?.code || "暂无代码"
            }}</pre>
          </a-modal>
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
  UserControllerService,
} from "../../../generated";
import { openPage, shouldOpenInNewTab } from "@/utils/navigation";
import {
  AcceptedQuestionSubmitItem,
  listAcceptedQuestionSubmitByPage,
} from "@/services/questionSubmitApi";

const route = useRoute();
const router = useRouter();

const pageLoading = ref(false);
const loadFailed = ref(false);
const errorMessage = ref("请稍后重试");
const activeTab = ref("post");
const codeModalVisible = ref(false);
interface ProfileQuestionSubmitItem extends AcceptedQuestionSubmitItem {
  questionNo?: number;
}
const selectedSubmit = ref<ProfileQuestionSubmitItem | null>(null);
const heatmapLoading = ref(false);
const acceptedHistory = ref<{ submitTime?: string }[]>([]);

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
const questionList = ref<ProfileQuestionSubmitItem[]>([]);
const favourList = ref<any[]>([]);

const postTotal = ref(0);
const questionTotal = ref(0);
const favourTotal = ref(0);

const postQuery = reactive({ current: 1, pageSize: 5 });
const questionQuery = reactive({ current: 1, pageSize: 5 });
const favourQuery = reactive({ current: 1, pageSize: 5 });

const profileUserId = computed(() => String(route.params.id || "").trim());
const loginUser = computed(() => store.state.user.loginUser || {});
const isSelf = computed(
  () => String(loginUser.value?.id || "") === profileUserId.value
);

interface HeatmapCell {
  key: string;
  date: string;
  count: number;
  level: number;
  empty?: boolean;
}

interface HeatmapMonthLabel {
  key: string;
  week: number;
  label: string;
}

const questionColumns = [
  { title: "题号", slotName: "questionNo" },
  { title: "题目", slotName: "questionTitle" },
  { title: "语言", dataIndex: "language" },
  { title: "最后通过时间", slotName: "submitTime" },
  { title: "操作", slotName: "optional" },
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

const DAY_MS = 24 * 60 * 60 * 1000;

const formatDateKey = (date: Date) => {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
};

const monthText = (month: number) => `${month + 1}月`;

const heatmapRange = computed(() => {
  const end = new Date();
  end.setHours(0, 0, 0, 0);
  const start = new Date(end);
  start.setDate(end.getDate() - 364);
  return { start, end };
});

const heatmapDayCountMap = computed(() => {
  const map = new Map<string, number>();
  const { start, end } = heatmapRange.value;
  const startTime = start.getTime();
  const endTime = end.getTime();
  acceptedHistory.value.forEach((item) => {
    if (!item.submitTime) {
      return;
    }
    const date = new Date(item.submitTime);
    if (Number.isNaN(date.getTime())) {
      return;
    }
    date.setHours(0, 0, 0, 0);
    const time = date.getTime();
    if (time < startTime || time > endTime) {
      return;
    }
    const key = formatDateKey(date);
    map.set(key, (map.get(key) || 0) + 1);
  });
  return map;
});

const yearlyAcceptedCount = computed(() => {
  let totalCount = 0;
  heatmapDayCountMap.value.forEach((value) => {
    totalCount += value;
  });
  return totalCount;
});

const activeDays = computed(() => heatmapDayCountMap.value.size);

const longestStreak = computed(() => {
  const { start, end } = heatmapRange.value;
  let maxStreak = 0;
  let currentStreak = 0;
  for (let time = start.getTime(); time <= end.getTime(); time += DAY_MS) {
    const dateKey = formatDateKey(new Date(time));
    if ((heatmapDayCountMap.value.get(dateKey) || 0) > 0) {
      currentStreak += 1;
      maxStreak = Math.max(maxStreak, currentStreak);
    } else {
      currentStreak = 0;
    }
  }
  return maxStreak;
});

const heatmapWeekCount = computed(() => {
  const { start, end } = heatmapRange.value;
  const totalDays = Math.floor((end.getTime() - start.getTime()) / DAY_MS) + 1;
  const leading = start.getDay();
  return Math.ceil((leading + totalDays) / 7);
});

const resolveCellLevel = (count: number, maxCount: number) => {
  if (count <= 0) {
    return 0;
  }
  if (maxCount <= 1) {
    return 4;
  }
  const ratio = count / maxCount;
  if (ratio <= 0.25) {
    return 1;
  }
  if (ratio <= 0.5) {
    return 2;
  }
  if (ratio <= 0.75) {
    return 3;
  }
  return 4;
};

const heatmapCells = computed<HeatmapCell[]>(() => {
  const { start, end } = heatmapRange.value;
  const cells: HeatmapCell[] = [];
  const leading = start.getDay();
  const maxCount = Math.max(
    ...Array.from(heatmapDayCountMap.value.values()),
    0
  );
  for (let i = 0; i < leading; i += 1) {
    cells.push({
      key: `empty-start-${i}`,
      date: "",
      count: 0,
      level: 0,
      empty: true,
    });
  }
  for (let time = start.getTime(); time <= end.getTime(); time += DAY_MS) {
    const date = new Date(time);
    const dateKey = formatDateKey(date);
    const count = heatmapDayCountMap.value.get(dateKey) || 0;
    cells.push({
      key: dateKey,
      date: dateKey,
      count,
      level: resolveCellLevel(count, maxCount),
    });
  }
  const trailing = (7 - (cells.length % 7)) % 7;
  for (let i = 0; i < trailing; i += 1) {
    cells.push({
      key: `empty-end-${i}`,
      date: "",
      count: 0,
      level: 0,
      empty: true,
    });
  }
  return cells;
});

const heatmapMonthLabels = computed<HeatmapMonthLabel[]>(() => {
  const { start, end } = heatmapRange.value;
  const labels: HeatmapMonthLabel[] = [];
  const seen = new Set<string>();
  for (let time = start.getTime(); time <= end.getTime(); time += DAY_MS) {
    const date = new Date(time);
    const key = `${date.getFullYear()}-${date.getMonth()}`;
    if (seen.has(key)) {
      continue;
    }
    seen.add(key);
    const dayOffset = Math.floor((time - start.getTime()) / DAY_MS);
    const week = Math.floor((start.getDay() + dayOffset) / 7);
    labels.push({
      key,
      week,
      label: monthText(date.getMonth()),
    });
  }
  return labels;
});

const goPostDetail = (postId: string | number, event?: MouseEvent) => {
  return openPage(router, `/post/${postId}`, {
    newTab: shouldOpenInNewTab(event),
  });
};

const goQuestion = (
  questionId: string | number | undefined,
  event?: MouseEvent
) => {
  if (!questionId) {
    return;
  }
  return openPage(router, `/view/question/${questionId}`, {
    newTab: shouldOpenInNewTab(event),
  });
};

const showSubmitCode = (record: ProfileQuestionSubmitItem) => {
  selectedSubmit.value = record;
  codeModalVisible.value = true;
};

const goUserProfile = (userId: string | number, event?: MouseEvent) => {
  if (!userId) {
    return;
  }
  return openPage(router, `/user/${userId}`, {
    newTab: shouldOpenInNewTab(event),
  });
};

const getProfileUserIdOrThrow = () => {
  const userId = profileUserId.value;
  if (!/^[1-9]\d*$/.test(userId)) {
    throw new Error("用户 id 无效");
  }
  return userId;
};

const loadUserInfo = async () => {
  const userId = getProfileUserIdOrThrow();
  const res = await UserControllerService.getUserVoByIdUsingGet(userId as any);
  if (res.code !== 0 || !res.data) {
    throw new Error(res.message || "用户信息获取失败");
  }
  Object.assign(userInfo, res.data);
};

const loadPostList = async () => {
  postLoading.value = true;
  try {
    const res = await PostControllerService.listPostVoByPageUsingPost({
      userId: getProfileUserIdOrThrow(),
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
    const res = await listAcceptedQuestionSubmitByPage({
      userId: getProfileUserIdOrThrow(),
      current: questionQuery.current,
      pageSize: questionQuery.pageSize,
    });
    questionList.value = res.records || [];
    questionTotal.value = Number(res.total) || 0;
  } finally {
    questionLoading.value = false;
  }
};

const loadQuestionHeatmap = async () => {
  heatmapLoading.value = true;
  try {
    const userId = getProfileUserIdOrThrow();
    const pageSize = 50;
    const allRecords: AcceptedQuestionSubmitItem[] = [];
    let current = 1;
    let total = 0;
    let guard = 0;

    while (guard < 200) {
      guard += 1;
      const pageData = await listAcceptedQuestionSubmitByPage({
        userId,
        current,
        pageSize,
      });
      const records = pageData.records || [];
      total = Number(pageData.total || 0);
      allRecords.push(...records);

      if (allRecords.length >= total || records.length === 0) {
        break;
      }
      current += 1;
    }
    acceptedHistory.value = allRecords;
  } finally {
    heatmapLoading.value = false;
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
    await Promise.all([
      loadUserInfo(),
      loadPostList(),
      loadQuestionList(),
      loadQuestionHeatmap(),
    ]);
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
    codeModalVisible.value = false;
    selectedSubmit.value = null;
    acceptedHistory.value = [];
    await loadAllData();
  }
);
</script>

<style scoped>
.profile-page {
  --card-radius: 16px;
  max-width: 1280px;
  margin: 0 auto;
  padding: 8px 0 24px;
  position: relative;
}

.profile-page::before {
  content: "";
  position: absolute;
  inset: 0 0 auto;
  height: 220px;
  border-radius: 20px;
  background: radial-gradient(
      120% 120% at 0% 0%,
      rgba(20, 115, 230, 0.16),
      transparent 60%
    ),
    linear-gradient(140deg, #f7fbff 0%, #edf4ff 45%, #f8fcff 100%);
  pointer-events: none;
}

.profile-layout {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 16px;
  position: relative;
  z-index: 1;
}

.left-panel {
  position: sticky;
  top: 10px;
  align-self: start;
}

.user-card,
.content-card,
.heatmap-card {
  border-radius: var(--card-radius);
  border: 1px solid #e5edf6;
  box-shadow: 0 12px 30px rgba(15, 30, 60, 0.08);
}

.user-card,
.content-card {
  background: #ffffffd9;
  backdrop-filter: blur(4px);
}

.heatmap-card {
  margin-bottom: 16px;
  background: linear-gradient(160deg, #f8fcff 0%, #eef6ff 100%);
}

.heatmap-card :deep(.arco-card-header-title) {
  color: #1f3a56;
}

.heatmap-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: baseline;
  color: #334e68;
  margin-bottom: 12px;
}

.heatmap-title {
  font-size: 16px;
}

.heatmap-highlight {
  font-size: 34px;
  line-height: 1;
  font-weight: 700;
  color: #0f6ad8;
}

.heatmap-stats {
  color: #52606d;
  font-size: 14px;
}

.stat-divider {
  margin: 0 6px;
  color: #829ab1;
}

.heatmap-body {
  overflow-x: auto;
  padding-bottom: 6px;
}

.heatmap-grid {
  display: grid;
  grid-auto-flow: column;
  grid-template-rows: repeat(7, 12px);
  grid-template-columns: repeat(var(--week-count), 12px);
  grid-auto-columns: 12px;
  gap: 4px;
  width: max-content;
  min-width: 100%;
}

.heatmap-cell {
  width: 12px;
  height: 12px;
  border-radius: 3px;
}

.heatmap-cell.empty,
.heatmap-cell.level0 {
  background: #dde7f2;
}

.heatmap-cell.level1 {
  background: #d3ebd8;
}

.heatmap-cell.level2 {
  background: #b2dfbc;
}

.heatmap-cell.level3 {
  background: #7ccf92;
}

.heatmap-cell.level4 {
  background: #3bac66;
}

.heatmap-months {
  margin-top: 8px;
  display: grid;
  grid-template-columns: repeat(var(--week-count), 12px);
  gap: 4px;
  width: max-content;
  min-width: 100%;
}

.month-label {
  color: #6b7f95;
  font-size: 12px;
  white-space: nowrap;
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

.stat-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 10px;
}

.stat-card {
  border: 1px solid #dce7f5;
  border-radius: 12px;
  background: #f8fbff;
  padding: 10px 12px;
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 12px;
}

.stat-card .label {
  color: #52606d;
  font-size: 13px;
}

.stat-card .value {
  color: #0f6ad8;
  font-weight: 700;
}

.list-item {
  display: block;
  padding: 12px 14px;
  border: 1px solid #e3edf8;
  border-radius: 12px;
  background: #fbfdff;
  transition: all 0.2s ease;
}

.list-item:hover {
  border-color: #c9defc;
  box-shadow: 0 8px 20px rgba(20, 60, 120, 0.08);
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
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.content-card :deep(.arco-card-body) {
  padding: 14px 16px 16px;
}

.profile-tabs :deep(.arco-tabs-nav::before) {
  height: 1px;
  background: #e5edf6;
}

.profile-tabs :deep(.arco-tabs-tab-title) {
  font-weight: 600;
}

.question-table :deep(.arco-table-th) {
  font-weight: 700;
  color: #243b53;
  background: #f7faff;
}

.question-table :deep(.arco-table-td) {
  vertical-align: middle;
}

.clickable-user {
  cursor: pointer;
}

.submit-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 12px;
  color: #4e5969;
}

.submit-code {
  margin: 0;
  max-height: 60vh;
  overflow: auto;
  background: #0f172a;
  color: #e5e7eb;
  border-radius: 8px;
  padding: 12px;
  line-height: 1.5;
  white-space: pre;
}

@media (max-width: 900px) {
  .profile-page {
    padding-top: 2px;
  }

  .profile-layout {
    grid-template-columns: 1fr;
  }

  .left-panel {
    position: static;
  }

  .heatmap-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
