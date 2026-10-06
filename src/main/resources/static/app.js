const API_BASE = '';
const TOKEN_KEY = 'kiosk_admin_jwt';
const USER_KEY = 'kiosk_admin_username';

function getToken() { return localStorage.getItem(TOKEN_KEY); }
function setSession(token, username) {
  localStorage.setItem(TOKEN_KEY, token);
  localStorage.setItem(USER_KEY, username);
}
function clearSession() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}

async function apiFetch(path, options = {}) {
  const headers = options.headers || {};
  headers['Content-Type'] = 'application/json';
  const token = getToken();
  if (token) headers['Authorization'] = 'Bearer ' + token;
  const res = await fetch(API_BASE + path, { ...options, headers });
  if (res.status === 401) {
    clearSession();
    showLogin();
    throw new Error('Phien dang nhap het han, vui long dang nhap lai');
  }
  const body = await res.json().catch(() => ({}));
  if (!res.ok || body.success === false) {
    throw new Error(body.message || ('Loi ' + res.status));
  }
  return body.data;
}

function showLogin() {
  document.getElementById('loginView').style.display = 'flex';
  document.getElementById('appView').style.display = 'none';
}
function showApp() {
  document.getElementById('loginView').style.display = 'none';
  document.getElementById('appView').style.display = 'block';
  document.getElementById('whoami').textContent = localStorage.getItem(USER_KEY) || '';
  loadDevices();
  loadAds();
}

document.addEventListener('DOMContentLoaded', () => {
  if (getToken()) {
    showApp();
  } else {
    showLogin();
  }

  document.getElementById('loginForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const errEl = document.getElementById('loginError');
    errEl.style.display = 'none';
    const username = document.getElementById('username').value.trim();
    const password = document.getElementById('password').value;
    try {
      const res = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
      });
      const body = await res.json();
      if (!res.ok || body.success === false) {
        throw new Error(body.message || 'Dang nhap that bai');
      }
      setSession(body.data.token, body.data.username);
      showApp();
    } catch (err) {
      errEl.textContent = err.message;
      errEl.style.display = 'block';
    }
  });

  document.getElementById('logoutBtn').addEventListener('click', () => {
    clearSession();
    showLogin();
  });

  document.querySelectorAll('.tab-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
      document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
      btn.classList.add('active');
      document.getElementById('tab-' + btn.dataset.tab).classList.add('active');
    });
  });

  // Devices
  document.getElementById('refreshDevicesBtn').addEventListener('click', loadDevices);
  document.getElementById('newDeviceBtn').addEventListener('click', () => {
    document.getElementById('dcCode').value = '';
    document.getElementById('dcStoreId').value = '';
    document.getElementById('dcStoreName').value = '';
    document.getElementById('dcNote').value = '';
    document.getElementById('newDeviceModal').style.display = 'flex';
  });
  document.getElementById('dcCancel').addEventListener('click', () => {
    document.getElementById('newDeviceModal').style.display = 'none';
  });
  document.getElementById('dcSubmit').addEventListener('click', async () => {
    const deviceCode = document.getElementById('dcCode').value.trim();
    if (!deviceCode) { alert('Vui long nhap ma may'); return; }
    const storeIdRaw = document.getElementById('dcStoreId').value.trim();
    try {
      const data = await apiFetch('/api/admin/devices', {
        method: 'POST',
        body: JSON.stringify({
          deviceCode,
          storeId: storeIdRaw ? Number(storeIdRaw) : null,
          storeName: document.getElementById('dcStoreName').value.trim() || null,
          note: document.getElementById('dcNote').value.trim() || null
        })
      });
      document.getElementById('newDeviceModal').style.display = 'none';
      showActivationModal(data);
      loadDevices();
    } catch (err) {
      alert(err.message);
    }
  });
  document.getElementById('actClose').addEventListener('click', () => {
    document.getElementById('activationModal').style.display = 'none';
  });

  // Ads
  document.getElementById('refreshAdsBtn').addEventListener('click', loadAds);
  document.getElementById('newAdBtn').addEventListener('click', () => openAdModal(null));
  document.getElementById('adCancel').addEventListener('click', () => {
    document.getElementById('adModal').style.display = 'none';
  });
  document.getElementById('adSubmit').addEventListener('click', submitAd);
});

function showActivationModal(data) {
  document.getElementById('actDeviceCode').textContent = data.deviceCode;
  document.getElementById('actCode').textContent = data.activationCode;
  document.getElementById('actExpires').textContent = new Date(data.expiresAt).toLocaleString('vi-VN');
  document.getElementById('actQrPayload').value = data.qrPayload || '';
  document.getElementById('activationModal').style.display = 'flex';
}

async function loadDevices() {
  const tbody = document.getElementById('devicesTableBody');
  tbody.innerHTML = '<tr><td colspan="7">Dang tai...</td></tr>';
  try {
    const devices = await apiFetch('/api/admin/devices');
    if (!devices.length) {
      tbody.innerHTML = '<tr><td colspan="7">Chua co may nao</td></tr>';
      return;
    }
    tbody.innerHTML = devices.map(renderDeviceRow).join('');
    tbody.querySelectorAll('button[data-action]').forEach(btn => {
      btn.addEventListener('click', () => handleDeviceAction(btn.dataset.action, btn.dataset.code));
    });
  } catch (err) {
    tbody.innerHTML = '<tr><td colspan="7">Loi: ' + escapeHtml(err.message) + '</td></tr>';
  }
}

function renderDeviceRow(d) {
  const created = d.createdAt ? new Date(d.createdAt).toLocaleString('vi-VN') : '-';
  const activated = d.activatedAt ? new Date(d.activatedAt).toLocaleString('vi-VN') : '-';
  let actions = '';
  if (d.status === 'PENDING') {
    actions += `<button class="btn-secondary btn-small" data-action="reissue" data-code="${d.deviceCode}">Cap lai ma</button>`;
  }
  if (d.status !== 'REVOKED') {
    actions += `<button class="btn-secondary btn-small" data-action="rotate" data-code="${d.deviceCode}">Ep xoay khoa</button>`;
    actions += `<button class="btn-danger btn-small" data-action="revoke" data-code="${d.deviceCode}">Thu hoi</button>`;
  }
  actions += `<button class="btn-danger btn-small" data-action="delete" data-code="${d.deviceCode}">Xoa</button>`;
  return `<tr>
    <td>${escapeHtml(d.deviceCode)}</td>
    <td>${escapeHtml(d.storeName || '-')}</td>
    <td><span class="status-pill status-${d.status}">${d.status}</span>${d.forceKeyRotation ? ' <small>(cho xoay khoa)</small>' : ''}</td>
    <td>${escapeHtml(d.note || '-')}</td>
    <td>${created}</td>
    <td>${activated}</td>
    <td>${actions}</td>
  </tr>`;
}

async function handleDeviceAction(action, code) {
  try {
    if (action === 'reissue') {
      const data = await apiFetch(`/api/admin/devices/${encodeURIComponent(code)}/activation-code`, { method: 'POST' });
      showActivationModal(data);
    } else if (action === 'rotate') {
      if (!confirm('Ep xoay khoa cho may ' + code + '?')) return;
      await apiFetch(`/api/admin/devices/${encodeURIComponent(code)}/force-key-rotation`, { method: 'POST' });
    } else if (action === 'revoke') {
      if (!confirm('Thu hoi may ' + code + '? May se bi tu choi o lan goi API tiep theo.')) return;
      await apiFetch(`/api/admin/devices/${encodeURIComponent(code)}/revoke`, { method: 'POST' });
    } else if (action === 'delete') {
      if (!confirm('Xoa han may ' + code + ' va cac ma kich hoat lien quan? Khong the hoan tac.')) return;
      await apiFetch(`/api/admin/devices/${encodeURIComponent(code)}`, { method: 'DELETE' });
    }
    loadDevices();
  } catch (err) {
    alert(err.message);
  }
}

async function loadAds() {
  const tbody = document.getElementById('adsTableBody');
  tbody.innerHTML = '<tr><td colspan="7">Dang tai...</td></tr>';
  try {
    const ads = await apiFetch('/api/admin/ads');
    if (!ads.length) {
      tbody.innerHTML = '<tr><td colspan="7">Chua co quang cao nao</td></tr>';
      return;
    }
    tbody.innerHTML = ads.map(renderAdRow).join('');
    tbody.querySelectorAll('button[data-action]').forEach(btn => {
      btn.addEventListener('click', () => handleAdAction(btn.dataset.action, btn.dataset.id, ads));
    });
  } catch (err) {
    tbody.innerHTML = '<tr><td colspan="7">Loi: ' + escapeHtml(err.message) + '</td></tr>';
  }
}

function renderAdRow(a) {
  return `<tr>
    <td>${a.displayOrder}</td>
    <td>${escapeHtml(a.name || '-')}</td>
    <td>${escapeHtml(a.mediaType)}</td>
    <td>${escapeHtml(a.actionType)}</td>
    <td><span class="status-pill status-${a.status}">${a.status}</span></td>
    <td style="max-width:220px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;">${escapeHtml(a.url)}</td>
    <td>
      <button class="btn-secondary btn-small" data-action="toggle" data-id="${a.id}">${a.status === 'ON' ? 'Tat' : 'Bat'}</button>
      <button class="btn-secondary btn-small" data-action="edit" data-id="${a.id}">Sua</button>
      <button class="btn-danger btn-small" data-action="delete" data-id="${a.id}">Xoa</button>
    </td>
  </tr>`;
}

async function handleAdAction(action, id, ads) {
  try {
    if (action === 'toggle') {
      await apiFetch(`/api/admin/ads/${encodeURIComponent(id)}/toggle`, { method: 'POST' });
      loadAds();
    } else if (action === 'delete') {
      if (!confirm('Xoa quang cao nay?')) return;
      await apiFetch(`/api/admin/ads/${encodeURIComponent(id)}`, { method: 'DELETE' });
      loadAds();
    } else if (action === 'edit') {
      const ad = ads.find(x => x.id === id);
      openAdModal(ad);
    }
  } catch (err) {
    alert(err.message);
  }
}

function openAdModal(ad) {
  document.getElementById('adModalTitle').textContent = ad ? 'Sua quang cao' : 'Them quang cao';
  document.getElementById('adId').value = ad ? ad.id : '';
  document.getElementById('adName').value = ad ? (ad.name || '') : '';
  document.getElementById('adMediaType').value = ad ? ad.mediaType : 'IMAGE';
  document.getElementById('adUrl').value = ad ? ad.url : '';
  document.getElementById('adActionType').value = ad ? ad.actionType : 'NONE';
  document.getElementById('adProductId').value = ad ? (ad.productId || '') : '';
  document.getElementById('adCategoryId').value = ad ? (ad.categoryId || '') : '';
  document.getElementById('adWebsiteUrl').value = ad ? (ad.websiteUrl || '') : '';
  document.getElementById('adDisplayOrder').value = ad ? ad.displayOrder : 0;
  document.getElementById('adModal').style.display = 'flex';
}

async function submitAd() {
  const id = document.getElementById('adId').value;
  const payload = {
    name: document.getElementById('adName').value.trim() || null,
    mediaType: document.getElementById('adMediaType').value.trim(),
    url: document.getElementById('adUrl').value.trim(),
    actionType: document.getElementById('adActionType').value.trim(),
    productId: document.getElementById('adProductId').value.trim() || null,
    categoryId: document.getElementById('adCategoryId').value.trim() || null,
    websiteUrl: document.getElementById('adWebsiteUrl').value.trim() || null,
    displayOrder: Number(document.getElementById('adDisplayOrder').value)
  };
  try {
    if (id) {
      await apiFetch(`/api/admin/ads/${encodeURIComponent(id)}`, { method: 'PUT', body: JSON.stringify(payload) });
    } else {
      await apiFetch('/api/admin/ads', { method: 'POST', body: JSON.stringify(payload) });
    }
    document.getElementById('adModal').style.display = 'none';
    loadAds();
  } catch (err) {
    alert(err.message);
  }
}

function escapeHtml(str) {
  if (str === null || str === undefined) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}
