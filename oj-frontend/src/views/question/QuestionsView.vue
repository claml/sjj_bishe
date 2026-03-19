<template>
  <div id="questionsView">
    <a-card class="filter-card search-card" :bordered="false">
      <div class="search-head">
        <div class="search-title">题库检索</div>
        <div class="search-subtitle">按题目名称和标签快速筛选</div>
      </div>
      <a-form :model="searchParams" layout="inline" class="search-form">
        <a-form-item
          field="title"
          label="名称"
          class="search-item search-item-wide"
        >
          <a-input
            v-model="searchParams.title"
            placeholder="请输入名称关键词"
            allow-clear
            @press-enter="doSubmit"
          />
        </a-form-item>
        <a-form-item
          field="tags"
          label="标签"
          class="search-item search-item-wide"
        >
          <a-input-tag
            v-model="searchParams.tags"
            placeholder="请输入标签"
            allow-clear
          />
        </a-form-item>
        <a-form-item class="search-actions">
          <a-space>
            <a-button type="primary" @click="doSubmit">搜索</a-button>
            <a-button @click="resetSearch">重置</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <a-card class="table-card" :bordered="false">
      <template #title>题目列表</template>
      <template #extra>
        <a-space>
          <a-button @click="refreshData">刷新</a-button>
          <a-button v-if="isAdmin" type="primary" @click="toAddQuestionPage">
            创建题目
          </a-button>
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
        <template #questionNo="{ rowIndex }">
          <span class="question-no">{{ getQuestionNo(rowIndex) }}</span>
        </template>
        <template #tags="{ record }">
          <a-space wrap>
            <a-tag
              v-for="(tag, index) of record.tags"
              :key="index"
              color="arcoblue"
              bordered
              class="clickable-tag"
              @click="searchByTag(tag)"
            >
              {{ tag }}
            </a-tag>
            <a-tag v-if="!(record.tags || []).length" color="gray">
              暂无标签
            </a-tag>
          </a-space>
        </template>
        <template #acceptedRate="{ record }">
          <span class="rate-text">{{ formatAcceptedRate(record) }}</span>
        </template>
        <template #optional="{ record }">
          <a-space>
            <a-button
              type="primary"
              size="small"
              @click="toQuestionPage(record)"
            >
              做题
            </a-button>
          </a-space>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from "vue";
import {
  Question,
  QuestionControllerService,
  QuestionQueryRequest,
  QuestionVO,
} from "../../../generated";
import message from "@arco-design/web-vue/es/message";
import { useRouter } from "vue-router";
import { useStore } from "vuex";
import ACCESS_ENUM from "@/access/accessEnum";

const dataList = ref<QuestionVO[]>([]);
const total = ref(0);
const loading = ref(false);

const searchParams = ref<QuestionQueryRequest>({
  title: "",
  tags: [],
  pageSize: 10,
  current: 1,
  sortField: "createTime",
  sortOrder: "descend",
});

const router = useRouter();
const store = useStore();

const isAdmin = computed(
  () => store.state.user?.loginUser?.userRole === ACCESS_ENUM.ADMIN
);

const loadData = async () => {
  loading.value = true;
  try {
    const res = await QuestionControllerService.listQuestionVoByPageUsingPost(
      searchParams.value
    );
    if (res.code === 0 && res.data) {
      dataList.value = res.data.records ?? [];
      total.value = Number(res.data.total ?? 0);
    } else {
      message.error("加载失败: " + res.message);
    }
  } finally {
    loading.value = false;
  }
};

const columns = [
  {
    title: "题号",
    slotName: "questionNo",
    width: 100,
  },
  {
    title: "题目名称",
    dataIndex: "title",
    minWidth: 300,
  },
  {
    title: "标签",
    slotName: "tags",
    minWidth: 220,
  },
  {
    title: "通过率",
    slotName: "acceptedRate",
    width: 180,
  },
  {
    title: "操作",
    slotName: "optional",
    width: 120,
  },
];

const formatAcceptedRate = (record: QuestionVO) => {
  const acceptedNum = Number(record?.acceptedNum ?? 0);
  const submitNum = Number(record?.submitNum ?? 0);
  const safeAccepted = Number.isFinite(acceptedNum) ? acceptedNum : 0;
  const safeSubmit = Number.isFinite(submitNum) ? submitNum : 0;
  if (safeSubmit <= 0) {
    return `0.00% (${safeAccepted}/${safeSubmit})`;
  }
  const rate = ((safeAccepted / safeSubmit) * 100).toFixed(2);
  return `${rate}% (${safeAccepted}/${safeSubmit})`;
};

const getQuestionNo = (rowIndex: number) => {
  const current = searchParams.value.current ?? 1;
  const pageSize = searchParams.value.pageSize ?? 10;
  return (current - 1) * pageSize + rowIndex + 1;
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

const toQuestionPage = (question: QuestionVO | Question) => {
  if (!question.id) {
    return;
  }
  router.push({
    path: `/view/question/${question.id}`,
  });
};

const toAddQuestionPage = () => {
  router.push({
    path: "/add/question",
  });
};

const doSubmit = () => {
  searchParams.value = {
    ...searchParams.value,
    current: 1,
  };
};

const searchByTag = (tag: string) => {
  const normalizedTag = String(tag ?? "").trim();
  if (!normalizedTag) {
    return;
  }
  searchParams.value = {
    ...searchParams.value,
    tags: [normalizedTag],
    current: 1,
  };
};

const resetSearch = () => {
  searchParams.value = {
    ...searchParams.value,
    title: "",
    tags: [],
    current: 1,
  };
};

const refreshData = () => {
  loadData();
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
#questionsView {
  max-width: 1360px;
  margin: 0 auto;
  padding: 6px 0 20px;
}

.filter-card {
  margin-bottom: 14px;
  border: 1px solid #d9e7fb;
  border-radius: 16px;
  background: linear-gradient(120deg, #f9fcff 0%, #f2f8ff 100%);
  box-shadow: 0 8px 18px rgba(16, 42, 67, 0.06);
}

.search-form {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 12px 14px;
}

.search-head {
  margin-bottom: 12px;
}

.search-title {
  font-size: 17px;
  font-weight: 700;
  color: #102a43;
  line-height: 1.2;
}

.search-subtitle {
  margin-top: 4px;
  color: #627d98;
  font-size: 13px;
}

.search-item {
  min-width: 260px;
}

.search-item-wide {
  flex: 1;
  max-width: 420px;
}

.search-actions {
  margin-left: auto;
}

.search-form :deep(.arco-form-item) {
  margin: 0;
}

.search-form :deep(.arco-form-item-label-col > label) {
  color: #334e68;
  font-weight: 600;
}

.search-form :deep(.arco-input-wrapper),
.search-form :deep(.arco-select-view),
.search-form :deep(.arco-input-tag) {
  border-radius: 10px;
  border: 1px solid #d7e3f7;
  background: #ffffff;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.search-form :deep(.arco-input-wrapper:hover),
.search-form :deep(.arco-select-view:hover),
.search-form :deep(.arco-input-tag:hover) {
  border-color: #9db8e8;
}

.search-form :deep(.arco-input-wrapper.arco-input-focus),
.search-form :deep(.arco-select-view.arco-select-view-focus),
.search-form :deep(.arco-input-tag.arco-input-tag-focus) {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.16);
}

.search-actions :deep(.arco-btn) {
  min-width: 86px;
  border-radius: 10px;
}

.search-actions :deep(.arco-btn-primary) {
  box-shadow: 0 6px 14px rgba(15, 106, 216, 0.2);
}

.table-card :deep(.arco-card-header),
.filter-card :deep(.arco-card-header) {
  border-bottom: none;
}

.table-card :deep(.arco-card-body),
.filter-card :deep(.arco-card-body) {
  padding: 16px 18px;
}

.question-no {
  display: inline-flex;
  min-width: 34px;
  height: 24px;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: #eaf3ff;
  color: #205493;
  font-weight: 700;
  font-size: 12px;
}

.rate-text {
  color: #334e68;
}

.clickable-tag {
  cursor: pointer;
}

.clickable-tag:hover {
  opacity: 0.86;
}

.table-card :deep(.arco-table-th) {
  font-weight: 700;
  color: #243b53;
  background: #f7faff;
}

@media (max-width: 768px) {
  .search-item,
  .search-item-wide {
    min-width: 100%;
    max-width: none;
    flex: 1 1 100%;
  }

  .search-actions {
    margin-left: 0;
    width: 100%;
  }

  .search-actions :deep(.arco-space) {
    display: flex;
    width: 100%;
    gap: 10px;
  }

  .search-actions :deep(.arco-space-item) {
    flex: 1;
  }

  .search-actions :deep(.arco-btn) {
    width: 100%;
  }
}
</style>
