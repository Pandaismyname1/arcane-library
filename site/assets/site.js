// Arcane Library website: mobile menu, gallery filter and lightbox, roll calculator, TOC highlight.
(function () {
  const $ = (s, r = document) => r.querySelector(s);
  const $$ = (s, r = document) => Array.from(r.querySelectorAll(s));

  const btn = $('.menu-btn'), nav = $('.nav');
  if (btn) btn.addEventListener('click', () => {
    const open = nav.classList.toggle('open');
    btn.setAttribute('aria-expanded', open);
  });

  // fade-in on scroll (content stays visible without JS, and a timer shows everything if the observer never fires)
  document.documentElement.classList.add('js');
  setTimeout(() => $$('.reveal').forEach(el => el.classList.add('in')), 2500);
  if ('IntersectionObserver' in window) {
    const io = new IntersectionObserver(es => es.forEach(e => { if (e.isIntersecting) { e.target.classList.add('in'); io.unobserve(e.target); } }), { threshold: 0.08 });
    $$('.reveal').forEach(el => io.observe(el));
  } else $$('.reveal').forEach(el => el.classList.add('in'));

  // gallery filter
  const chips = $$('.chip[data-f]');
  chips.forEach(c => c.addEventListener('click', () => {
    chips.forEach(o => o.setAttribute('aria-pressed', o === c));
    $$('.gallery .shot').forEach(s => { s.hidden = c.dataset.f !== 'all' && s.dataset.cat !== c.dataset.f; });
  }));

  // lightbox
  const shots = $$('a.shot');
  if (shots.length) {
    const lb = document.createElement('div');
    lb.className = 'lb'; lb.setAttribute('role', 'dialog'); lb.setAttribute('aria-label', 'Image viewer');
    lb.innerHTML = '<button class="x" aria-label="Close">✕</button><button class="p" aria-label="Previous">‹</button><button class="n" aria-label="Next">›</button><img alt=""><div class="cap"></div>';
    document.body.appendChild(lb);
    const img = $('img', lb), cap = $('.cap', lb);
    let list = [], idx = 0;
    const show = i => {
      idx = (i + list.length) % list.length;
      const a = list[idx];
      img.src = a.href; img.alt = a.dataset.cap || '';
      cap.textContent = a.dataset.cap || '';
    };
    const close = () => lb.classList.remove('open');
    shots.forEach(a => a.addEventListener('click', e => {
      e.preventDefault();
      list = shots.filter(s => !s.hidden && s.closest('.gallery, .strip') === a.closest('.gallery, .strip'));
      show(list.indexOf(a));
      lb.classList.add('open');
    }));
    $('.x', lb).onclick = close;
    $('.p', lb).onclick = () => show(idx - 1);
    $('.n', lb).onclick = () => show(idx + 1);
    lb.addEventListener('click', e => { if (e.target === lb) close(); });
    document.addEventListener('keydown', e => {
      if (!lb.classList.contains('open')) return;
      if (e.key === 'Escape') close();
      if (e.key === 'ArrowLeft') show(idx - 1);
      if (e.key === 'ArrowRight') show(idx + 1);
    });
  }

  // roll calculator (mirrors logic/Roll.java and logic/Costs.java)
  const calc = $('#calc');
  if (calc) {
    const bonus = { 1: 0, 2: 8, 3: 16 }, refines = { 1: 1, 2: 2, 3: 3 };
    const power = (die, b, max) => {
      if (max <= 1) return 1;
      const score = Math.max(1, Math.min(100, die + b));
      return Math.max(1, Math.min(max, Math.ceil(score * max / 100)));
    };
    const run = () => {
      const max = +$('#c-max', calc).value, m = +$('#c-mast', calc).value;
      const quill = $('#c-quill', calc).checked, rare = $('#c-rare', calc).checked;
      const b = bonus[m] + (quill ? 6 : 0);
      const rolls = 1 + refines[m] + (quill ? 1 : 0);
      const hits = new Array(max + 2).fill(0);
      for (let d = 1; d <= 100; d++) hits[power(d, b, max)]++;
      const bars = $('.bars', calc); bars.innerHTML = '';
      for (let L = max; L >= 1; L--) {
        let single = 0; for (let k = L; k <= max; k++) single += hits[k] / 100;
        const best = 1 - Math.pow(1 - single, rolls);
        const row = document.createElement('div'); row.className = 'bar';
        row.innerHTML = '<span>Power ' + L + '+</span><div class="track"><i style="width:' + (best * 100).toFixed(1) + '%"></i></div><b>' + Math.round(best * 100) + '%</b>';
        bars.appendChild(row);
      }
      const inscribe = 2 + Math.floor(Math.max(1, max) / 2) + (rare ? 2 : 0);
      let refine = 0; for (let i = 0; i < rolls - 1; i++) refine += 2 + i;
      $('.out', calc).innerHTML = 'Chance of reaching at least each power with every refinement used (<b>' + rolls + ' rolls</b>, bonus <b>+' + b + '</b>). ' +
        'Inscribe costs <b>' + inscribe + ' dust</b>; using all <b>' + (rolls - 1) + ' refinements</b> costs <b>' + refine + ' more</b> (<b>' + (inscribe + refine) + ' in total</b>).';
    };
    $$('select, input', calc).forEach(i => i.addEventListener('input', run));
    run();
  }

  // TOC highlight
  const toc = $$('.toc a');
  if (toc.length && 'IntersectionObserver' in window) {
    const map = new Map(toc.map(a => [a.getAttribute('href').slice(1), a]));
    const io2 = new IntersectionObserver(es => es.forEach(e => {
      if (e.isIntersecting) { toc.forEach(a => a.classList.remove('on')); const a = map.get(e.target.id); if (a) a.classList.add('on'); }
    }), { rootMargin: '-20% 0px -70% 0px' });
    map.forEach((_, id) => { const el = document.getElementById(id); if (el) io2.observe(el); });
  }
})();
