<template>
  <div id="userLayout">
    <a-layout class="page-container">
      <a-layout-header class="header">
        <div class="header-subtitle">毕业设计 by Sjj</div>
      </a-layout-header>
      <a-layout-content class="content">
        <router-view />
      </a-layout-content>
      <a-layout-footer class="footer">
        <a target="_blank"> 在线编程学习平台 by SJJ </a>
      </a-layout-footer>
    </a-layout>
  </div>
</template>

<style scoped>
#userLayout {
  text-align: center;
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: url("https://gw.alipayobjects.com/zos/rmsportal/FfdJeJRQWjEeGTpqgBKj.png")
    0% 0% / 100% 100%;
}

#userLayout .page-container {
  min-height: 100vh;
  width: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background: transparent;
}

#userLayout .header {
  height: auto;
  line-height: 1.5;
  margin-bottom: 8px;
  padding: 0;
  background: transparent;
}

#userLayout .header-subtitle {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.65);
  letter-spacing: 0.5px;
}

#userLayout .content {
  margin-bottom: 16px;
  padding: 0;
}

#userLayout .footer {
  margin-top: 24px;
  padding: 0;
  text-align: center;
  background: transparent;
}
</style>
<script></script>
