const fs = require('fs');
const p = 'D:/SJJ_biyesheji/bishe/oj-frontend/src/views/post/DiscussionView.vue';
const lines = fs.readFileSync(p, 'utf8').split(/\r?\n/);
let bad = false;
for (let i = 0; i < lines.length; i += 1) {
  const line = lines[i];
  if (/[锟�]/.test(line) || line.includes('ͼƬ') || line.includes('����')) {
    bad = true;
    console.log(String(i + 1).padStart(4, ' ') + ': ' + line);
  }
}
if (!bad) {
  console.log('NO_BAD_LINES');
}
