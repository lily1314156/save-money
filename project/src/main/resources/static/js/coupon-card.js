/* ──────────────────────────────────────────────
   共用：優惠券卡片產生器（index / stores / profile 三頁共用）

   核心安全點：所有來自 DB 的文字都用 textContent，不用 innerHTML 拼字串。
   商家就算在欄位塞 <script> 或 <img onerror=...>，也只會顯示成文字、不會被執行。

   三頁的卡片結構、class 相同，差異只有：卡片外層 class、進場動畫時間、
   右上角按鈕、price 版型的欄位名——這些都用 opts 參數傳進來，其餘共用一份。
   ────────────────────────────────────────────── */

/* 建一個元素；文字一律用 textContent（安全） */
function mk(tag, className, text) {
  const el = document.createElement(tag);
  if (className) el.className = className;
  if (text != null) el.textContent = text;
  return el;
}

/* 把含換行(\n)的文字安全塞進元素：換行用 <br>，文字本身當純文字 */
function setMultiline(el, text) {
  String(text ?? '').split('\n').forEach((line, idx) => {
    if (idx > 0) el.appendChild(document.createElement('br'));
    el.appendChild(document.createTextNode(line));
  });
}

/* 距離到期還剩幾天：今天到期=0、已過期=負數、沒有 endDate=null
   endDate 是後端給的 'yyyy-MM-dd' 字串 */
function daysUntil(endDate) {
  if (!endDate) return null;

  // 加 'T00:00:00' 讓它以「當地時間」解析，避免被當成 UTC 差一天
  const end = new Date(endDate + 'T00:00:00');
  const today = new Date();
  today.setHours(0, 0, 0, 0);   // 今天歸零到 00:00，算「差幾天」才會是整數

  return Math.round((end - today) / 86400000);  // 86400000 = 一天的毫秒數
}

/* 到期文案：7 天內 → 「N天後到期」；其他 → 只顯示期限 */
function expiryText(endDate) {
  const daysLeft = daysUntil(endDate);
  if (daysLeft === null) return null;

  if (daysLeft < 0)   return '已過期';
  if (daysLeft === 0) return '今天到期';
  if (daysLeft <= 7)  return `${daysLeft}天後到期`;
  return '期限：' + endDate.replaceAll('-', '.');   // 2026-12-31 → 2026.12.31
}

/* 條款清單 <ul>（每筆 term 用 textContent）
   沒有任何條款就回傳 null，不塞空的 <ul> 進卡片 */
function buildTerms(terms) {
  if (!terms || terms.length === 0) return null;
  const ul = mk('ul', 'coupon-meta');
  terms.forEach(m => ul.appendChild(mk('li', null, m)));
  return ul;
}

/* ── 兩顆共用按鈕 ──────────────────────────────
   注意：它們會呼叫頁面自己的函式：
     heartButton  → toggleHeart(btn)      （index / stores 有）
     removeButton → removeAndRefresh(id)  （profile 有）
   哪一頁用哪顆，就要有對應的那個函式。
   ────────────────────────────────────────────── */

//愛心收藏鈕：點擊切換收藏
function heartButton(c) {
  const heart = mk('button', 'heart-btn' + (c.liked ? ' liked' : ''), '♥');
  heart.dataset.id = c.id;
  heart.addEventListener('click', () => toggleHeart(heart));
  return heart;
}

//取消收藏鈕
function removeButton(c) {
  const btn = mk('button', 'heart-btn liked', '✖');
  btn.title = '取消收藏';
  btn.addEventListener('click', () => removeAndRefresh(c.id));
  return btn;
}

/*
 * 建立一張優惠券卡片，回傳 DOM 元素。
 *   c    一筆優惠券資料
 *   i    索引（給進場動畫錯開時間用）
 */
function buildCouponCard(c, i, opts = {}) {
  const {
    cardClass  = 'coupon-card',
    animMs     = '0.45s',
    stagger    = 0.055,         //每張卡延遲秒數
    priceField = 'discount',
    spendField = 'content',
    button     = heartButton,   // 預設就是愛心鈕，所以 index 不用傳 opts
  } = opts;

  const card = mk('div', cardClass + (c.type === 'buy1get1' ? ' buy-one' : ''));
  card.style.animation = `fadeUp ${animMs} ${i * stagger}s ease both`;

  const top = mk('div', 'coupon-top');

  if (c.type === 'price') {
    // price 版型：標題外面多包一層 div
    const titleWrap = document.createElement('div');
    const title = mk('div', 'coupon-title-sm');
    setMultiline(title, c.title || '');          // 標題可能有換行
    titleWrap.appendChild(title);
    top.appendChild(titleWrap);
    if (button) top.appendChild(button(c));
    card.appendChild(top);

    const priceRow = mk('div', 'coupon-price-row');
    priceRow.appendChild(mk('div', 'coupon-price', '$' + (c[priceField] ?? '')));
    priceRow.appendChild(mk('div', 'coupon-price-tag', c.priceTag || ''));
    card.appendChild(priceRow);

    card.appendChild(mk('div', 'coupon-spend', c[spendField] || ''));
  } else {
    // buy1get1 版型：標題直接放在 coupon-top
    top.appendChild(mk('div', 'coupon-title-sm', c.title || ''));
    if (button) top.appendChild(button(c));
    card.appendChild(top);

    card.appendChild(mk('div', 'big-deal', c.dealText || ''));
    card.appendChild(mk('div', 'deal-sub', c.dealSub || ''));
  }

  card.appendChild(mk('hr', 'coupon-sep'));

  const termsEl = buildTerms(c.terms);
  if (termsEl) card.appendChild(termsEl);

  // 到期文案獨立成一個框，放在 terms 下面（跟條款區隔開）
  // 3 天內到期（含今天）→ 加 urgent class 變紅字
  const exp = expiryText(c.endDate);
  if (exp) {
    const d = daysUntil(c.endDate);
    const urgent = d >= 0 && d <= 3 ? ' urgent' : '';
    card.appendChild(mk('div', 'coupon-expiry' + urgent, exp));
  }

  return card;
}