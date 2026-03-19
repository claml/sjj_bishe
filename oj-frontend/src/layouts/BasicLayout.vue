<template>
  <div id="basicLayout">
    <a-layout class="main-layout">
      <a-layout-header class="header">
        <GlobalHeader />
      </a-layout-header>
      <a-layout-content class="content">
        <router-view />
      </a-layout-content>
      <a-layout-footer class="footer">
        <a> 在线编程学习平台 </a>
      </a-layout-footer>
    </a-layout>
  </div>
</template>

<style scoped>
#basicLayout {
  min-height: 100vh;
}

#basicLayout .main-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

#basicLayout .header {
  margin-bottom: 16px;
  height: 72px;
  padding: 0 20px;
  background: #f5f6f7;
  border-bottom: 1px solid #e5e6eb;
  box-shadow: none;
}

#basicLayout .content {
  flex: 1;
  background: linear-gradient(to right, #fefefe, #fff);
  margin-bottom: 16px;
  padding: 20px;
}

#basicLayout .footer {
  background: #efefef;
  padding: 16px;
  text-align: center;
}
</style>
<script>
import GlobalHeader from "@/components/GlobalHeader";

export default {
  components: { GlobalHeader },
};
</script>
