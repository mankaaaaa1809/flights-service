(() => {
  const box = document.getElementById('payBox'), st = document.getElementById('st');
  const method = box.dataset.method, open = box.dataset.open;
  const done = until => {
    st.innerHTML = `<span>✅ Оплата подтверждена${until ? ' — доступ до ' + until : ''}.</span> <a class="btn primary" href="${open}">Открыть услугу</a>`;
    clearInterval(timer);
  };
  async function poll() {
    try {
      const r = await fetch('/ui/status/' + encodeURIComponent(method), { headers: { Accept: 'application/json' } });
      if (!r.ok) return;
      const d = await r.json();
      if (d.paid) done(d.until);
    } catch { /* повторим на следующем тике */ }
  }
  const timer = setInterval(poll, 3000);
  if (box.dataset.paid === 'true') poll(); else setTimeout(poll, 500);
})();
