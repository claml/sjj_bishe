const fs = require('fs');
const p = 'D:/SJJ_biyesheji/bishe/oj-frontend/src/views/post/DiscussionView.vue';
let s = fs.readFileSync(p, 'utf8');
const re = /message\.error\(`[^`]*\?\?\{res\.message\}`\);/g;
let i = 0;
s = s.replace(re, () => {
  i += 1;
  return i === 1
    ? 'message.error(`加载失败：${res.message}`);'
    : 'message.error(`发布失败：${res.message}`);';
});
fs.writeFileSync(p, s, 'utf8');
console.log('replaced', i);
