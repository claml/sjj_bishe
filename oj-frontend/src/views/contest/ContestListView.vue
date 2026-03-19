<template>
  <div id="contestListView">
    <a-card class="filter-card search-card" :bordered="false">
      <div class="search-head">
        <div class="search-title">竞赛筛选</div>
        <div class="search-subtitle">按竞赛名称和状态快速定位目标场次</div>
      </div>
      <a-form :model="searchParams" layout="inline" class="search-form">
        <a-form-item
          field="title"
          label="竞赛名称"
          class="search-item search-item-wide"
        >
          <a-input
            v-model="searchParams.title"
            placeholder="输入竞赛名称"
            allow-clear
            @press-enter="doSearch"
          />
        </a-form-item>
        <a-form-item field="status" label="状态" class="search-item">
          <a-select
            v-model="searchParams.status"
            allow-clear
            placeholder="全部状态"
          >
            <a-option value="not_started">未开始</a-option>
            <a-option value="running">进行中</a-option>
            <a-option value="ended">已结束</a-option>
          </a-select>
        </a-form-item>
        <a-form-item class="search-actions">
          <a-space>
            <a-button type="primary" @click="doSearch">搜索</a-button>
            <a-button @click="resetSearch">重置</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <a-card class="table-card" :bordered="false">
      <template #title>竞赛列表</template>
      <template #extra>
        <a-button @click="refreshData">刷新</a-button>
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
          current: searchParams.current,
          pageSize: searchParams.pageSize,
          total,
        }"
        @page-change="onPageChange"
        @page-size-change="onPageSizeChange"
      >
        <template #titleInfo="{ record }">
          <div class="title-cell">
            <span class="title-text">{{ record.title || "未命名竞赛" }}</span>
            <a-typography-paragraph
              :ellipsis="{ rows: 1, showTooltip: true }"
              class="desc-text"
            >
              {{ record.description || "暂无描述" }}
            </a-typography-paragraph>
          </div>
        </template>
        <template #status="{ record }">
          <a-tag :color="statusColorMap[record.status] || 'gray'">
            {{ statusTextMap[record.status] || "未知" }}
          </a-tag>
        </template>
        <template #timeRange="{ record }">
          <div class="time-cell">
            <span>{{ formatDateTime(record.startTime) }}</span>
            <span class="time-divider">~</span>
            <span>{{ formatDateTime(record.endTime) }}</span>
          </div>
        </template>
        <template #optional="{ record }">
          <a-space>
            <a-button
              type="outline"
              size="small"
              @click="toContestDetail(record.id)"
            >
              进入竞赛
            </a-button>
            <a-button
              v-if="!record.joined && record.status !== 'ended'"
              type="primary"
              size="small"
              @click="openJoinCard(record.id)"
            >
              参加竞赛
            </a-button>
          </a-space>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:visible="joinCardVisible"
      title="请输入参赛码"
      :mask-closable="false"
      :confirm-loading="joining"
      ok-text="参加竞赛"
      @ok="submitJoin"
      @cancel="handleJoinCancel"
    >
      <a-card :bordered="false">
        <a-space direction="vertical" fill>
          <a-typography-text>请输入参赛码（如无可留空）</a-typography-text>
          <a-input
            v-model="joinCodeInput"
            placeholder="输入参赛码"
            allow-clear
            @press-enter="submitJoin"
          />
        </a-space>
      </a-card>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from "vue";
import message from "@arco-design/web-vue/es/message";
import { useRouter } from "vue-router";
import moment from "moment";
import {
  ContestVO,
  getErrorMessage,
  joinContest,
  listContestVOByPage,
} from "@/services/contestApi";

type ContestStatus = "not_started" | "running" | "ended";

interface ContestSearchParams {
  title: string;
  status?: ContestStatus;
  current: number;
  pageSize: number;
}

const router = useRouter();
const dataList = ref<ContestVO[]>([]);
const total = ref(0);
const loading = ref(false);
const searchParams = ref<ContestSearchParams>({
  title: "",
  status: undefined,
  current: 1,
  pageSize: 10,
});
const joinCardVisible = ref(false);
const joinContestId = ref<number>();
const joinCodeInput = ref("");
const joining = ref(false);

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
    title: "竞赛信息",
    slotName: "titleInfo",
    minWidth: 320,
  },
  {
    title: "状态",
    slotName: "status",
    width: 120,
  },
  {
    title: "有效时间",
    slotName: "timeRange",
    minWidth: 300,
  },
  {
    title: "操作",
    slotName: "optional",
    width: 180,
  },
];

const loadData = async () => {
  loading.value = true;
  try {
    const pageData = await listContestVOByPage(searchParams.value);
    dataList.value = pageData.records ?? [];
    total.value = Number(pageData.total ?? 0);
  } catch (error) {
    message.error(getErrorMessage(error, "加载竞赛失败"));
  } finally {
    loading.value = false;
  }
};

const doSearch = () => {
  searchParams.value = {
    ...searchParams.value,
    current: 1,
  };
};

const resetSearch = () => {
  searchParams.value = {
    ...searchParams.value,
    title: "",
    status: undefined,
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

const toContestDetail = (id: number) => {
  router.push(`/contest/${id}`);
};

const openJoinCard = (contestId: number) => {
  joinContestId.value = contestId;
  joinCodeInput.value = "";
  joinCardVisible.value = true;
};

const handleJoinCancel = () => {
  joinCardVisible.value = false;
  joinContestId.value = undefined;
  joinCodeInput.value = "";
};

const submitJoin = async () => {
  if (!joinContestId.value) {
    return;
  }
  joining.value = true;
  try {
    const joinCode = joinCodeInput.value.trim();
    await joinContest(joinContestId.value, joinCode || undefined);
    message.success("参加成功");
    handleJoinCancel();
    await loadData();
  } catch (error) {
    message.error(getErrorMessage(error, "参加失败"));
  } finally {
    joining.value = false;
  }
};

const formatDateTime = (value?: string) => {
  if (!value) {
    return "-";
  }
  return moment(value).format("YYYY-MM-DD HH:mm:ss");
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
#contestListView {
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
  min-width: 220px;
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

.title-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.title-text {
  font-weight: 600;
  color: #243b53;
}

.desc-text {
  margin: 0;
  color: #52606d;
}

.time-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #334e68;
}

.time-divider {
  color: #829ab1;
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
