(() => {
  const dlg = document.getElementById('userDlg'), btn = document.getElementById('userBtn');
  if (btn && dlg) {
    btn.onclick = () => dlg.showModal();
    dlg.addEventListener('click', e => { if (e.target === dlg) dlg.close(); }); // клик по фону закрывает окно
  }
  async function copy(text) {
    try { await navigator.clipboard.writeText(text); return true; }
    catch {
      const t = document.createElement('textarea'); t.value = text; document.body.appendChild(t); t.select();
      const ok = document.execCommand('copy'); t.remove(); return ok;
    }
  }
  document.addEventListener('click', async e => {
    const b = e.target.closest('[data-copy]'); if (!b) return;
    const old = b.dataset.label || (b.dataset.label = b.textContent);
    b.textContent = (await copy(b.dataset.copy)) ? 'Скопировано ✓' : 'Не удалось';
    setTimeout(() => b.textContent = old, 1500);
  });
})();
