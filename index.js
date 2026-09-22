const express = require('express');
const fs = require('fs/promises');
const path = require('path');
const app = express();
const ROOT = path.resolve(__dirname);

function escapeHtml(s) {
  return (s || '').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
}

// 目录列表
app.get('/', async (req, res) => {
  try {
    const rel = req.query.path || '.';
    const target = path.resolve(ROOT, rel);
    if (!target.startsWith(ROOT)) return res.status(403).send('Forbidden');
    const stat = await fs.stat(target);
    if (!stat.isDirectory()) return res.redirect(`/file?path=${encodeURIComponent(rel)}`);
    const list = await fs.readdir(target, {withFileTypes:true});
    let html = `<a href="/?path=${encodeURIComponent(path.dirname(rel))}">..</a><br>`;
    for(const item of list){
      const p = path.join(rel, item.name);
      if(item.isDirectory()){
        html += `<a href="/?path=${encodeURIComponent(p)}">[DIR] ${escapeHtml(item.name)}</a><br>`;
      }else{
        html += `<a href="/file?path=${encodeURIComponent(p)}">[FILE] ${escapeHtml(item.name)}</a><br>`;
      }
    }
    res.send(html);
  }catch(e){
    res.status(500).send(escapeHtml(e.message));
  }
});

// 查看文件内容
app.get('/file', async (req, res) => {
  try {
    const rel = req.query.path;
    const target = path.resolve(ROOT, rel);
    if (!target.startsWith(ROOT)) return res.status(403).send('Forbidden');
    const stat = await fs.stat(target);
    if (stat.isDirectory()) return res.redirect(`/?path=${encodeURIComponent(rel)}`);
    const content = await fs.readFile(target, 'utf8');
    res.send(`<a href="/?path=${encodeURIComponent(path.dirname(rel))}">..</a><pre>${escapeHtml(content)}</pre>`);
  }catch(e){
    res.status(500).send(escapeHtml(e.message));
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT);
