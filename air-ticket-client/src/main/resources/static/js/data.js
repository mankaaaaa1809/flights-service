(() => {
  const root = document.getElementById('data'), method = root.dataset.method, payUrl = root.dataset.pay;
  const csrf = document.querySelector('meta[name=_csrf]').content, csrfHeader = document.querySelector('meta[name=_csrf_header]').content;
  const $ = s => document.querySelector(s);
  const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  const dt = s => s ? String(s).replace('T', ' ').slice(0, 16) : '';
  const STATUS = { CONFIRMED: 'Подтверждена', CANCELLED: 'Отменена' };
  /* Поля — как в API сервиса билетов */
  const CFG = {
    Flights: { empty: 'Рейсов нет',
      check: o => o.arrivalTime <= o.departureTime ? 'Прибытие должно быть позже вылета' : null,
      cols: [{ f: 'flightNumber', t: 'Рейс', mono: 1 }, { f: 'origin', t: 'Откуда' }, { f: 'destination', t: 'Куда' },
        { f: 'departureTime', t: 'Вылет', type: 'datetime-local' }, { f: 'arrivalTime', t: 'Прибытие', type: 'datetime-local' },
        { f: 'price', t: 'Цена', type: 'number', step: '0.01' }, { f: 'totalSeats', t: 'Всего мест', type: 'number' },
        { f: 'seatsAvailable', t: 'Свободно', ro: 1 }] },
    Bookings: { empty: 'Билетов нет',
      cols: [{ f: 'flightId', t: 'ID рейса', type: 'number' }, { f: 'flightNumber', t: 'Рейс', ro: 1, mono: 1 },
        { f: 'passengerName', t: 'Пассажир' }, { f: 'passportNumber', t: 'Паспорт', mono: 1 },
        { f: 'status', t: 'Статус', type: 'select', opts: Object.keys(STATUS) }, { f: 'createdAt', t: 'Создана', ro: 1, fmt: dt }] }
  };
  const R = CFG[method];
  const API = `/ui/api/${method.toLowerCase()}`;
  let rows = [];

  function toast(msg, err) { const t = $('#toast'); t.textContent = msg; t.className = err ? 'err' : ''; t.style.display = 'block'; clearTimeout(toast.h); toast.h = setTimeout(() => t.style.display = 'none', 5000); }
  async function api(path, opts = {}) {
    let r; try { r = await fetch(API + path, { headers: { 'Content-Type': 'application/json', [csrfHeader]: csrf }, ...opts }); } catch { throw new Error('Нет связи с сервером'); }
    const txt = await r.text(); let d = null; try { d = txt ? JSON.parse(txt) : null; } catch { d = txt; }
    if (!r.ok) throw Object.assign(new Error((d && (d.message || d.error)) || 'HTTP ' + r.status), { pay: r.status === 402 });
    return d;
  }
  const banner = msg => { $('#bn').innerHTML = `<div class="banner"><span>${esc(msg)}</span><a class="btn primary" href="${esc(payUrl)}">Оплатить услугу</a></div>`; };

  const field = (c, v) => {
    if (c.type === 'select') return `<select data-f="${c.f}">${c.opts.map(o => `<option value="${o}" ${o === v ? 'selected' : ''}>${esc(STATUS[o] || o)}</option>`).join('')}</select>`;
    const val = c.type === 'datetime-local' && v ? String(v).slice(0, 16) : (v ?? '');
    return `<input data-f="${c.f}" type="${c.type || 'text'}" ${c.step ? `step="${c.step}"` : ''} value="${esc(val)}">`;
  };
  const rowHtml = r => `<tr data-id="${r.id}" data-q="${esc(Object.values(r).join(' ').toLowerCase())}"><td class="mono muted">${r.id}</td>
    ${R.cols.map(c => `<td class="${c.mono ? 'mono' : ''}">${c.ro ? esc(c.fmt ? c.fmt(r[c.f]) : r[c.f]) : field(c, r[c.f])}</td>`).join('')}
    <td class="act"><button data-act="save" disabled>Сохранить</button><button class="danger" data-act="del">Удалить</button></td></tr>`;
  const newRow = () => `<tr class="new"><td class="mono muted">+</td>${R.cols.map(c => `<td>${c.ro ? '' : field(c, c.type === 'select' ? c.opts[0] : '')}</td>`).join('')}
    <td class="act"><button class="primary" data-act="add">Добавить</button></td></tr>`;

  function draw() {
    $('#tbl').innerHTML = `<table><thead><tr><th>ID</th>${R.cols.map(c => `<th>${c.t}</th>`).join('')}<th></th></tr></thead>
      <tbody>${newRow()}${rows.map(rowHtml).join('')}</tbody></table>${rows.length ? '' : `<div class="empty">${R.empty}</div>`}`;
    filter();
  }
  function filter() {
    const q = $('#flt').value.trim().toLowerCase();
    document.querySelectorAll('#tbl tr[data-id]').forEach(tr => tr.hidden = !!q && !tr.dataset.q.includes(q));
  }
  function collect(tr) {
    const o = {};
    for (const el of tr.querySelectorAll('[data-f]')) {
      let v = el.value;
      if (el.type === 'number') v = v === '' ? null : Number(v);
      if (el.type === 'datetime-local' && v.length === 16) v += ':00';
      o[el.dataset.f] = v;
    }
    return o;
  }
  async function load() {
    try { rows = await api(''); $('#bn').innerHTML = ''; draw(); }
    catch (e) { if (e.pay) banner(e.message); $('#tbl').innerHTML = `<div class="empty">${e.pay ? 'Данные недоступны без оплаты' : esc(e.message)}</div>`; }
  }

  const tbl = $('#tbl');
  const touched = e => { const tr = e.target.closest('tr[data-id]'); if (tr) { tr.classList.add('dirty'); tr.querySelector('[data-act=save]').disabled = false; } };
  tbl.addEventListener('input', touched); tbl.addEventListener('change', touched);
  tbl.addEventListener('click', async e => {
    const b = e.target.closest('[data-act]'); if (!b) return;
    const tr = b.closest('tr'), id = tr.dataset.id;
    try {
      if (b.dataset.act === 'del') {
        if (!confirm('Удалить запись ' + id + '?')) return;
        await api('/' + id, { method: 'DELETE' }); toast('Удалено');
      } else {
        const o = collect(tr), bad = R.check && R.check(o); if (bad) return toast(bad, true);
        if (b.dataset.act === 'save') { await api('/' + id, { method: 'PUT', body: JSON.stringify(o) }); toast('Сохранено'); }
        else { await api('', { method: 'POST', body: JSON.stringify(o) }); toast('Добавлено'); }
      }
      load();
    } catch (err) { if (err.pay) banner(err.message); toast(err.message, true); }
  });
  $('#flt').oninput = filter; $('#reload').onclick = load;
  load();
})();
