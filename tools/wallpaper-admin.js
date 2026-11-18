#!/usr/bin/env node
// Admin helper (Node 18+, firebase-admin). NOT included in the app. Not executed/tested in the build sandbox.
// Setup: cd tools && npm i firebase-admin && put your service-account key at tools/serviceAccountKey.json (never commit it)
// Usage:
//   node wallpaper-admin.js import wallpapers.sample.json   # add/update wallpapers from a JSON array
//   node wallpaper-admin.js list [category]
//   node wallpaper-admin.js set <id> <field> <value>        # e.g. set sample-8k-001 trending true
//   node wallpaper-admin.js remove <id>
//   node wallpaper-admin.js category <slug> "<Name>" [order]
const admin = require('firebase-admin');
const fs = require('fs');
admin.initializeApp({ credential: admin.credential.cert(require('./serviceAccountKey.json')) });
const db = admin.firestore();

const parse = v => (v === 'true' ? true : v === 'false' ? false : isNaN(v) || v === '' ? v : Number(v));

(async () => {
  const [cmd, a, b, c] = process.argv.slice(2);
  if (cmd === 'import') {
    const items = JSON.parse(fs.readFileSync(a, 'utf8'));
    for (const w of items) {
      const { id, ...data } = w;
      data.maxSide = Math.max(data.width || 0, data.height || 0); // real resolution drives the 8K/4K badge
      data.createdAt = data.createdAt || Date.now();
      data.downloads = data.downloads || 0;
      await db.collection('wallpapers').doc(id).set(data, { merge: true });
      console.log('saved', id, data.maxSide >= 7680 ? '(8K)' : data.maxSide >= 3840 ? '(4K)' : '');
    }
  } else if (cmd === 'list') {
    let q = db.collection('wallpapers');
    if (a) q = q.where('category', '==', a);
    (await q.get()).forEach(d => console.log(d.id, d.data().title, d.data().category));
  } else if (cmd === 'set') {
    await db.collection('wallpapers').doc(a).update({ [b]: parse(c) });
    console.log('updated');
  } else if (cmd === 'remove') {
    await db.collection('wallpapers').doc(a).delete();
    console.log('removed');
  } else if (cmd === 'category') {
    await db.collection('categories').doc(a).set({ name: b, order: Number(c || 1000) });
    console.log('category saved');
  } else {
    console.log('See usage at top of file.');
  }
})().catch(e => { console.error(e); process.exit(1); });
