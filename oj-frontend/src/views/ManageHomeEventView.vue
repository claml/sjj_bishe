<template>
  <div id="manageHomeEventView">
    <section class="hero">
      <div>
        <h2 class="hero-title">管理首页活动</h2>
        <p class="hero-subtitle">维护首页展示的近期活动与赛训安排。</p>
      </div>
      <a-button type="primary" @click="openAddModal">新增活动</a-button>
    </section>

    <a-card class="table-card" :bordered="false">
      <a-table
        :columns="columns"
        :data="dataList"
        :loading="loading"
        row-key="id"
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
        <template #eventTime="{ record }">
          {{ formatDateTime(record.eventTime) }}
        </template>
        <template #eventUrl="{ record }">
          <a-link
            v-if="record.eventUrl"
            :href="getExternalLink(record.eventUrl)"
            target="_blank"
            rel="noopener noreferrer"
          >
            详情链接
          </a-link>
          <span v-else>-</span>
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
            <a-popconfirm
              content="确认删除该活动配置？"
              @ok="doDelete(record.id)"
            >
              <a-button status="danger" size="small">删除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:visible="modalVisible"
      :title="modalMode === 'add' ? '新增活动' : '编辑活动'"
      @ok="submitModal"
      @cancel="resetModalForm"
    >
      <a-form :model="modalForm" layout="vertical">
        <a-form-item field="contestType" label="活动类型">
          <a-select
            v-model="modalForm.contestType"
            placeholder="请选择活动类型"
          >
            <a-option
              v-for="item in contestTypeOptions"
              :key="item"
              :value="item"
            >
              {{ item }}
            </a-option>
          </a-select>
        </a-form-item>
        <a-form-item field="title" label="活动名称">
          <a-input v-model="modalForm.title" placeholder="请输入活动名称" />
        </a-form-item>
        <a-form-item field="eventTime" label="活动时间">
          <a-date-picker
            v-model="modalForm.eventTime"
            show-time
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item field="location" label="地点">
          <a-input v-model="modalForm.location" placeholder="线上 / 线下地点" />
        </a-form-item>
        <a-form-item field="eventUrl" label="详情链接">
          <a-input
            v-model="modalForm.eventUrl"
            placeholder="可选，粘贴完整活动地址（含 http:// 或 https://）"
          />
        </a-form-item>
        <a-form-item field="description" label="描述">
          <a-textarea
            v-model="modalForm.description"
            :auto-size="{ minRows: 2, maxRows: 4 }"
            placeholder="可选，填写简介或报名提示"
          />
        </a-form-item>
        <a-form-item field="sortOrder" label="排序值（越小越靠前）">
          <a-input-number
            v-model="modalForm.sortOrder"
            :min="0"
            mode="button"
            style="width: 100%"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from "vue";
import message from "@arco-design/web-vue/es/message";
import moment from "moment";
import {
  addHomeEvent,
  deleteHomeEvent,
  getErrorMessage,
  HomeEventAddPayload,
  HomeEventItem,
  HomeEventQueryRequest,
  HomeEventUpdatePayload,
  listHomeEventByPage,
  updateHomeEvent,
} from "@/services/homeEventApi";

type ModalMode = "add" | "edit";

interface ModalFormState {
  id: number;
  contestType: string;
  title: string;
  eventTime: string;
  location: string;
  eventUrl: string;
  description: string;
  sortOrder: number;
}

const contestTypeOptions = ["赛训", "比赛"];

const makeDefaultModalForm = (): ModalFormState => ({
  id: 0,
  contestType: "赛训",
  title: "",
  eventTime: "",
  location: "",
  eventUrl: "",
  description: "",
  sortOrder: 0,
});

const dataList = ref<HomeEventItem[]>([]);
const total = ref(0);
const loading = ref(false);
const modalVisible = ref(false);
const modalMode = ref<ModalMode>("add");
const modalForm = ref<ModalFormState>(makeDefaultModalForm());

const listQuery = ref<HomeEventQueryRequest>({
  current: 1,
  pageSize: 10,
  sortField: "eventTime",
  sortOrder: "ascend",
});

const columns = [
  {
    title: "类型",
    dataIndex: "contestType",
    width: 110,
  },
  {
    title: "活动名称",
    dataIndex: "title",
    minWidth: 220,
  },
  {
    title: "活动时间",
    slotName: "eventTime",
    width: 180,
  },
  {
    title: "地点",
    dataIndex: "location",
    width: 160,
  },
  {
    title: "排序值（越小越靠前）",
    dataIndex: "sortOrder",
    width: 90,
  },
  {
    title: "链接",
    slotName: "eventUrl",
    width: 100,
  },
  {
    title: "操作",
    slotName: "optional",
    width: 140,
  },
];

const loadData = async () => {
  loading.value = true;
  try {
    const pageData = await listHomeEventByPage(listQuery.value);
    dataList.value = pageData.records ?? [];
    total.value = Number(pageData.total ?? 0);
  } catch (error) {
    message.error(getErrorMessage(error, "加载活动配置失败"));
  } finally {
    loading.value = false;
  }
};

const formatDateTime = (value?: string) => {
  if (!value) {
    return "-";
  }
  return moment(value).format("YYYY-MM-DD HH:mm");
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

const openAddModal = () => {
  modalMode.value = "add";
  modalForm.value = makeDefaultModalForm();
  modalVisible.value = true;
};

const openEditModal = (record: HomeEventItem) => {
  modalMode.value = "edit";
  modalForm.value = {
    id: record.id,
    contestType: record.contestType || "赛训",
    title: record.title || "",
    eventTime: record.eventTime || "",
    location: record.location || "",
    eventUrl: record.eventUrl || "",
    description: record.description || "",
    sortOrder: Number(record.sortOrder ?? 0),
  };
  modalVisible.value = true;
};

const resetModalForm = () => {
  modalVisible.value = false;
  modalForm.value = makeDefaultModalForm();
};

const validateModalForm = () => {
  if (!modalForm.value.contestType.trim()) {
    message.warning("请选择活动类型");
    return false;
  }
  if (!modalForm.value.title.trim()) {
    message.warning("请输入活动名称");
    return false;
  }
  if (!modalForm.value.eventTime) {
    message.warning("请选择活动时间");
    return false;
  }
  const eventUrl = modalForm.value.eventUrl.trim();
  if (eventUrl && !/^(https?:)?\/\//i.test(eventUrl)) {
    message.warning("详情链接请填写完整地址（http:// 或 https://）");
    return false;
  }
  return true;
};

const submitModal = async () => {
  if (!validateModalForm()) {
    return false;
  }
  const basePayload: HomeEventAddPayload = {
    contestType: modalForm.value.contestType.trim(),
    title: modalForm.value.title.trim(),
    eventTime: modalForm.value.eventTime,
    location: modalForm.value.location.trim(),
    eventUrl: modalForm.value.eventUrl.trim(),
    description: modalForm.value.description.trim(),
    sortOrder: modalForm.value.sortOrder,
  };
  try {
    if (modalMode.value === "add") {
      await addHomeEvent(basePayload);
      message.success("新增成功");
    } else {
      const payload: HomeEventUpdatePayload = {
        id: modalForm.value.id,
        ...basePayload,
      };
      await updateHomeEvent(payload);
      message.success("更新成功");
    }
    resetModalForm();
    await loadData();
    return true;
  } catch (error) {
    message.error(getErrorMessage(error, "保存失败"));
    return false;
  }
};

const doDelete = async (id: number) => {
  try {
    await deleteHomeEvent(id);
    message.success("删除成功");
    await loadData();
  } catch (error) {
    message.error(getErrorMessage(error, "删除失败"));
  }
};

watch(
  listQuery,
  () => {
    loadData();
  },
  { deep: true, immediate: true }
);
</script>

<style scoped>
#manageHomeEventView {
  max-width: 1180px;
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

.table-card :deep(.arco-card-body) {
  padding: 12px 18px 18px;
}

.table-card :deep(.arco-table-th) {
  font-weight: 700;
  color: #243b53;
  background: #f7faff;
}

@media (max-width: 768px) {
  .hero {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
