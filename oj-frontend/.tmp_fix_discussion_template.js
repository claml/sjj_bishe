const fs = require('fs');
const p = 'D:/SJJ_biyesheji/bishe/oj-frontend/src/views/post/DiscussionView.vue';
let s = fs.readFileSync(p, 'utf8');
const marker = '    <a-list';
const idx = s.indexOf(marker);
if (idx === -1) {
  throw new Error('marker not found');
}
const head = `<template>
  <div id="discussionView">
    <a-card class="publish-card" title="发布讨论" :bordered="false">
      <a-space direction="vertical" fill>
        <a-textarea
          v-model="publishForm.content"
          :max-length="2000"
          :auto-size="{ minRows: 4, maxRows: 8 }"
          allow-clear
          placeholder="分享你的想法..."
        />
        <div
          v-if="publishForm.images.length"
          class="publish-image-preview-list"
        >
          <div
            v-for="(url, index) in publishForm.images"
            :key="url"
            class="preview-item"
          >
            <a-image :src="url" width="120" height="120" fit="cover">
              <template #error>
                <div class="img-error">图片加载失败</div>
              </template>
            </a-image>
            <a-button
              type="text"
              size="mini"
              status="danger"
              @click="removePublishImage(index)"
              >移除</a-button
            >
          </div>
        </div>
        <a-space wrap class="publish-actions">
          <a-input
            v-model="publishForm.title"
            style="width: 240px"
            placeholder="标题（可选）"
            allow-clear
          />
          <a-input-tag
            v-model="publishForm.tags"
            placeholder="标签（可选）"
            allow-clear
          />
          <a-button :loading="uploadingImage" @click="triggerImageSelect"
            >上传图片</a-button
          >
          <input
            ref="postImageInputRef"
            type="file"
            accept="image/png,image/jpeg,image/webp"
            style="display: none"
            @change="handlePostImageSelect"
          />
          <a-button
            type="primary"
            :loading="publishing"
            :disabled="!publishForm.content?.trim()"
            @click="doPublish"
          >
            发布
          </a-button>
        </a-space>
      </a-space>
    </a-card>

    <a-card class="search-card" :bordered="false">
      <a-space wrap class="search-actions">
        <a-input
          v-model="searchForm.title"
          class="search-input"
          placeholder="搜索帖子标题"
          allow-clear
          @press-enter="doSearch"
        />
        <a-input-tag
          v-model="searchForm.tags"
          class="search-tag-input"
          placeholder="按标签搜索"
          allow-clear
        />
        <a-button type="primary" @click="doSearch">搜索</a-button>
        <a-button @click="resetSearch">重置</a-button>
      </a-space>
    </a-card>

    <div
      v-if="
        searchParams.title || (searchParams.tags && searchParams.tags.length)
      "
      class="active-filters"
    >
      <span class="active-label">当前筛选</span>
      <a-tag v-if="searchParams.title" color="arcoblue" bordered>
        标题：{{ searchParams.title }}
      </a-tag>
      <a-tag
        v-for="tag in searchParams.tags || []"
        :key="\`active-\${tag}\`"
        color="orangered"
        bordered
      >
        标签：{{ tag }}
      </a-tag>
      <a-button type="text" size="mini" @click="resetSearch">清空筛选</a-button>
    </div>
`;

s = head + s.slice(idx);
fs.writeFileSync(p, s, 'utf8');
