<template>
  <div id="homeView">
    <section class="today-banner">
      <div class="today-big">{{ todayDateText }}</div>
      <div class="today-week">{{ todayWeekText }}</div>
    </section>

    <a-card class="checkin-card" :bordered="false">
      <div class="checkin-wrapper">
        <div>
          <h3 class="checkin-title">每日签到</h3>
          <p class="checkin-desc">
            {{
              signedToday
                ? "今天已签到，继续冲刺比赛吧！"
                : "签到可获得今日趣味彩蛋。"
            }}
          </p>
        </div>
        <a-button type="primary" :disabled="signedToday" @click="handleSignIn">
          {{ signedToday ? "今日已签到" : "立即签到" }}
        </a-button>
      </div>
    </a-card>

    <section class="event-columns">
      <a-card class="event-card" :bordered="false">
        <template #title>近期比赛</template>
        <a-spin :loading="loading">
          <a-empty
            v-if="!loading && contestEventList.length === 0"
            description="管理员暂未配置近期比赛"
          />
          <div v-else class="event-list">
            <article
              v-for="item in contestEventList"
              :key="item.id"
              class="event-item"
            >
              <div class="event-content">
                <div class="event-head">
                  <a-tag color="arcoblue" bordered>{{
                    item.contestType
                  }}</a-tag>
                  <span class="event-time">{{
                    formatDateTime(item.eventTime)
                  }}</span>
                </div>
                <h4 class="event-title">{{ item.title }}</h4>
                <p v-if="item.description" class="event-description">
                  {{ item.description }}
                </p>
              </div>
              <div class="event-side">
                <span class="event-location">{{
                  item.location || "线上 / 待定地点"
                }}</span>
                <span class="event-remain">{{
                  getRemainText(item.eventTime)
                }}</span>
                <a-link
                  v-if="item.eventUrl"
                  :href="getExternalLink(item.eventUrl)"
                  target="_blank"
                  rel="noopener noreferrer"
                  class="event-link"
                >
                  查看详情
                </a-link>
              </div>
            </article>
          </div>
        </a-spin>
      </a-card>

      <a-card class="event-card" :bordered="false">
        <template #title>赛训通知</template>
        <a-spin :loading="loading">
          <a-empty
            v-if="!loading && noticeEventList.length === 0"
            description="暂无赛训通知"
          />
          <div v-else class="event-list">
            <article
              v-for="item in noticeEventList"
              :key="item.id"
              class="event-item"
            >
              <div class="event-content">
                <div class="event-head">
                  <a-tag color="gold" bordered>{{ item.contestType }}</a-tag>
                  <span class="event-time">{{
                    formatDateTime(item.eventTime)
                  }}</span>
                </div>
                <h4 class="event-title">{{ item.title }}</h4>
                <p v-if="item.description" class="event-description">
                  {{ item.description }}
                </p>
              </div>
              <div class="event-side">
                <span class="event-location">{{
                  item.location || "训练安排通知"
                }}</span>
                <a-link
                  v-if="item.eventUrl"
                  :href="getExternalLink(item.eventUrl)"
                  target="_blank"
                  rel="noopener noreferrer"
                  class="event-link"
                >
                  查看详情
                </a-link>
              </div>
            </article>
          </div>
        </a-spin>
      </a-card>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { useStore } from "vuex";
import { Message, Modal } from "@arco-design/web-vue";
import moment from "moment";
import {
  getErrorMessage,
  HomeEventItem,
  listUpcomingHomeEvents,
} from "@/services/homeEventApi";

const store = useStore();
const loading = ref(false);
const eventList = ref<HomeEventItem[]>([]);
const signedToday = ref(false);
const NOTICE_TYPES = ["赛训", "赛训通知"];

const surpriseList = [
  "今日彩蛋：调试像侦探，提交像诗人。",
  "今日彩蛋：AC 之前都叫数据采样。",
  "今日彩蛋：再写 20 行，也许就是正解。",
  "今日彩蛋：喝水、拉伸、再写代码，效率更高。",
  "今日彩蛋：先过样例，再想极限数据。",
];

const todayDateText = computed(() => moment().format("YYYY年MM月DD日"));

const todayWeekText = computed(() => {
  const weekMap = [
    "星期日",
    "星期一",
    "星期二",
    "星期三",
    "星期四",
    "星期五",
    "星期六",
  ];
  return weekMap[new Date().getDay()];
});

const loginUser = computed(() => store.state.user?.loginUser ?? {});

const canSignIn = computed(() => {
  const userRole = loginUser.value?.userRole;
  return Boolean(loginUser.value?.id && userRole && userRole !== "notLogin");
});

const signInKey = computed(() => {
  if (!canSignIn.value) {
    return "";
  }
  return `home_sign_in_${loginUser.value.id}_${moment().format("YYYY-MM-DD")}`;
});

const refreshSignState = () => {
  if (!signInKey.value) {
    signedToday.value = false;
    return;
  }
  signedToday.value = Boolean(localStorage.getItem(signInKey.value));
};

const loadEventList = async () => {
  loading.value = true;
  try {
    eventList.value = await listUpcomingHomeEvents(20);
  } catch (error) {
    Message.error(getErrorMessage(error, "加载比赛信息失败"));
  } finally {
    loading.value = false;
  }
};

const contestEventList = computed(() =>
  eventList.value.filter(
    (item) => !NOTICE_TYPES.includes(item.contestType || "")
  )
);

const noticeEventList = computed(() =>
  eventList.value.filter((item) =>
    NOTICE_TYPES.includes(item.contestType || "")
  )
);

const formatDateTime = (value: string) => {
  return moment(value).format("YYYY-MM-DD HH:mm");
};

const getRemainText = (value: string) => {
  const today = moment().startOf("day");
  const targetDay = moment(value).startOf("day");
  const diffDays = targetDay.diff(today, "days");
  if (diffDays <= 0) {
    return "今天";
  }
  return `还有 ${diffDays} 天`;
};

const getExternalLink = (url?: string) => {
  const trimmedUrl = (url || "").trim();
  if (!trimmedUrl) {
    return "";
  }
  if (/^(https?:)?\/\//i.test(trimmedUrl)) {
    return trimmedUrl;
  }
  return `https://${trimmedUrl}`;
};

const getRandomSurprise = () => {
  const randomIndex = Math.floor(Math.random() * surpriseList.length);
  return surpriseList[randomIndex];
};

const handleSignIn = () => {
  if (!canSignIn.value) {
    Message.info("请先登录后签到");
    return;
  }
  if (signedToday.value) {
    Message.success("今天已经签到过啦");
    return;
  }
  if (!signInKey.value) {
    Message.error("签到状态异常，请刷新后重试");
    return;
  }
  localStorage.setItem(signInKey.value, new Date().toISOString());
  signedToday.value = true;
  Modal.success({
    title: "签到成功",
    content: getRandomSurprise(),
  });
};

onMounted(async () => {
  await loadEventList();
  refreshSignState();
});

watch(signInKey, () => {
  refreshSignState();
});
</script>

<style scoped>
#homeView {
  position: relative;
  max-width: 1320px;
  margin: 0 auto;
  padding: 10px 8px 24px;
}

#homeView::before {
  content: "";
  position: absolute;
  inset: 80px 0 auto 0;
  height: 320px;
  background: radial-gradient(
      600px 220px at 8% 10%,
      rgba(15, 106, 216, 0.08),
      transparent 70%
    ),
    radial-gradient(
      620px 240px at 92% 0%,
      rgba(248, 183, 0, 0.08),
      transparent 72%
    );
  pointer-events: none;
  z-index: 0;
}

#homeView > * {
  position: relative;
  z-index: 1;
}

.today-banner {
  text-align: center;
  margin: 6px 0 20px;
}

.today-big {
  font-size: 44px;
  line-height: 1.1;
  font-weight: 700;
  letter-spacing: 1px;
  color: #102a43;
  text-shadow: 0 8px 24px rgba(16, 42, 67, 0.08);
}

.today-week {
  margin-top: 8px;
  font-size: 18px;
  color: #486581;
}

.checkin-card {
  margin-bottom: 16px;
  border-radius: 16px;
  border: 1px solid #dce9fb;
  background: linear-gradient(120deg, #f8fbff 0%, #f3f9ff 100%);
  box-shadow: 0 8px 20px rgba(15, 106, 216, 0.08);
}

.checkin-card :deep(.arco-card-body) {
  padding: 16px 20px;
}

.checkin-wrapper {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.checkin-title {
  margin: 0 0 4px;
  color: #102a43;
  font-size: 28px;
}

.checkin-desc {
  margin: 0;
  color: #52667a;
  font-size: 23px;
}

.checkin-card :deep(.arco-btn-primary) {
  border-radius: 10px;
  box-shadow: 0 6px 14px rgba(15, 106, 216, 0.25);
}

.event-card {
  width: 100%;
  border-radius: 16px;
  border: 1px solid #dce9fb;
  background: linear-gradient(145deg, #ffffff 0%, #f8fbff 100%);
  box-shadow: 0 10px 22px rgba(16, 42, 67, 0.08);
  overflow: hidden;
}

.event-card :deep(.arco-card-header) {
  border-bottom: 1px solid #e9f0fb;
  padding: 14px 18px;
}

.event-card :deep(.arco-card-body) {
  padding: 12px 18px 18px;
}

.event-card :deep(.arco-card-header-title) {
  font-size: 22px;
  color: #102a43;
  font-weight: 700;
  position: relative;
  padding-left: 12px;
}

.event-card :deep(.arco-card-header-title)::before {
  content: "";
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 18px;
  border-radius: 3px;
  background: #0f6ad8;
}

.event-columns {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 22px;
}

.event-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.event-item {
  width: 100%;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  border-radius: 14px;
  border: 1px solid #e3ebf8;
  padding: 14px 18px;
  background: #ffffff;
  box-shadow: 0 4px 10px rgba(16, 42, 67, 0.04);
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.event-content {
  flex: 1;
  min-width: 0;
}

.event-item:hover {
  transform: translateY(-2px);
  border-color: #cfe1fb;
  box-shadow: 0 12px 22px rgba(15, 106, 216, 0.12);
}

.event-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.event-time {
  color: #486581;
  font-size: 13px;
  padding: 3px 8px;
  border-radius: 999px;
  background: #f2f7ff;
  border: 1px solid #deebff;
}

.event-title {
  margin: 10px 0 6px;
  font-size: 22px;
  color: #102a43;
}

.event-description {
  margin: 0;
  color: #52667a;
  line-height: 1.6;
  min-height: 20px;
  font-size: 15px;
}

.event-side {
  min-width: 220px;
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
  color: #627d98;
  font-size: 13px;
}

.event-remain {
  color: #0f6ad8;
  font-weight: 600;
}

.event-link {
  font-weight: 600;
}

@media (max-width: 768px) {
  .today-big {
    font-size: 34px;
  }

  .today-week {
    font-size: 16px;
  }

  .checkin-wrapper {
    flex-direction: column;
    align-items: flex-start;
  }

  .event-columns {
    grid-template-columns: 1fr;
  }

  .event-item {
    flex-direction: column;
    align-items: flex-start;
  }

  .event-side {
    min-width: 0;
    width: 100%;
    justify-content: flex-start;
    padding-top: 8px;
    border-top: 1px dashed #e6edf7;
  }

  .event-link {
    margin-left: 0;
  }
}
</style>
