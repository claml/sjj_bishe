<template>
  <div id="contestDetailView">
    <a-card :title="contest?.title || '竞赛详情'" :loading="loading">
      <template #extra>
        <a-space>
          <a-tag :color="statusColor">{{ statusText }}</a-tag>
          <a-button
            v-if="contest && !contest.joined && contest.status !== 'ended'"
            type="primary"
            @click="doJoin"
          >
            参加竞赛
          </a-button>
        </a-space>
      </template>

      <a-typography-paragraph style="margin-bottom: 8px">
        {{ contest?.description || "暂无描述" }}
      </a-typography-paragraph>
      <a-typography-text type="secondary">
        竞赛时间：{{ formatDateTime(contest?.startTime) }} ~
        {{ formatDateTime(contest?.endTime) }}
      </a-typography-text>
    </a-card>

    <a-row :gutter="[16, 16]" style="margin-top: 16px">
      <a-col :md="14" :xs="24">
        <a-card title="竞赛题目">
          <a-table
            :columns="questionColumns"
            :data="contest?.questionList || []"
          >
            <template #solved="{ record }">
              <a-tag v-if="isSolved(record.id)" color="green">已通过</a-tag>
              <a-tag v-else color="gray">未通过</a-tag>
            </template>
            <template #acceptedRate="{ record }">
              {{ getAcceptedRate(record.acceptedNum, record.submitNum) }}
            </template>
            <template #optional="{ record }">
              <a-button
                type="primary"
                size="small"
                @click="toQuestion(record.id)"
              >
                去做题
              </a-button>
            </template>
          </a-table>
        </a-card>
      </a-col>

      <a-col :md="10" :xs="24">
        <a-card title="实时排行榜">
          <a-table :columns="rankColumns" :data="rankList" :pagination="false">
            <template #user="{ record }">
              <a-space>
                <a-avatar
                  :size="24"
                  :image-url="record.userAvatar"
                  class="clickable-user"
                  @click="goUserProfile(record.userId, $event)"
                >
                  {{ record.userName?.slice(0, 1) || "U" }}
                </a-avatar>
                <span
                  class="clickable-user"
                  @click="goUserProfile(record.userId, $event)"
                >
                  {{ record.userName || `用户${record.userId}` }}
                </span>
              </a-space>
            </template>
            <template #lastAcceptedTime="{ record }">
              {{ formatDateTime(record.lastAcceptedTime) }}
            </template>
          </a-table>
        </a-card>
      </a-col>
    </a-row>

    <a-modal
      v-model:visible="inviteModalVisible"
      title="请输入参赛码"
      :mask-closable="false"
      @ok="submitInviteCode"
      @cancel="handleInviteCancel"
    >
      <a-space direction="vertical" fill>
        <a-typography-text>
          该竞赛需要输入正确的参赛码后才能查看详情并参加。
        </a-typography-text>
        <a-input
          v-model="inviteCodeInput"
          placeholder="输入参赛码"
          allow-clear
          @press-enter="submitInviteCode"
        />
      </a-space>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import {
  computed,
  onBeforeUnmount,
  onMounted,
  ref,
  withDefaults,
  defineProps,
} from "vue";
import { useRouter } from "vue-router";
import message from "@arco-design/web-vue/es/message";
import moment from "moment";
import {
  ContestRankVO,
  ContestVO,
  getContestDetailWithInviteCode,
  getContestRank,
  getErrorMessage,
  joinContest,
} from "@/services/contestApi";
import { openPage, shouldOpenInNewTab } from "@/utils/navigation";

interface Props {
  id: string;
}

const props = withDefaults(defineProps<Props>(), {
  id: "",
});

const router = useRouter();
const loading = ref(false);
const contest = ref<ContestVO>();
const rankList = ref<ContestRankVO[]>([]);

const inviteCode = ref("");
const inviteCodeInput = ref("");
const inviteModalVisible = ref(false);
const pendingJoin = ref(false);

let rankTimer: number | null = null;

const statusTextMap: Record<string, string> = {
  not_started: "未开始",
  running: "进行中",
  ended: "已结束",
};
const statusColorMap: Record<string, string> = {
  not_started: "orange",
  running: "green",
  ended: "gray",
};

const statusText = computed(() => {
  if (!contest.value?.status) {
    return "未知";
  }
  return statusTextMap[contest.value.status] || "未知";
});

const statusColor = computed(() => {
  if (!contest.value?.status) {
    return "gray";
  }
  return statusColorMap[contest.value.status] || "gray";
});

const questionColumns = [
  { title: "题目", dataIndex: "title" },
  { title: "通过状态", slotName: "solved" },
  { title: "通过率", slotName: "acceptedRate" },
  { title: "操作", slotName: "optional" },
];

const rankColumns = [
  { title: "排名", dataIndex: "rank", width: 72 },
  { title: "用户", slotName: "user" },
  { title: "通过数", dataIndex: "solvedCount", width: 84 },
  { title: "最近通过", slotName: "lastAcceptedTime" },
];

const formatDateTime = (value?: string) => {
  if (!value) {
    return "-";
  }
  return moment(value).format("YYYY-MM-DD HH:mm:ss");
};

const getAcceptedRate = (acceptedNum?: number, submitNum?: number) => {
  const accepted = Number(acceptedNum ?? 0);
  const submit = Number(submitNum ?? 0);
  if (!submit) {
    return `0% (${accepted}/${submit})`;
  }
  return `${((accepted / submit) * 100).toFixed(1)}% (${accepted}/${submit})`;
};

const isSolved = (questionId: number) => {
  return !!contest.value?.solvedQuestionIdList?.includes(questionId);
};

const isInviteCodeError = (error: any) => {
  const errorMessage = getErrorMessage(error, "").toLowerCase();
  return (
    errorMessage.includes("invite code") ||
    errorMessage.includes("邀请码") ||
    errorMessage.includes("参赛码")
  );
};

const loadContestDetail = async () => {
  loading.value = true;
  try {
    contest.value = await getContestDetailWithInviteCode(
      props.id,
      inviteCode.value || undefined
    );
    return true;
  } catch (error) {
    if (isInviteCodeError(error)) {
      inviteModalVisible.value = true;
      message.warning("请输入正确的参赛码");
      return false;
    }
    message.error(getErrorMessage(error, "加载竞赛详情失败"));
    return false;
  } finally {
    loading.value = false;
  }
};

const loadRank = async () => {
  if (!contest.value?.id) {
    return;
  }
  try {
    rankList.value = await getContestRank(props.id);
  } catch (error) {
    message.error(getErrorMessage(error, "加载排行榜失败"));
  }
};

const doJoinInternal = async () => {
  if (!contest.value?.id) {
    return;
  }
  await joinContest(contest.value.id, inviteCode.value || undefined);
  message.success("参加成功");
  await loadContestDetail();
  await loadRank();
};

const doJoin = async () => {
  try {
    await doJoinInternal();
  } catch (error) {
    if (isInviteCodeError(error)) {
      pendingJoin.value = true;
      inviteModalVisible.value = true;
      message.warning("参加前请先输入正确的参赛码");
      return;
    }
    message.error(getErrorMessage(error, "参加失败"));
  }
};

const submitInviteCode = async () => {
  const code = inviteCodeInput.value.trim();
  if (!code) {
    message.warning("请输入参赛码");
    return;
  }
  inviteCode.value = code;

  const loaded = await loadContestDetail();
  if (!loaded) {
    return;
  }

  inviteModalVisible.value = false;
  inviteCodeInput.value = "";
  await loadRank();
  startRankTimer();

  if (pendingJoin.value) {
    pendingJoin.value = false;
    await doJoin();
  }
};

const handleInviteCancel = () => {
  pendingJoin.value = false;
  if (!contest.value) {
    router.push("/contests");
  }
};

const toQuestion = (questionId: number) => {
  router.push({
    path: `/view/question/${questionId}`,
    query: { contestId: props.id },
  });
};

const goUserProfile = (userId?: string | number, event?: MouseEvent) => {
  if (!userId) {
    return;
  }
  return openPage(router, `/user/${userId}`, {
    newTab: shouldOpenInNewTab(event),
  });
};

const startRankTimer = () => {
  stopRankTimer();
  rankTimer = window.setInterval(() => {
    loadRank();
  }, 5000);
};

const stopRankTimer = () => {
  if (rankTimer) {
    window.clearInterval(rankTimer);
    rankTimer = null;
  }
};

onMounted(async () => {
  const loaded = await loadContestDetail();
  if (!loaded) {
    return;
  }
  await loadRank();
  startRankTimer();
});

onBeforeUnmount(() => {
  stopRankTimer();
});
</script>

<style scoped>
#contestDetailView {
  max-width: 1360px;
  margin: 0 auto;
}

.clickable-user {
  cursor: pointer;
}
</style>
