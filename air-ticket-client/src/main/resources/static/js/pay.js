(() => {
  const pm = document.getElementById('pm'), from = document.getElementById('pfrom'), to = document.getElementById('pto');
  const sum = () => { const o = pm.selectedOptions[0]; document.getElementById('psum').textContent = o ? (o.dataset.price || 0) : '—'; };
  const pad = n => String(n).padStart(2, '0');
  const fmt = d => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
  pm.onchange = sum; sum();
  document.getElementById('presets').onclick = e => {
    const b = e.target.closest('[data-h]'); if (!b || !from.value) return;
    to.value = fmt(new Date(new Date(from.value).getTime() + (+b.dataset.h) * 36e5));
  };
})();
