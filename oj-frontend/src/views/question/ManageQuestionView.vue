<template>
  <div id="manageQuestionView">
    <section class="hero">
      <div>
        <h2 class="hero-title">管理题目</h2>
        <p class="hero-subtitle">筛选、维护与快速管理整套题库</p>
      </div>
      <div class="hero-metrics">
        <div class="metric-item">
          <span class="metric-label">题目总数</span>
          <span class="metric-value">{{ total }}</span>
        </div>
        <div class="metric-item">
          <span class="metric-label">当前页</span>
          <span class="metric-value">{{ searchParams.current }}</span>
        </div>
      </div>
    </section>

    <a-card class="filter-card" :bordered="false">
      <a-form :model="filterForm" layout="inline">
        <a-form-item field="id" label="题号">
          <a-input-number
            v-model="filterForm.id"
            :min="1"
            hide-button
            placeholder="按 ID 筛选"
            style="width: 150px"
          />
        </a-form-item>
        <a-form-item field="title" label="标题">
          <a-input
            v-model="filterForm.title"
            placeholder="输入题目标题关键词"
            style="width: 240px"
            allow-clear
          />
        </a-form-item>
        <a-form-item field="tagsText" label="标签">
          <a-input
            v-model="filterForm.tagsText"
            placeholder="多个标签用空格或逗号分隔"
            style="width: 260px"
            allow-clear
          />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="applyFilters">查询</a-button>
            <a-button @click="resetFilters">重置</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <a-card class="table-card" :bordered="false">
      <template #title>题目列表</template>
      <template #extra>
        <a-space>
          <a-button @click="refreshData">刷新</a-button>
          <a-button type="primary" @click="goCreateQuestion">新建题目</a-button>
        </a-space>
      </template>
      <a-table
        :columns="columns"
        :data="dataList"
        :loading="loading"
        row-key="id"
        :pagination="{
          showTotal: true,
          showPageSize: true,
          pageSizeOptions: [10, 20, 50],
          pageSize: searchParams.pageSize,
          current: searchParams.current,
          total,
        }"
        @page-change="onPageChange"
        @page-size-change="onPageSizeChange"
      >
        <template #title="{ record }">
          <div class="title-cell">
            <span class="title-text">{{ record.title || "未命名题目" }}</span>
          </div>
        </template>
        <template #tags="{ record }">
          <a-space wrap>
            <a-tag
              v-for="tag in getTagList(record.tags)"
              :key="`${record.id}-${tag}`"
              color="arcoblue"
              bordered
            >
              {{ tag }}
            </a-tag>
            <a-tag v-if="getTagList(record.tags).length === 0" color="gray">
              暂无标签
            </a-tag>
          </a-space>
        </template>
        <template #summary="{ record }">
          <div class="summary-cell">
            <div class="summary-item">
              <span class="summary-label">题面</span>
              <a-typography-paragraph
                :ellipsis="{ rows: 1, showTooltip: true }"
                class="summary-text"
              >
                {{ record.content || "-" }}
              </a-typography-paragraph>
            </div>
            <div class="summary-item">
              <span class="summary-label">答案</span>
              <a-typography-paragraph
                :ellipsis="{ rows: 1, showTooltip: true }"
                class="summary-text"
              >
                {{ record.answer || "-" }}
              </a-typography-paragraph>
            </div>
          </div>
        </template>
        <template #stats="{ record }">
          <div class="stats-cell">
            <a-tag color="green">
              {{ Number(record.acceptedNum ?? 0) }} 通过
            </a-tag>
            <a-tag color="blue">
              {{ Number(record.submitNum ?? 0) }} 提交
            </a-tag>
            <span class="rate-text">
              通过率 {{ getAcceptedRate(record.acceptedNum, record.submitNum) }}
            </span>
          </div>
        </template>
        <template #meta="{ record }">
          <div class="meta-cell">
            <span>创建人：{{ record.userId ?? "-" }}</span>
            <span>{{ formatDateTime(record.createTime) }}</span>
          </div>
        </template>
        <template #optional="{ record }">
          <a-space>
            <a-button type="primary" size="small" @click="doUpdate(record)">
              修改
            </a-button>
            <a-popconfirm content="确定删除该题目吗？" @ok="doDelete(record)">
              <a-button status="danger" size="small">删除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from "vue";
import {
  Question,
  QuestionQueryRequest,
  QuestionControllerService,
} from "../../../generated";
import message from "@arco-design/web-vue/es/message";
import { useRouter } from "vue-router";
import moment from "moment";

interface FilterForm {
  id?: number;
  title: string;
  tagsText: string;
}

const dataList = ref<Question[]>([]);
const total = ref(0);
const loading = ref(false);
const searchParams = ref<QuestionQueryRequest>({
  pageSize: 10,
  current: 1,
  sortField: "createTime",
  sortOrder: "descend",
});
const filterForm = ref<FilterForm>({
  id: undefined,
  title: "",
  tagsText: "",
});

const loadData = async () => {
  loading.value = true;
  try {
    const res = await QuestionControllerService.listQuestionByPageUsingPost(
      searchParams.value
    );
    if (res.code === 0 && res.data) {
      dataList.value = res.data.records ?? [];
      total.value = Number(res.data.total ?? 0);
    } else {
      message.error("加载失败，" + res.message);
    }
  } finally {
    loading.value = false;
  }
};

const columns = [
  {
    title: "ID",
    dataIndex: "id",
    width: 90,
    align: "center",
  },
  {
    title: "标题",
    slotName: "title",
    minWidth: 240,
  },
  {
    title: "标签",
    slotName: "tags",
    minWidth: 220,
  },
  {
    title: "题目摘要",
    slotName: "summary",
    minWidth: 300,
  },
  {
    title: "通过情况",
    slotName: "stats",
    width: 190,
  },
  {
    title: "创建信息",
    slotName: "meta",
    width: 220,
  },
  {
    title: "操作",
    slotName: "optional",
    width: 160,
    fixed: "right",
  },
];

const parseTagsText = (value: string) => {
  return value
    .split(/[\s,，]+/)
    .map((item) => item.trim())
    .filter(Boolean);
};

const applyFilters = () => {
  const tags = parseTagsText(filterForm.value.tagsText);
  searchParams.value = {
    ...searchParams.value,
    id: filterForm.value.id,
    title: filterForm.value.title.trim() || undefined,
    tags: tags.length ? tags : undefined,
    current: 1,
  };
};

const resetFilters = () => {
  filterForm.value = {
    id: undefined,
    title: "",
    tagsText: "",
  };
  searchParams.value = {
    ...searchParams.value,
    id: undefined,
    title: undefined,
    tags: undefined,
    current: 1,
  };
};

const refreshData = () => {
  loadData();
};

const onPageChange = (page: number) => {
  searchParams.value = {
    ...searchParams.value,
    current: page,
  };
};

const onPageSizeChange = (pageSize: number) => {
  searchParams.value = {
    ...searchParams.value,
    pageSize,
    current: 1,
  };
};

const doDelete = async (question: Question) => {
  if (!question.id) {
    return;
  }
  const res = await QuestionControllerService.deleteQuestionUsingPost({
    id: question.id,
  });
  if (res.code === 0) {
    message.success("删除成功");
    loadData();
  } else {
    message.error("删除失败");
  }
};

const router = useRouter();

const goCreateQuestion = () => {
  router.push({
    path: "/add/question",
  });
};

const doUpdate = (question: Question) => {
  if (!question.id) {
    return;
  }
  router.push({
    path: "/update/question",
    query: {
      id: question.id,
    },
  });
};

const getTagList = (tags: unknown): string[] => {
  if (Array.isArray(tags)) {
    return tags.map((item) => String(item)).filter(Boolean);
  }
  if (typeof tags !== "string") {
    return [];
  }
  const raw = tags.trim();
  if (!raw) {
    return [];
  }
  try {
    const parsed = JSON.parse(raw);
    if (Array.isArray(parsed)) {
      return parsed.map((item) => String(item)).filter(Boolean);
    }
  } catch (error) {
    // Fallback to plain text split when tags is not JSON.
  }
  return parseTagsText(raw);
};

const getAcceptedRate = (acceptedNum?: number, submitNum?: number) => {
  const accepted = Number(acceptedNum ?? 0);
  const submit = Number(submitNum ?? 0);
  if (!submit) {
    return "0%";
  }
  return `${((accepted / submit) * 100).toFixed(1)}%`;
};

const formatDateTime = (value?: string) => {
  if (!value) {
    return "-";
  }
  return moment(value).format("YYYY-MM-DD HH:mm");
};

watch(
  searchParams,
  () => {
    loadData();
  },
  { deep: true, immediate: true }
);
</script>

<style scoped>
#manageQuestionView {
  --page-accent: #0f6ad8;
  --page-accent-light: #e9f3ff;
  max-width: 1400px;
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
  min-width: 104px;
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

.filter-card {
  margin-bottom: 14px;
}

.table-card :deep(.arco-card-header),
.filter-card :deep(.arco-card-header) {
  border-bottom: none;
}

.table-card :deep(.arco-card-body),
.filter-card :deep(.arco-card-body) {
  padding: 16px 18px;
}

.title-cell {
  display: flex;
  align-items: center;
}

.title-text {
  max-width: 250px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 600;
  color: #1f2d3d;
}

.summary-cell {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.summary-item {
  display: grid;
  grid-template-columns: 36px 1fr;
  align-items: baseline;
  gap: 8px;
}

.summary-label {
  color: #74808d;
  font-size: 12px;
}

.summary-text {
  margin: 0;
  color: #243b53;
}

.stats-cell {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
}

.rate-text {
  font-size: 12px;
  color: #52606d;
}

.meta-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  color: #52606d;
  font-size: 13px;
}

.table-card :deep(.arco-table-th) {
  font-weight: 700;
  color: #243b53;
  background: #f7faff;
}

.table-card :deep(.arco-table-td) {
  vertical-align: top;
}

@media (max-width: 900px) {
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
}
</style>
