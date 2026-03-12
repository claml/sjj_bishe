import type { Router } from "vue-router";

interface OpenPageOptions {
  newTab?: boolean;
}

/**
 * 统一页面跳转方法：支持当前页跳转和新标签页打开。
 */
export const openPage = (
  router: Router,
  path: string,
  options: OpenPageOptions = {}
) => {
  const { newTab = false } = options;
  if (!newTab) {
    return router.push(path);
  }

  const targetUrl = router.resolve(path).href;
  window.open(targetUrl, "_blank", "noopener");
  return Promise.resolve();
};

/**
 * 处理 Ctrl / Cmd / Shift / 中键 等“新标签打开”场景。
 */
export const shouldOpenInNewTab = (event?: MouseEvent) => {
  if (!event) {
    return false;
  }
  return event.metaKey || event.ctrlKey || event.shiftKey || event.button === 1;
};
