<template>
  <div id="viewQuestionView">
    <a-row :gutter="[24, 24]">
      <a-col :md="13" :xs="24">
        <a-tabs default-active-key="question" class="question-tabs">
          <a-tab-pane key="question" title="题目">
            <a-card
              v-if="question"
              class="question-card"
              :title="question.title"
            >
              <a-descriptions
                title="判题条件"
                :column="{ xs: 1, md: 2, lg: 3 }"
              >
                <a-descriptions-item label="时间限制">
                  {{ question.judgeConfig.timeLimit ?? 0 }} ms
                </a-descriptions-item>
                <a-descriptions-item label="内存限制">
                  {{ question.judgeConfig.memoryLimit ?? 0 }} KB
                </a-descriptions-item>
                <a-descriptions-item label="栈限制">
                  {{ question.judgeConfig.stackLimit ?? 0 }} KB
                </a-descriptions-item>
              </a-descriptions>
              <div class="question-stat-list">
                <div class="question-stat-item">
                  <div class="label">提交数</div>
                  <div class="value">{{ question.submitNum ?? 0 }}</div>
                </div>
                <div class="question-stat-item">
                  <div class="label">通过数</div>
                  <div class="value">{{ question.acceptedNum ?? 0 }}</div>
                </div>
                <div class="question-stat-item">
                  <div class="label">通过率</div>
                  <div class="value">{{ acceptRate }}</div>
                </div>
              </div>
              <MdViewer :value="question.content || ''" />
              <template #extra>
                <a-space wrap>
                  <a-tag
                    v-for="(tag, index) of question.tags"
                    :key="index"
                    color="green"
                  >
                    {{ tag }}
                  </a-tag>
                </a-space>
              </template>
            </a-card>
          </a-tab-pane>
          <a-tab-pane v-if="!contestId" key="discussion" title="讨论">
            <QuestionDiscussionPanel :question-id="question?.id || props.id" />
          </a-tab-pane>
          <a-tab-pane v-if="!contestId" key="answer" title="答案">
            <a-card class="answer-card" :bordered="false">
              <CodeEditor
                v-if="question?.answer"
                :value="question.answer as string"
                :language="form.language"
                :read-only="true"
                :show-minimap="true"
                :handle-change="noopCodeChange"
              />
              <a-empty
                v-else
                description="通过该题后可查看答案（管理员始终可见）"
              />
            </a-card>
          </a-tab-pane>
        </a-tabs>
      </a-col>
      <a-col :md="11" :xs="24">
        <a-card class="code-panel-card" title="在线编程">
          <a-form :model="form" layout="inline">
            <a-form-item
              field="language"
              label="编程语言"
              style="min-width: 240px"
            >
              <a-select
                v-model="form.language"
                :style="{ width: '320px' }"
                placeholder="选择编程语言"
              >
                <a-option>java</a-option>
                <a-option>python</a-option>
                <a-option>cpp</a-option>
                <a-option>c</a-option>
              </a-select>
            </a-form-item>
          </a-form>
          <CodeEditor
            :value="form.code as string"
            :language="form.language"
            :handle-change="changeCode"
          />
          <a-divider size="0" />
          <a-space class="submit-action-group">
            <a-button
              type="primary"
              class="submit-btn"
              :loading="submitting"
              @click="doSubmit"
            >
              提交代码
            </a-button>
            <a-button v-if="contestId" type="outline" @click="backToContest">
              返回竞赛
            </a-button>
          </a-space>
        </a-card>
        <a-card v-if="latestSubmit" class="judge-result-card" title="判题状态">
          <a-descriptions :column="{ xs: 1, md: 2 }" bordered>
            <a-descriptions-item label="提交 ID">
              {{ displaySubmitId }}
            </a-descriptions-item>
            <a-descriptions-item label="状态">
              <a-tag :color="submitStatusColor">{{ submitStatusText }}</a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="判题结果">
              {{ submitResultText }}
            </a-descriptions-item>
            <a-descriptions-item label="耗时">
              {{ formatJudgeTime(latestSubmit.judgeInfo?.time) }}
            </a-descriptions-item>
            <a-descriptions-item label="内存">
              {{ formatJudgeMemory(latestSubmit.judgeInfo?.memory) }}
            </a-descriptions-item>
            <a-descriptions-item label="更新时间">
              {{
                formatTime(latestSubmit.updateTime || latestSubmit.createTime)
              }}
            </a-descriptions-item>
          </a-descriptions>
          <a-alert
            v-if="judgePolling && !isFinalStatus(latestSubmit.status)"
            style="margin-top: 12px"
            type="info"
          >
            正在判题，请稍候...
          </a-alert>
          <div class="judge-output">
            <div class="judge-output-title">程序输出</div>
            <pre class="judge-output-content">{{
              formatProgramOutput(latestSubmit.judgeInfo?.outputList)
            }}</pre>
          </div>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import {
  computed,
  onMounted,
  onBeforeUnmount,
  ref,
  watch,
  withDefaults,
  defineProps,
} from "vue";
import {
  QuestionControllerService,
  QuestionSubmitAddRequest,
  QuestionSubmitControllerService,
  QuestionSubmitQueryRequest,
  QuestionSubmitVO,
  QuestionVO,
} from "../../../generated";
import message from "@arco-design/web-vue/es/message";
import { useRoute, useRouter } from "vue-router";
import store from "@/store";
import CodeEditor from "@/components/CodeEditor.vue";
import MdViewer from "@/components/MdViewer.vue";
import QuestionDiscussionPanel from "@/views/question/QuestionDiscussionPanel.vue";

interface Props {
  id: string;
}

const props = withDefaults(defineProps<Props>(), {
  id: "",
});

const route = useRoute();
const router = useRouter();
const question = ref<QuestionVO>();
const submitting = ref(false);
const judgePolling = ref(false);
const latestSubmit = ref<QuestionSubmitVO>();
const latestSubmitId = ref<string>();
const pollingTimer = ref<number>();
const currentPollingSubmitId = ref<string>();
const POLLING_INTERVAL = 1500;
const MAX_POLLING_TIMES = 80;

const contestId = computed<string | undefined>(() => {
  const rawValue = route.query.contestId;
  const value = Array.isArray(rawValue) ? rawValue[0] : rawValue;
  if (!value) {
    return undefined;
  }
  const idStr = String(value).trim();
  return /^\d+$/.test(idStr) ? idStr : undefined;
});

const loginUserId = computed(() => Number(store.state.user.loginUser?.id || 0));

const acceptRate = computed(() => {
  const submitNum = Number(question.value?.submitNum || 0);
  const acceptedNum = Number(question.value?.acceptedNum || 0);
  if (submitNum <= 0) {
    return "0%";
  }
  return `${((acceptedNum / submitNum) * 100).toFixed(1)}%`;
});

const submitStatusTextMap: Record<number, string> = {
  0: "等待中",
  1: "判题中",
  2: "已完成",
  3: "失败",
};

const submitStatusColorMap: Record<number, string> = {
  0: "gold",
  1: "arcoblue",
  2: "green",
  3: "red",
};

const judgeMessageTextMap: Record<string, string> = {
  Accepted: "通过",
  Running: "判题中",
  Failed: "失败",
  "Wrong Answer": "答案错误",
  "Compile Error": "编译错误",
  "Memory Limit Exceeded": "超出内存限制",
  "Time Limit Exceeded": "超出时间限制",
  "Presentation Error": "格式错误",
  Waiting: "等待中",
  "Output Limit Exceeded": "输出超限",
  "Dangerous Operation": "危险操作",
  "Runtime Error": "运行错误",
  "System Error": "系统错误",
};

const submitStatusText = computed(() => {
  const status = latestSubmit.value?.status;
  if (status === undefined || status === null) {
    return "未知";
  }
  return submitStatusTextMap[status] ?? "未知";
});

const submitStatusColor = computed(() => {
  const status = latestSubmit.value?.status;
  if (status === undefined || status === null) {
    return "gray";
  }
  return submitStatusColorMap[status] ?? "gray";
});

const submitResultText = computed(() => {
  const status = latestSubmit.value?.status;
  const messageKey = latestSubmit.value?.judgeInfo?.message;
  if (messageKey) {
    return judgeMessageTextMap[messageKey] ?? messageKey;
  }
  if (status === 0 || status === 1) {
    return "等待判题";
  }
  return "--";
});

const normalizeSubmitId = (id: unknown) => {
  if (id === undefined || id === null) {
    return undefined;
  }
  const normalized = String(id).trim();
  return normalized.length > 0 ? normalized : undefined;
};

const displaySubmitId = computed(() => {
  return (
    latestSubmitId.value ?? normalizeSubmitId(latestSubmit.value?.id) ?? "--"
  );
});

const isFinalStatus = (status?: number) => status === 2 || status === 3;

const formatJudgeTime = (time?: number) => {
  if (time === undefined || time === null) {
    return "--";
  }
  return `${time} ms`;
};

const formatJudgeMemory = (memory?: number) => {
  if (memory === undefined || memory === null) {
    return "--";
  }
  return `${memory} KB`;
};

const formatProgramOutput = (outputList?: string[]) => {
  if (!outputList || outputList.length === 0) {
    return "--";
  }
  return outputList.join("\n");
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

const clearPollingTimer = () => {
  if (pollingTimer.value) {
    clearTimeout(pollingTimer.value);
    pollingTimer.value = undefined;
  }
};

const stopPolling = () => {
  judgePolling.value = false;
  clearPollingTimer();
  currentPollingSubmitId.value = undefined;
};

const querySubmitById = async (submitId: string) => {
  const query: QuestionSubmitQueryRequest = {
    current: 1,
    pageSize: 1,
    id: submitId as unknown as number,
    sortField: "id",
    sortOrder: "descend",
  };
  const res =
    await QuestionSubmitControllerService.listQuestionSubmitByPageUsingPost(
      query
    );
  if (res.code !== 0) {
    throw new Error(res.message || "获取判题结果失败");
  }
  const records = res.data?.records || [];
  return records.find((item) => normalizeSubmitId(item.id) === submitId);
};

const pollSubmitResult = async (submitId: string, count = 1) => {
  if (currentPollingSubmitId.value !== submitId) {
    return;
  }
  try {
    const submitDetail = await querySubmitById(submitId);
    if (submitDetail) {
      latestSubmit.value = submitDetail;
      latestSubmitId.value = normalizeSubmitId(submitDetail.id) ?? submitId;
      if (isFinalStatus(submitDetail.status)) {
        stopPolling();
        await loadData();
        if (submitDetail.judgeInfo?.message === "Accepted") {
          message.success(`判题完成：${submitResultText.value}`);
        } else {
          message.warning(`判题完成：${submitResultText.value}`);
        }
        return;
      }
    }
  } catch (error: any) {
    if (count === 1) {
      message.warning(error?.message || "判题结果拉取失败，正在重试");
    }
  }

  if (count >= MAX_POLLING_TIMES) {
    stopPolling();
    message.warning("判题时间较长，请稍后刷新页面查看结果");
    return;
  }

  pollingTimer.value = setTimeout(() => {
    pollSubmitResult(submitId, count + 1);
  }, POLLING_INTERVAL) as unknown as number;
};

const startPolling = (submitId: string) => {
  stopPolling();
  judgePolling.value = true;
  currentPollingSubmitId.value = submitId;
  pollSubmitResult(submitId, 1);
};

const loadData = async () => {
  const res = await QuestionControllerService.getQuestionVoByIdUsingGet(
    props.id as any,
    contestId.value
  );
  if (res.code === 0) {
    question.value = res.data;
  } else {
    message.error("加载失败: " + res.message);
  }
};

const form = ref<QuestionSubmitAddRequest>({
  language: "java",
  code: "",
});

const doSubmit = async () => {
  if (submitting.value) {
    return;
  }
  if (!question.value?.id) {
    return;
  }
  if (!loginUserId.value) {
    await store.dispatch("user/getLoginUser");
  }
  if (!loginUserId.value) {
    message.error("请先登录");
    return;
  }

  submitting.value = true;
  const payload: any = {
    ...form.value,
    questionId: question.value.id,
  };
  if (contestId.value) {
    payload.contestId = contestId.value;
  }

  try {
    const res = await QuestionSubmitControllerService.doQuestionSubmitUsingPost(
      payload
    );
    if (res.code === 0 && res.data) {
      const submitId = normalizeSubmitId(res.data);
      if (!submitId) {
        message.error("提交失败: 未获取到有效提交 ID");
        return;
      }
      latestSubmitId.value = submitId;
      latestSubmit.value = {
        status: 0,
        judgeInfo: {
          message: "Waiting",
        },
        language: form.value.language,
      };
      message.success("提交成功，正在判题");
      startPolling(submitId);
    } else {
      message.error("提交失败: " + res.message);
    }
  } catch (error: any) {
    message.error(error?.message || "提交失败，请稍后重试");
  } finally {
    submitting.value = false;
  }
};

const backToContest = () => {
  if (!contestId.value) {
    return;
  }
  router.push(`/contest/${contestId.value}`);
};

onMounted(() => {
  store.dispatch("user/getLoginUser");
  loadData();
});

onBeforeUnmount(() => {
  stopPolling();
});

const changeCode = (value: string) => {
  form.value.code = value;
};

const noopCodeChange = () => undefined;

watch([() => props.id, () => contestId.value], () => {
  stopPolling();
  latestSubmit.value = undefined;
  latestSubmitId.value = undefined;
  loadData();
});
</script>

<style>
#viewQuestionView {
  position: relative;
  max-width: 1400px;
  margin: 0 auto;
  padding: 6px 0 18px;
}

#viewQuestionView::before {
  content: "";
  position: absolute;
  inset: 0;
  height: 320px;
  z-index: -1;
  pointer-events: none;
  background: radial-gradient(
      circle at 8% 0%,
      rgba(84, 170, 255, 0.16) 0%,
      rgba(84, 170, 255, 0) 54%
    ),
    radial-gradient(
      circle at 92% 14%,
      rgba(84, 242, 205, 0.18) 0%,
      rgba(84, 242, 205, 0) 56%
    );
}

#viewQuestionView .question-tabs {
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid #e8eef7;
  border-radius: 16px;
  box-shadow: 0 10px 30px rgba(50, 84, 122, 0.08);
  padding: 6px 10px 10px;
}

#viewQuestionView .question-card,
#viewQuestionView .answer-card,
#viewQuestionView .code-panel-card,
#viewQuestionView .judge-result-card {
  border-radius: 14px;
  border: 1px solid #e8edf6;
  box-shadow: 0 8px 24px rgba(34, 62, 92, 0.08);
}

#viewQuestionView .question-card .arco-card-header-title,
#viewQuestionView .code-panel-card .arco-card-header-title,
#viewQuestionView .judge-result-card .arco-card-header-title {
  font-weight: 700;
}

#viewQuestionView .question-stat-list {
  margin: 10px 0 16px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

#viewQuestionView .question-stat-item {
  border-radius: 10px;
  padding: 10px 12px;
  background: linear-gradient(135deg, #f7fbff 0%, #eef6ff 100%);
  border: 1px solid #e0ecff;
}

#viewQuestionView .question-stat-item .label {
  font-size: 12px;
  color: #66738a;
}

#viewQuestionView .question-stat-item .value {
  margin-top: 4px;
  font-size: 18px;
  line-height: 1;
  color: #204a81;
  font-weight: 700;
}

#viewQuestionView .arco-space-horizontal .arco-space-item {
  margin-bottom: 0 !important;
}

#viewQuestionView .code-panel-card {
  margin-bottom: 16px;
}

#viewQuestionView .submit-action-group {
  width: 100%;
}

#viewQuestionView .submit-btn {
  min-width: 200px;
}

#viewQuestionView .judge-result-card {
  margin-top: 0;
}

#viewQuestionView .judge-output {
  margin-top: 12px;
}

#viewQuestionView .judge-output-title {
  font-weight: 600;
  margin-bottom: 6px;
}

#viewQuestionView .judge-output-content {
  margin: 0;
  max-height: 220px;
  overflow: auto;
  border: 1px solid #dfe8f5;
  border-radius: 10px;
  padding: 10px;
  background: #f6f9ff;
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 992px) {
  #viewQuestionView .question-tabs {
    padding: 4px 6px 8px;
  }

  #viewQuestionView .question-stat-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  #viewQuestionView .submit-btn {
    min-width: 160px;
  }
}

@media (max-width: 576px) {
  #viewQuestionView {
    padding: 0 8px 14px;
  }

  #viewQuestionView .question-stat-list {
    grid-template-columns: repeat(1, minmax(0, 1fr));
  }
}
</style>
