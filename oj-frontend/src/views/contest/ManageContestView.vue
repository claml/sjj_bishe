<template>
  <div id="manageContestView">
    <section class="hero">
      <div>
        <h2 class="hero-title">管理竞赛</h2>
        <p class="hero-subtitle">创建竞赛、维护赛程，并统一管理题目配置</p>
      </div>
      <div class="hero-metrics">
        <div class="metric-item">
          <span class="metric-label">竞赛总数</span>
          <span class="metric-value">{{ total }}</span>
        </div>
        <div class="metric-item">
          <span class="metric-label">进行中</span>
          <span class="metric-value">{{ runningCount }}</span>
        </div>
        <div class="metric-item">
          <span class="metric-label">未开始</span>
          <span class="metric-value">{{ notStartedCount }}</span>
        </div>
      </div>
    </section>

    <a-card class="create-card" :bordered="false">
      <template #title>创建竞赛</template>
      <a-form :model="createForm" layout="vertical">
        <a-row :gutter="[16, 0]">
          <a-col :md="8" :xs="24">
            <a-form-item field="title" label="竞赛名称">
              <a-input
                v-model="createForm.title"
                placeholder="请输入竞赛名称"
              />
            </a-form-item>
          </a-col>
          <a-col :md="8" :xs="24">
            <a-form-item field="description" label="竞赛描述">
              <a-input
                v-model="createForm.description"
                placeholder="请输入竞赛描述"
              />
            </a-form-item>
          </a-col>
          <a-col :md="8" :xs="24">
            <a-form-item field="inviteCode" label="参赛码">
              <a-input
                v-model="createForm.inviteCode"
                placeholder="请输入参赛码"
                allow-clear
              />
            </a-form-item>
          </a-col>
        </a-row>

        <a-row :gutter="[16, 0]">
          <a-col :md="12" :xs="24">
            <a-form-item field="startTime" label="开始时间">
              <a-date-picker
                v-model="createForm.startTime"
                show-time
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
          <a-col :md="12" :xs="24">
            <a-form-item field="endTime" label="结束时间">
              <a-date-picker
                v-model="createForm.endTime"
                show-time
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
        </a-row>

        <a-form-item field="questionIdList" label="从题库选择题目">
          <a-select
            v-model="createForm.questionIdList"
            multiple
            allow-search
            allow-clear
            placeholder="可多选题库题目"
          >
            <a-option
              v-for="item in questionBankList"
              :key="item.id"
              :value="item.id"
            >
              {{ `${item.id}. ${item.title}` }}
            </a-option>
          </a-select>
        </a-form-item>

        <a-card title="创建新题目并加入竞赛" size="small">
          <template #extra>
            <a-button type="outline" @click="addNewQuestion">新增题目</a-button>
          </template>
          <a-empty
            v-if="createForm.newQuestionList.length === 0"
            description="暂无新题目"
          />

          <a-collapse v-else>
            <a-collapse-item
              v-for="(item, index) in createForm.newQuestionList"
              :key="index"
              :header="`新题目 ${index + 1}`"
            >
              <a-space direction="vertical" fill>
                <a-input v-model="item.title" placeholder="题目标题" />
                <a-textarea
                  v-model="item.content"
                  placeholder="题目内容（支持 Markdown）"
                />
                <a-textarea v-model="item.answer" placeholder="题目答案说明" />
                <a-input-tag v-model="item.tags" placeholder="题目标签" />
                <a-row :gutter="[8, 0]">
                  <a-col :span="8">
                    <a-input-number
                      v-model="item.judgeConfig.timeLimit"
                      placeholder="时间限制"
                      mode="button"
                      :min="1"
                      style="width: 100%"
                    />
                  </a-col>
                  <a-col :span="8">
                    <a-input-number
                      v-model="item.judgeConfig.memoryLimit"
                      placeholder="内存限制"
                      mode="button"
                      :min="1"
                      style="width: 100%"
                    />
                  </a-col>
                  <a-col :span="8">
                    <a-input-number
                      v-model="item.judgeConfig.stackLimit"
                      placeholder="栈限制"
                      mode="button"
                      :min="1"
                      style="width: 100%"
                    />
                  </a-col>
                </a-row>
                <a-divider style="margin: 8px 0" />
                <a-space direction="vertical" fill>
                  <div
                    v-for="(judgeCase, caseIndex) in item.judgeCase"
                    :key="caseIndex"
                    class="judge-case"
                  >
                    <a-input
                      v-model="judgeCase.input"
                      :placeholder="`输入用例 ${caseIndex + 1}`"
                    />
                    <a-input
                      v-model="judgeCase.output"
                      :placeholder="`输出用例 ${caseIndex + 1}`"
                    />
                    <a-button
                      status="danger"
                      type="text"
                      @click="removeJudgeCase(index, caseIndex)"
                    >
                      删除用例
                    </a-button>
                  </div>
                  <a-button type="outline" @click="addJudgeCase(index)">
                    新增判题用例
                  </a-button>
                </a-space>
                <a-button status="danger" @click="removeNewQuestion(index)">
                  删除该新题目
                </a-button>
              </a-space>
            </a-collapse-item>
          </a-collapse>
        </a-card>

        <a-space style="margin-top: 16px">
          <a-button type="primary" @click="submitCreateContest"
            >创建竞赛</a-button
          >
          <a-button @click="resetCreateForm">重置</a-button>
        </a-space>
      </a-form>
    </a-card>

    <a-card class="table-card" :bordered="false">
      <template #title>竞赛管理</template>
      <a-table
        :columns="columns"
        :data="contestList"
        :loading="listLoading"
        :pagination="{
          showTotal: true,
          showPageSize: true,
          pageSizeOptions: [10, 20, 50],
          pageSize: listQuery.pageSize,
          current: listQuery.current,
          total,
        }"
        @page-change="onPageChange"
        @page-size-change="onPageSizeChange"
      >
        <template #status="{ record }">
          <a-tag :color="statusColorMap[record.status] || 'gray'">
            {{ statusTextMap[record.status] || "未知" }}
          </a-tag>
        </template>
        <template #timeRange="{ record }">
          {{ formatDateTime(record.startTime) }} ~
          {{ formatDateTime(record.endTime) }}
        </template>
        <template #optional="{ record }">
          <a-space>
            <a-button
              type="outline"
              size="small"
              @click="openEditModal(record)"
            >
              编辑
            </a-button>
            <a-popconfirm content="确认删除该竞赛？" @ok="doDelete(record.id)">
              <a-button status="danger" size="small">删除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:visible="editVisible"
      title="编辑竞赛"
      @ok="submitUpdateContest"
      @cancel="closeEditModal"
    >
      <a-form :model="editForm" layout="vertical">
        <a-form-item field="title" label="竞赛名称">
          <a-input v-model="editForm.title" />
        </a-form-item>
        <a-form-item field="description" label="竞赛描述">
          <a-input v-model="editForm.description" />
        </a-form-item>
        <a-form-item field="startTime" label="开始时间">
          <a-date-picker
            v-model="editForm.startTime"
            show-time
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item field="endTime" label="结束时间">
          <a-date-picker
            v-model="editForm.endTime"
            show-time
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import message from "@arco-design/web-vue/es/message";
import moment from "moment";
import {
  addContest,
  ContestAddPayload,
  ContestUpdatePayload,
  ContestVO,
  deleteContest,
  getErrorMessage,
  listContestVOByPage,
  listQuestionBank,
  QuestionBankItem,
  updateContest,
} from "@/services/contestApi";

interface NewQuestionFormItem {
  title: string;
  content: string;
  answer: string;
  tags: string[];
  judgeConfig: {
    memoryLimit: number;
    stackLimit: number;
    timeLimit: number;
  };
  judgeCase: Array<{
    input: string;
    output: string;
  }>;
}

interface CreateContestForm {
  title: string;
  description: string;
  startTime: string;
  endTime: string;
  inviteCode: string;
  questionIdList: number[];
  newQuestionList: NewQuestionFormItem[];
}

interface ContestListQuery {
  current: number;
  pageSize: number;
}

const makeNewQuestion = (): NewQuestionFormItem => ({
  title: "",
  content: "",
  answer: "",
  tags: [],
  judgeConfig: {
    memoryLimit: 128,
    stackLimit: 128,
    timeLimit: 1000,
  },
  judgeCase: [
    {
      input: "",
      output: "",
    },
  ],
});

const createForm = ref<CreateContestForm>({
  title: "",
  description: "",
  startTime: "",
  endTime: "",
  inviteCode: "",
  questionIdList: [],
  newQuestionList: [],
});

const questionBankList = ref<QuestionBankItem[]>([]);
const contestList = ref<ContestVO[]>([]);
const total = ref(0);
const listLoading = ref(false);
const listQuery = ref<ContestListQuery>({
  current: 1,
  pageSize: 10,
});

const editVisible = ref(false);
const editForm = ref<ContestUpdatePayload>({
  id: 0,
  title: "",
  description: "",
  startTime: "",
  endTime: "",
});

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

const columns = [
  {
    title: "竞赛名称",
    dataIndex: "title",
  },
  {
    title: "状态",
    slotName: "status",
  },
  {
    title: "有效时间",
    slotName: "timeRange",
  },
  {
    title: "操作",
    slotName: "optional",
  },
];

const formatDateTime = (value?: string) => {
  if (!value) {
    return "-";
  }
  return moment(value).format("YYYY-MM-DD HH:mm:ss");
};

const loadQuestionBank = async () => {
  try {
    const pageData = await listQuestionBank();
    questionBankList.value = pageData.records ?? [];
  } catch (error) {
    message.error(getErrorMessage(error, "加载题库失败"));
  }
};

const loadContestList = async () => {
  listLoading.value = true;
  try {
    const pageData = await listContestVOByPage(listQuery.value);
    contestList.value = pageData.records ?? [];
    total.value = Number(pageData.total ?? 0);
  } catch (error) {
    message.error(getErrorMessage(error, "加载竞赛列表失败"));
  } finally {
    listLoading.value = false;
  }
};

const runningCount = computed(
  () => contestList.value.filter((item) => item.status === "running").length
);

const notStartedCount = computed(
  () => contestList.value.filter((item) => item.status === "not_started").length
);

const addNewQuestion = () => {
  createForm.value.newQuestionList.push(makeNewQuestion());
};

const removeNewQuestion = (index: number) => {
  createForm.value.newQuestionList.splice(index, 1);
};

const addJudgeCase = (questionIndex: number) => {
  createForm.value.newQuestionList[questionIndex].judgeCase.push({
    input: "",
    output: "",
  });
};

const removeJudgeCase = (questionIndex: number, caseIndex: number) => {
  const currentCases =
    createForm.value.newQuestionList[questionIndex].judgeCase;
  if (currentCases.length === 1) {
    return;
  }
  currentCases.splice(caseIndex, 1);
};

const validateCreateForm = () => {
  if (!createForm.value.title.trim()) {
    message.warning("请填写竞赛名称");
    return false;
  }
  if (!createForm.value.startTime || !createForm.value.endTime) {
    message.warning("请填写竞赛时间");
    return false;
  }
  if (!createForm.value.inviteCode.trim()) {
    message.warning("请填写参赛码");
    return false;
  }
  if (
    createForm.value.questionIdList.length === 0 &&
    createForm.value.newQuestionList.length === 0
  ) {
    message.warning("请至少添加一道题目");
    return false;
  }
  return true;
};

const submitCreateContest = async () => {
  if (!validateCreateForm()) {
    return;
  }
  const payload: ContestAddPayload = {
    title: createForm.value.title.trim(),
    description: createForm.value.description,
    startTime: createForm.value.startTime,
    endTime: createForm.value.endTime,
    inviteCode: createForm.value.inviteCode.trim(),
    questionIdList: createForm.value.questionIdList,
    newQuestionList: createForm.value.newQuestionList,
  };
  try {
    await addContest(payload);
    message.success("创建竞赛成功");
    resetCreateForm();
    await loadContestList();
  } catch (error) {
    message.error(getErrorMessage(error, "创建竞赛失败"));
  }
};

const resetCreateForm = () => {
  createForm.value = {
    title: "",
    description: "",
    startTime: "",
    endTime: "",
    inviteCode: "",
    questionIdList: [],
    newQuestionList: [],
  };
};

const openEditModal = (contest: ContestVO) => {
  editForm.value = {
    id: contest.id,
    title: contest.title,
    description: contest.description,
    startTime: contest.startTime,
    endTime: contest.endTime,
  };
  editVisible.value = true;
};

const closeEditModal = () => {
  editVisible.value = false;
  editForm.value = {
    id: 0,
    title: "",
    description: "",
    startTime: "",
    endTime: "",
  };
};

const submitUpdateContest = async () => {
  if (!editForm.value.id) {
    return;
  }
  try {
    await updateContest(editForm.value);
    message.success("更新成功");
    closeEditModal();
    await loadContestList();
  } catch (error) {
    message.error(getErrorMessage(error, "更新失败"));
  }
};

const doDelete = async (id: number) => {
  try {
    await deleteContest(id);
    message.success("删除成功");
    await loadContestList();
  } catch (error) {
    message.error(getErrorMessage(error, "删除失败"));
  }
};

const onPageChange = (page: number) => {
  listQuery.value = {
    ...listQuery.value,
    current: page,
  };
};

const onPageSizeChange = (pageSize: number) => {
  listQuery.value = {
    ...listQuery.value,
    pageSize,
    current: 1,
  };
};

watch(
  listQuery,
  () => {
    loadContestList();
  },
  { deep: true, immediate: true }
);

onMounted(() => {
  loadQuestionBank();
});
</script>

<style scoped>
#manageContestView {
  --page-accent: #0f6ad8;
  max-width: 1360px;
  margin: 0 auto;
  padding: 6px 0 20px;
}

.hero {
  margin-bottom: 14px;
  border-radius: 16px;
  padding: 18px 22px;
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  background: linear-gradient(120deg, #f7fbff 0%, #edf4ff 60%, #e7f0ff 100%);
  border: 1px solid #d8e9ff;
}

.hero-title {
  margin: 0 0 6px;
  font-size: 23px;
  color: #102a43;
}

.hero-subtitle {
  margin: 0;
  color: #486581;
  font-size: 14px;
}

.hero-metrics {
  display: flex;
  gap: 10px;
}

.metric-item {
  min-width: 94px;
  padding: 10px 12px;
  border-radius: 12px;
  background: #ffffffb8;
  border: 1px solid #dbe7f7;
  display: flex;
  flex-direction: column;
  gap: 4px;
  text-align: right;
}

.metric-label {
  font-size: 12px;
  color: #61758a;
}

.metric-value {
  font-size: 20px;
  line-height: 1;
  color: var(--page-accent);
  font-weight: 700;
}

.create-card {
  margin-bottom: 14px;
}

.table-card :deep(.arco-card-header),
.create-card :deep(.arco-card-header) {
  border-bottom: none;
}

.table-card :deep(.arco-card-body),
.create-card :deep(.arco-card-body) {
  padding: 16px 18px;
}

.table-card :deep(.arco-table-th) {
  font-weight: 700;
  color: #243b53;
  background: #f7faff;
}

.judge-case {
  display: grid;
  grid-template-columns: 1fr 1fr auto;
  gap: 8px;
}

@media (max-width: 768px) {
  .hero {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-metrics {
    width: 100%;
  }

  .metric-item {
    flex: 1;
    text-align: left;
  }

  .judge-case {
    grid-template-columns: 1fr;
  }
}
</style>
