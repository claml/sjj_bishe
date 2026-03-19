const fs = require('fs');
const p = 'D:/SJJ_biyesheji/bishe/oj-frontend/src/views/post/DiscussionView.vue';
let s = fs.readFileSync(p, 'utf8');

s = s.replace(/<a-empty description="[^"]*" \/>/, '<a-empty description="暂无帖子，快来发布第一条讨论吧" />');
s = s.replace(/\{\{ item\.user\?\.userName \|\| "[^"]*" \}\}/, '{{ item.user?.userName || "匿名用户" }}');
s = s.replace(/<div class="img-error">[^<]*<\/div>/g, '<div class="img-error">图片加载失败</div>');
s = s.replace(/>[^<]* \{\{ item\.thumbNum \|\| 0 \}\}<\/a-button/, '>点赞 {{ item.thumbNum || 0 }}</a-button');
s = s.replace(/>[^<]* \{\{ item\.favourNum \|\| 0 \}\}<\/a-button/, '>收藏 {{ item.favourNum || 0 }}</a-button');
s = s.replace(/@click="goPostDetail\(item\.id, \$event\)"\s*>\s*[^<]*<\/a-button/, '@click="goPostDetail(item.id, $event)"\n                >查看详情</a-button');

let errTplIdx = 0;
s = s.replace(/message\.error\(`[^`]*\$\{res\.message\}`\);/g, () => {
  errTplIdx += 1;
  if (errTplIdx === 1) {
    return 'message.error(`加载失败：${res.message}`);';
  }
  return 'message.error(`发布失败：${res.message}`);';
});

let errIdx = 0;
s = s.replace(/message\.error\(res\.message \|\| "[^"]*"\);/g, () => {
  errIdx += 1;
  if (errIdx === 1) {
    return 'message.error(res.message || "图片上传失败");';
  }
  return 'message.error(res.message || "操作失败");';
});

let succIdx = 0;
s = s.replace(/message\.success\("[^"]*"\);/g, () => {
  succIdx += 1;
  if (succIdx === 1) {
    return 'message.success("图片上传成功");';
  }
  return 'message.success("发布成功");';
});

let warnIdx = 0;
s = s.replace(/message\.warning\("[^"]*"\);/g, () => {
  warnIdx += 1;
  if (warnIdx === 1) {
    return 'message.warning("内容不能为空");';
  }
  return 'message.warning("请先登录");';
});

fs.writeFileSync(p, s, 'utf8');
