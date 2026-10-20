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
    throw new Error('Phiên đăng nhập hết hạn, vui lòng đăng nhập lại');
  }
  const body = await res.json().catch(() => ({}));
  if (!res.ok || body.success === false) {
    throw new Error(body.message || ('Lỗi ' + res.status));
  }
  return body.data;
}

function showLogin() {
  document.getElementById('loginView').style.display = 'flex';
  document.getElementById('appView').style.display = 'none';
}
function showApp() {
  document.getElementById('loginView').style.display = 'none';
  document.getElementById('appView').style.display = 'flex';
  const username = localStorage.getItem(USER_KEY) || '';
  document.getElementById('whoami').textContent = username;
  document.getElementById('whoamiAvatar').textContent = username.charAt(0) || '?';
  loadDevices();
  loadAds();
  loadOverview();
}

const PAGE_TITLES = {
  overview: ['Tổng quan', 'Trạng thái hệ thống máy kiosk và sản phẩm'],
  devices: ['Máy Kiosk', 'Quản lý thiết bị, mã kích hoạt và trạng thái hoạt động'],
  ads: ['Quảng cáo', 'Nội dung phát trên màn hình chờ của máy kiosk'],
  products: ['Sản phẩm', 'Danh mục, sản phẩm, giá và tồn kho cho kênh KIOSK'],
  categories: ['Nhóm sản phẩm', 'Các tab nhóm sản phẩm hiện trên App: tên, thứ tự, ẩn/hiện'],
  banners: ['Banner', 'Ảnh banner trên màn danh sách sản phẩm của App'],
  stores: ['Cửa hàng', 'Cửa hàng khách chọn để nhận hàng trên App'],
  limits: ['Hạn mức giao dịch', 'Số lượng tối đa mỗi sản phẩm trong một đơn'],
  config: ['Cấu hình kênh', 'Giờ bán, hotline, thời gian chờ, tạm dừng bán, phiên bản app tối thiểu'],
  errorlogs: ['Log lỗi thiết bị', 'Lỗi mà các máy kiosk báo về'],
  auditlogs: ['Audit log', 'Nhật ký thao tác của quản trị viên'],
  txlogs: ['Log giao dịch/giá', 'Nhật ký gọi API và cập nhật giá']
};

function setPageTitle(tab) {
  const t = PAGE_TITLES[tab];
  if (!t) return;
  document.getElementById('pageTitle').textContent = t[0];
  document.getElementById('pageSubtitle').textContent = t[1];
}

// Trang hien tai (0-based) cho cac tab log phan trang don gian.
const pageState = {
  errorlogs: 0,
  auditlogs: 0,
  txlogs: 0,
  pricelogs: 0,
  stores: 0
};
const PAGE_SIZE = 50;

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
        throw new Error(body.message || 'Đăng nhập thất bại');
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

  document.querySelectorAll('.nav-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('.nav-btn').forEach(b => b.classList.remove('active'));
      document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
      btn.classList.add('active');
      document.getElementById('tab-' + btn.dataset.tab).classList.add('active');
      setPageTitle(btn.dataset.tab);
      onTabActivated(btn.dataset.tab);
    });
  });

  // Dong modal bang nut X o header, hoac bam ra ngoai modal (backdrop).
  document.querySelectorAll('.modal-x').forEach(btn => {
    btn.addEventListener('click', () => {
      document.getElementById(btn.dataset.close).style.display = 'none';
    });
  });
  document.querySelectorAll('.modal-backdrop').forEach(backdrop => {
    backdrop.addEventListener('click', (e) => {
      if (e.target === backdrop) backdrop.style.display = 'none';
    });
  });

  // Devices
  document.getElementById('refreshDevicesBtn').addEventListener('click', loadDevices);
  document.getElementById('newDeviceBtn').addEventListener('click', async () => {
    document.getElementById('dcCode').value = '';
    await fillStoreSelect(document.getElementById('dcStore'), null);
    document.getElementById('newDeviceModal').style.display = 'flex';
  });
  document.getElementById('dcCancel').addEventListener('click', () => {
    document.getElementById('newDeviceModal').style.display = 'none';
  });
  document.getElementById('dcSubmit').addEventListener('click', async () => {
    const deviceCode = document.getElementById('dcCode').value.trim();
    if (!deviceCode) { alert('Vui lòng nhập mã máy'); return; }
    const store = selectedStore(document.getElementById('dcStore'));
    if (!store) { alert('Vui lòng chọn cửa hàng'); return; }
    try {
      const data = await apiFetch('/api/admin/devices', {
        method: 'POST',
        body: JSON.stringify({ deviceCode, storeId: store.id, storeName: store.name })
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
  document.getElementById('adActionType').addEventListener('change', syncAdActionRows);

  // Doi cua hang cua may
  document.getElementById('deviceStoreCancel').addEventListener('click', () => {
    document.getElementById('deviceStoreModal').style.display = 'none';
  });
  document.getElementById('deviceStoreSubmit').addEventListener('click', submitDeviceStore);

  // Nhom san pham (tab rieng)
  document.getElementById('refreshCategoriesBtn').addEventListener('click', loadCategoriesTab);
  document.getElementById('newCategoryBtn2').addEventListener('click', () => openCategoryModal(null));
  document.getElementById('saveCategoryOrderBtn').addEventListener('click', saveCategoryOrder);

  // Banner
  document.getElementById('refreshBannersBtn').addEventListener('click', loadBanners);
  document.getElementById('newBannerBtn').addEventListener('click', () => openBannerModal(null));
  document.getElementById('bannerCancel').addEventListener('click', () => {
    document.getElementById('bannerModal').style.display = 'none';
  });
  document.getElementById('bannerSubmit').addEventListener('click', submitBanner);

  // Cua hang
  document.getElementById('refreshStoresBtn').addEventListener('click', () => { pageState.stores = 0; loadStores(); });
  document.getElementById('storeSearch').addEventListener('keydown', (e) => {
    if (e.key === 'Enter') { pageState.stores = 0; loadStores(); }
  });
  document.getElementById('storesPrevBtn').addEventListener('click', () => {
    if (pageState.stores > 0) { pageState.stores--; loadStores(); }
  });
  document.getElementById('storesNextBtn').addEventListener('click', () => { pageState.stores++; loadStores(); });

  // Han muc
  document.getElementById('refreshLimitsBtn').addEventListener('click', loadLimits);
  document.getElementById('limitCancel').addEventListener('click', () => {
    document.getElementById('limitModal').style.display = 'none';
  });
  document.getElementById('limitSubmit').addEventListener('click', submitLimit);

  // Cau hinh kenh
  document.getElementById('refreshConfigBtn').addEventListener('click', loadConfig);
  document.getElementById('saveConfigBtn').addEventListener('click', saveConfig);

  // Products
  document.getElementById('refreshProductsBtn').addEventListener('click', loadProducts);
  document.getElementById('newProductBtn').addEventListener('click', () => openProductModal(null));
  document.getElementById('productCancel').addEventListener('click', () => {
    document.getElementById('productModal').style.display = 'none';
  });
  document.getElementById('productSubmit').addEventListener('click', submitProduct);

  // Nhom san pham (category)
  document.getElementById('newCategoryBtn').addEventListener('click', () => openCategoryModal(null));
  document.getElementById('categoryCancel').addEventListener('click', () => {
    document.getElementById('categoryModal').style.display = 'none';
  });
  document.getElementById('categorySubmit').addEventListener('click', submitCategory);

  // Gia
  document.getElementById('priceCancel').addEventListener('click', () => {
    document.getElementById('priceModal').style.display = 'none';
  });
  document.getElementById('priceSubmit').addEventListener('click', submitPrice);

  // Ton kho
  document.getElementById('inventoryCancel').addEventListener('click', () => {
    document.getElementById('inventoryModal').style.display = 'none';
  });
  document.getElementById('inventorySubmit').addEventListener('click', submitInventory);

  // Device error logs
  document.getElementById('refreshErrorLogsBtn').addEventListener('click', () => {
    pageState.errorlogs = 0;
    loadErrorLogs();
  });
  document.getElementById('errorLogsPrevBtn').addEventListener('click', () => {
    if (pageState.errorlogs > 0) { pageState.errorlogs--; loadErrorLogs(); }
  });
  document.getElementById('errorLogsNextBtn').addEventListener('click', () => {
    pageState.errorlogs++; loadErrorLogs();
  });

  // Audit logs
  document.getElementById('refreshAuditLogsBtn').addEventListener('click', () => {
    pageState.auditlogs = 0;
    loadAuditLogs();
  });
  document.getElementById('auditLogsPrevBtn').addEventListener('click', () => {
    if (pageState.auditlogs > 0) { pageState.auditlogs--; loadAuditLogs(); }
  });
  document.getElementById('auditLogsNextBtn').addEventListener('click', () => {
    pageState.auditlogs++; loadAuditLogs();
  });

  // Transaction / price logs
  document.getElementById('refreshTxLogsBtn').addEventListener('click', () => {
    pageState.txlogs = 0;
    loadTxLogs();
  });
  document.getElementById('txLogsPrevBtn').addEventListener('click', () => {
    if (pageState.txlogs > 0) { pageState.txlogs--; loadTxLogs(); }
  });
  document.getElementById('txLogsNextBtn').addEventListener('click', () => {
    pageState.txlogs++; loadTxLogs();
  });
  document.getElementById('refreshPriceLogsBtn').addEventListener('click', () => {
    pageState.pricelogs = 0;
    loadPriceLogs();
  });
  document.getElementById('priceLogsPrevBtn').addEventListener('click', () => {
    if (pageState.pricelogs > 0) { pageState.pricelogs--; loadPriceLogs(); }
  });
  document.getElementById('priceLogsNextBtn').addEventListener('click', () => {
    pageState.pricelogs++; loadPriceLogs();
  });
});

// Cac tab log/san pham chi tai du lieu khi nguoi dung bam vao (lazy load) de khong goi API
// thua luc moi dang nhap.
function onTabActivated(tab) {
  if (tab === 'overview') {
    loadOverview();
  } else if (tab === 'products') {
    loadCategoriesForDropdown();
    loadProducts();
  } else if (tab === 'categories') {
    loadCategoriesTab();
  } else if (tab === 'banners') {
    loadBanners();
  } else if (tab === 'stores') {
    loadStores();
  } else if (tab === 'limits') {
    loadLimits();
  } else if (tab === 'config') {
    loadConfig();
  } else if (tab === 'errorlogs') {
    loadErrorLogs();
  } else if (tab === 'auditlogs') {
    loadAuditLogs();
  } else if (tab === 'txlogs') {
    loadTxLogs();
    loadPriceLogs();
  }
}

function showActivationModal(data) {
  document.getElementById('actDeviceCode').textContent = data.deviceCode;
  document.getElementById('actCode').textContent = data.activationCode;
  document.getElementById('actExpires').textContent = data.expiresAt ? new Date(data.expiresAt).toLocaleString('vi-VN') : '-';
  document.getElementById('actQrPayload').value = data.qrPayload || '';
  document.getElementById('activationModal').style.display = 'flex';
}

// ---------- Tong quan (dashboard overview) ----------

async function loadOverview() {
  const setStat = (id, val) => { document.getElementById(id).textContent = val; };
  try {
    const [devices, ads, productsPage, auditPage] = await Promise.all([
      apiFetch('/api/admin/devices').catch(() => []),
      apiFetch('/api/admin/ads').catch(() => []),
      apiFetch('/api/admin/products?size=100').catch(() => ({ content: [] })),
      apiFetch('/api/admin/audit-logs?page=0&size=6').catch(() => ({ content: [] }))
    ]);

    setStat('statDevicesTotal', devices.length);
    // "Online ngay bay gio" = may dang mo ket noi SSE toi kiosk-api, KHAC voi status=ACTIVE (chi la
    // da kich hoat/chua bi thu hoi, khong tu het han).
    setStat('statDevicesActive', devices.filter(d => d.online).length);
    setStat('statDevicesPending', devices.filter(d => d.status === 'PENDING').length);
    setStat('statDevicesRevoked', devices.filter(d => d.status === 'REVOKED').length);
    setStat('statAdsOn', ads.filter(a => a.enabled).length);
    const products = productsPage.content || [];
    setStat('statProductsVisible', products.filter(p => !p.kioskInvisible).length);

    const recentDevices = [...devices]
      .sort((a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0))
      .slice(0, 6);
    const devicesEl = document.getElementById('overviewRecentDevices');
    devicesEl.innerHTML = recentDevices.length
      ? '<ul class="mini-list">' + recentDevices.map(d => `<li>
          <span class="mini-primary">${escapeHtml(d.deviceCode)}${d.storeName ? ' &middot; ' + escapeHtml(d.storeName) : ''}</span>
          <span class="online-dot ${d.online ? 'is-online' : 'is-offline'}">${d.online ? 'Online' : 'Offline'}</span>
        </li>`).join('') + '</ul>'
      : '<p class="empty-hint">Chưa có máy nào được tạo.</p>';

    const auditRows = auditPage.content || [];
    const auditEl = document.getElementById('overviewRecentAudit');
    auditEl.innerHTML = auditRows.length
      ? '<ul class="mini-list">' + auditRows.map(a => `<li>
          <span class="mini-primary">${escapeHtml(a.username || '-')} &rarr; ${escapeHtml(a.action || '-')}</span>
          <span class="mini-secondary">${a.createdAt ? new Date(a.createdAt).toLocaleString('vi-VN') : '-'}</span>
        </li>`).join('') + '</ul>'
      : '<p class="empty-hint">Chưa có hoạt động nào được ghi nhận.</p>';
  } catch (err) {
    console.error('loadOverview failed', err);
  }
}

let devicesCache = [];
let currentStoreDeviceCode = null;
let storesForSelect = null;

// Danh sach cua hang (kenh KIOSK) cho dropdown chon cua hang dat may - tai 1 lan roi cache.
async function fillStoreSelect(select, selectedId) {
  if (!storesForSelect) {
    try {
      const page = await apiFetch('/api/admin/stores?page=0&size=200');
      storesForSelect = page.data || [];
    } catch (err) {
      alert('Không tải được danh sách cửa hàng: ' + err.message);
      storesForSelect = null;
      select.innerHTML = '';
      return;
    }
  }
  select.innerHTML = '<option value="">-- Chọn cửa hàng --</option>' + storesForSelect
    .map(s => `<option value="${s.id}">${escapeHtml(s.name)} (#${s.id})</option>`).join('');
  select.value = selectedId != null ? String(selectedId) : '';
}

function selectedStore(select) {
  if (!select.value || !storesForSelect) return null;
  return storesForSelect.find(s => String(s.id) === select.value) || null;
}

async function submitDeviceStore() {
  const store = selectedStore(document.getElementById('deviceStoreSelect'));
  if (!store) { alert('Vui lòng chọn cửa hàng'); return; }
  try {
    await apiFetch(`/api/admin/devices/${encodeURIComponent(currentStoreDeviceCode)}`, {
      method: 'PUT', body: JSON.stringify({ storeId: store.id, storeName: store.name })
    });
    document.getElementById('deviceStoreModal').style.display = 'none';
    loadDevices();
  } catch (err) {
    alert(err.message);
  }
}

async function loadDevices() {
  const tbody = document.getElementById('devicesTableBody');
  tbody.innerHTML = '<tr><td colspan="8">Đang tải...</td></tr>';
  try {
    const devices = await apiFetch('/api/admin/devices');
    devicesCache = devices;
    if (!devices.length) {
      tbody.innerHTML = '<tr><td colspan="8">Chưa có máy nào</td></tr>';
      return;
    }
    tbody.innerHTML = devices.map(renderDeviceRow).join('');
    tbody.querySelectorAll('button[data-action]').forEach(btn => {
      btn.addEventListener('click', () => handleDeviceAction(btn.dataset.action, btn.dataset.code));
    });
  } catch (err) {
    tbody.innerHTML = '<tr><td colspan="8">Lỗi: ' + escapeHtml(err.message) + '</td></tr>';
  }
}

function renderDeviceRow(d) {
  const created = d.createdAt ? new Date(d.createdAt).toLocaleString('vi-VN') : '-';
  const activated = d.activatedAt ? new Date(d.activatedAt).toLocaleString('vi-VN') : '-';
  let actions = '';
  if (d.status === 'PENDING') {
    actions += `<button class="btn-secondary btn-small" data-action="reissue" data-code="${d.deviceCode}">Cấp lại mã</button>`;
  }
  if (d.status !== 'REVOKED') {
    actions += `<button class="btn-secondary btn-small" data-action="store" data-code="${escapeHtml(d.deviceCode)}">Đổi cửa hàng</button>`;
  }
  if (d.status === 'ACTIVE') {
    actions += `<button class="btn-secondary btn-small" data-action="rotate" data-code="${escapeHtml(d.deviceCode)}">Ép xoay khóa</button>`;
  }
  if (d.status !== 'REVOKED') {
    actions += `<button class="btn-danger btn-small" data-action="revoke" data-code="${escapeHtml(d.deviceCode)}">Thu hồi</button>`;
  }
  actions += `<button class="btn-danger btn-small" data-action="delete" data-code="${escapeHtml(d.deviceCode)}">Xóa</button>`;
  const onlineLabel = d.online
    ? '<span class="online-dot is-online">Online</span>'
    : '<span class="online-dot is-offline">Offline</span>';
  const deviceInfo = [d.deviceModel, d.osVersion ? 'Android ' + d.osVersion : null, d.appVersion ? 'app ' + d.appVersion : null]
    .filter(Boolean).join(' · ');
  return `<tr>
    <td>${escapeHtml(d.deviceCode)}</td>
    <td>${escapeHtml(d.storeName || '-')}${d.storeId ? ' <small>(#' + d.storeId + ')</small>' : ''}</td>
    <td><span class="status-pill status-${d.status}">${d.status}</span>${d.forceKeyRotation ? ' <small>(chờ xoay khóa)</small>' : ''}</td>
    <td>${onlineLabel}</td>
    <td>${escapeHtml(deviceInfo || '-')}</td>
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
    } else if (action === 'store') {
      const device = devicesCache.find(x => x.deviceCode === code);
      currentStoreDeviceCode = code;
      document.getElementById('deviceStoreCode').textContent = code;
      await fillStoreSelect(document.getElementById('deviceStoreSelect'), device ? device.storeId : null);
      document.getElementById('deviceStoreModal').style.display = 'flex';
      return;
    } else if (action === 'rotate') {
      if (!confirm('Ép xoay khóa cho máy ' + code + '?')) return;
      await apiFetch(`/api/admin/devices/${encodeURIComponent(code)}/force-key-rotation`, { method: 'POST' });
    } else if (action === 'revoke') {
      if (!confirm('Thu hồi máy ' + code + '? Máy sẽ bị từ chối ở lần gọi API tiếp theo.')) return;
      await apiFetch(`/api/admin/devices/${encodeURIComponent(code)}/revoke`, { method: 'POST' });
    } else if (action === 'delete') {
      if (!confirm('Xóa hẳn máy ' + code + ' và các mã kích hoạt liên quan? Không thể hoàn tác.')) return;
      await apiFetch(`/api/admin/devices/${encodeURIComponent(code)}`, { method: 'DELETE' });
    }
    loadDevices();
  } catch (err) {
    alert(err.message);
  }
}

let adsCache = [];

async function loadAds() {
  const tbody = document.getElementById('adsTableBody');
  tbody.innerHTML = '<tr><td colspan="9">Đang tải...</td></tr>';
  try {
    const ads = await apiFetch('/api/admin/ads');
    adsCache = ads;
    if (!ads.length) {
      tbody.innerHTML = '<tr><td colspan="9">Chưa có quảng cáo nào</td></tr>';
      return;
    }
    tbody.innerHTML = ads.map(renderAdRow).join('');
    tbody.querySelectorAll('button[data-action]').forEach(btn => {
      btn.addEventListener('click', () => handleAdAction(btn.dataset.action, btn.dataset.id, ads));
    });
  } catch (err) {
    tbody.innerHTML = '<tr><td colspan="9">Lỗi: ' + escapeHtml(err.message) + '</td></tr>';
  }
}

function formatBytes(n) {
  if (n == null) return '-';
  if (n >= 1024 * 1024) return (n / 1024 / 1024).toFixed(1) + ' MB';
  return Math.round(n / 1024) + ' KB';
}

function describeAdAction(a) {
  if (a.actionType === 'PRODUCT') return 'Mở SP ' + (a.productId || '');
  if (a.actionType === 'CATEGORY') return 'Mở nhóm ' + (a.categoryId || '');
  if (a.actionType === 'WEBSITE') return 'Mở web ' + (a.websiteUrl || '');
  return 'Không làm gì';
}

function renderAdRow(a) {
  const preview = a.mediaType === 'IMAGE'
    ? `<img class="thumb" src="${escapeHtml(a.url)}" alt="">`
    : `<a href="${escapeHtml(a.url)}" target="_blank" rel="noopener">Xem video</a>`;
  const status = a.enabled ? 'ON' : 'OFF';
  return `<tr>
    <td>${a.order}</td>
    <td>${preview}</td>
    <td>${escapeHtml(a.name || '-')}</td>
    <td>${escapeHtml(a.mediaType)}</td>
    <td class="cell-clip">${escapeHtml(describeAdAction(a))}</td>
    <td>${a.displaySeconds}s</td>
    <td>${formatBytes(a.sizeBytes)}</td>
    <td><span class="status-pill status-${status}">${a.enabled ? 'Đang bật' : 'Đang tắt'}</span></td>
    <td>
      <button class="btn-secondary btn-small" data-action="toggle" data-id="${escapeHtml(a.id)}">${a.enabled ? 'Tắt' : 'Bật'}</button>
      <button class="btn-secondary btn-small" data-action="edit" data-id="${escapeHtml(a.id)}">Sửa</button>
      <button class="btn-danger btn-small" data-action="delete" data-id="${escapeHtml(a.id)}">Xóa</button>
    </td>
  </tr>`;
}

async function handleAdAction(action, id, ads) {
  try {
    if (action === 'toggle') {
      await apiFetch(`/api/admin/ads/${encodeURIComponent(id)}/toggle`, { method: 'POST' });
      loadAds();
    } else if (action === 'delete') {
      if (!confirm('Xóa quảng cáo này?')) return;
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

function syncAdActionRows() {
  const action = document.getElementById('adActionType').value;
  document.getElementById('adProductRow').style.display = action === 'PRODUCT' ? '' : 'none';
  document.getElementById('adCategoryRow').style.display = action === 'CATEGORY' ? '' : 'none';
  document.getElementById('adWebsiteRow').style.display = action === 'WEBSITE' ? '' : 'none';
}

// Dropdown san pham/nhom cho quang cao: App mo san pham theo id (admin-portal) va nhom theo MA nhom.
async function fillAdTargetSelects(ad) {
  const [productsPage, categoriesPage] = await Promise.all([
    apiFetch('/api/admin/products?size=200').catch(() => ({ content: [] })),
    apiFetch('/api/admin/categories?size=100').catch(() => ({ content: [] }))
  ]);
  const products = productsPage.content || [];
  const categories = categoriesPage.content || [];
  const productSelect = document.getElementById('adProductId');
  productSelect.innerHTML = '<option value="">-- Chọn sản phẩm --</option>' + products
    .map(p => `<option value="${escapeHtml(p.id)}">${escapeHtml(p.code)} - ${escapeHtml(p.name)}</option>`).join('');
  const categorySelect = document.getElementById('adCategoryId');
  categorySelect.innerHTML = '<option value="">-- Chọn nhóm --</option>' + categories
    .map(c => `<option value="${escapeHtml(c.code)}">${escapeHtml(c.name)} (${escapeHtml(c.code)})</option>`).join('');
  productSelect.value = ad && ad.productId ? ad.productId : '';
  categorySelect.value = ad && ad.categoryId ? ad.categoryId : '';
}

async function openAdModal(ad) {
  document.getElementById('adModalTitle').textContent = ad ? 'Sửa quảng cáo' : 'Thêm quảng cáo';
  document.getElementById('adId').value = ad ? ad.id : '';
  document.getElementById('adName').value = ad ? (ad.name || '') : '';
  document.getElementById('adMediaType').value = ad ? ad.mediaType : 'IMAGE';
  document.getElementById('adUrl').value = ad ? ad.url : '';
  document.getElementById('adActionType').value = ad ? ad.actionType : 'NONE';
  document.getElementById('adWebsiteUrl').value = ad ? (ad.websiteUrl || '') : '';
  document.getElementById('adDisplayOrder').value = ad ? ad.order : (adsCache.length + 1);
  document.getElementById('adDisplaySeconds').value = ad ? ad.displaySeconds : 8;
  document.getElementById('adEnabled').checked = ad ? ad.enabled : true;
  syncAdActionRows();
  await fillAdTargetSelects(ad);
  document.getElementById('adModal').style.display = 'flex';
}

async function submitAd() {
  const id = document.getElementById('adId').value;
  const actionType = document.getElementById('adActionType').value;
  const payload = {
    name: document.getElementById('adName').value.trim(),
    mediaType: document.getElementById('adMediaType').value,
    url: document.getElementById('adUrl').value.trim(),
    actionType,
    productId: actionType === 'PRODUCT' ? (document.getElementById('adProductId').value || null) : null,
    categoryId: actionType === 'CATEGORY' ? (document.getElementById('adCategoryId').value || null) : null,
    websiteUrl: actionType === 'WEBSITE' ? (document.getElementById('adWebsiteUrl').value.trim() || null) : null,
    order: Number(document.getElementById('adDisplayOrder').value),
    displaySeconds: Number(document.getElementById('adDisplaySeconds').value),
    enabled: document.getElementById('adEnabled').checked
  };
  if (!payload.name || !payload.url) { alert('Vui lòng nhập tên và URL media'); return; }
  if (actionType === 'PRODUCT' && !payload.productId) { alert('Vui lòng chọn sản phẩm'); return; }
  if (actionType === 'CATEGORY' && !payload.categoryId) { alert('Vui lòng chọn nhóm sản phẩm'); return; }
  if (actionType === 'WEBSITE' && !payload.websiteUrl) { alert('Vui lòng nhập website URL'); return; }
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

// ---------- San pham (proxy sang admin-portal, kenh KIOSK) ----------

let categoriesCache = [];
let inventoryCache = [];

async function loadCategoriesForDropdown() {
  try {
    const page = await apiFetch('/api/admin/categories?size=100');
    categoriesCache = page.content || [];
    const select = document.getElementById('productCategoryId');
    const current = select.value;
    select.innerHTML = '<option value="">-- Chọn nhóm sản phẩm --</option>' +
      categoriesCache.map(c => `<option value="${escapeHtml(c.id)}">${escapeHtml(c.name || c.code || c.id)}</option>`).join('');
    select.value = current;
  } catch (err) {
    console.error('Không tải được danh sách nhóm sản phẩm', err);
  }
}

function categoryName(categoryId, categoryName) {
  if (categoryName) return categoryName;
  if (!categoryId) return '-';
  const found = categoriesCache.find(c => c.id === categoryId);
  return found ? (found.name || found.id) : categoryId;
}

async function loadProducts() {
  const tbody = document.getElementById('productsTableBody');
  tbody.innerHTML = '<tr><td colspan="6">Đang tải...</td></tr>';
  try {
    const page = await apiFetch('/api/admin/products?size=100');
    const products = page.content || [];
    if (!products.length) {
      tbody.innerHTML = '<tr><td colspan="6">Chưa có sản phẩm nào</td></tr>';
      return;
    }
    tbody.innerHTML = products.map(renderProductRow).join('');
    tbody.querySelectorAll('button[data-action]').forEach(btn => {
      btn.addEventListener('click', () => handleProductAction(btn.dataset.action, btn.dataset.id, products));
    });
  } catch (err) {
    tbody.innerHTML = '<tr><td colspan="6">Lỗi: ' + escapeHtml(err.message) + '</td></tr>';
  }
}

function renderProductRow(p) {
  return `<tr>
    <td>${escapeHtml(p.code || '-')}</td>
    <td>${escapeHtml(p.name || '-')}</td>
    <td>${escapeHtml(categoryName(p.categoryId, p.categoryName))}</td>
    <td>${p.quantity != null ? p.quantity : '-'}</td>
    <td><span class="status-pill ${p.kioskInvisible ? 'status-OFF' : 'status-ON'}">${p.kioskInvisible ? 'Ẩn' : 'Hiện'}</span></td>
    <td>
      <button class="btn-secondary btn-small" data-action="toggle" data-id="${p.id}">${p.kioskInvisible ? 'Hiển thị' : 'Ẩn đi'}</button>
      <button class="btn-secondary btn-small" data-action="edit" data-id="${p.id}">Sửa</button>
      <button class="btn-secondary btn-small" data-action="price" data-id="${p.id}">Giá</button>
      <button class="btn-secondary btn-small" data-action="inventory" data-id="${p.id}">Tồn kho</button>
      <button class="btn-danger btn-small" data-action="delete" data-id="${p.id}">Xóa</button>
    </td>
  </tr>`;
}

async function handleProductAction(action, id, products) {
  try {
    if (action === 'toggle') {
      await apiFetch(`/api/admin/products/${encodeURIComponent(id)}/toggle-visibility`, { method: 'POST' });
      loadProducts();
    } else if (action === 'delete') {
      if (!confirm('Xóa sản phẩm này (và các partnerConfig/ánh liên quan bên admin-portal)? Không thể hoàn tác.')) return;
      await apiFetch(`/api/admin/products/${encodeURIComponent(id)}`, { method: 'DELETE' });
      loadProducts();
    } else if (action === 'edit') {
      const product = products.find(x => x.id === id);
      openProductModal(product);
    } else if (action === 'price') {
      const product = products.find(x => x.id === id);
      openPriceModal(product);
    } else if (action === 'inventory') {
      const product = products.find(x => x.id === id);
      openInventoryModal(product);
    }
  } catch (err) {
    alert(err.message);
  }
}

function openProductModal(product) {
  document.getElementById('productModalTitle').textContent = product ? 'Sửa sản phẩm' : 'Tạo sản phẩm';
  document.getElementById('productId').value = product ? product.id : '';
  document.getElementById('productCode').value = product ? (product.code || '') : '';
  document.getElementById('productName').value = product ? (product.name || '') : '';
  document.getElementById('productCategoryId').value = product ? (product.categoryId || '') : '';
  document.getElementById('productQuantity').value = product && product.quantity != null ? product.quantity : '';
  document.getElementById('productDescription').value = product ? (product.description || '') : '';
  document.getElementById('productInvisible').checked = !!(product && product.invisible);
  document.getElementById('productModal').style.display = 'flex';
}

async function submitProduct() {
  const id = document.getElementById('productId').value;
  const quantityRaw = document.getElementById('productQuantity').value.trim();
  const payload = {
    code: document.getElementById('productCode').value.trim(),
    name: document.getElementById('productName').value.trim(),
    categoryId: document.getElementById('productCategoryId').value || null,
    quantity: quantityRaw ? Number(quantityRaw) : null,
    description: document.getElementById('productDescription').value.trim(),
    invisible: document.getElementById('productInvisible').checked
  };
  if (!payload.code) { alert('Vui lòng nhập mã sản phẩm'); return; }
  if (!payload.name) { alert('Vui lòng nhập tên sản phẩm'); return; }
  if (!payload.categoryId) { alert('Vui lòng chọn nhóm sản phẩm'); return; }
  try {
    if (id) {
      await apiFetch(`/api/admin/products/${encodeURIComponent(id)}`, { method: 'PUT', body: JSON.stringify(payload) });
    } else {
      await apiFetch('/api/admin/products', { method: 'POST', body: JSON.stringify(payload) });
    }
    document.getElementById('productModal').style.display = 'none';
    loadProducts();
  } catch (err) {
    alert(err.message);
  }
}

// ---------- Nhom san pham (category) ----------

function openCategoryModal(category) {
  document.getElementById('categoryModalTitle').textContent = category ? 'Sửa nhóm sản phẩm' : 'Tạo nhóm sản phẩm';
  document.getElementById('categoryId').value = category ? category.id : '';
  document.getElementById('categoryCode').value = category ? (category.code || '') : '';
  document.getElementById('categoryName').value = category ? (category.name || '') : '';
  document.getElementById('categoryDescription').value = category ? (category.description || '') : '';
  // admin-portal: mo ta bat buoc khi tao, khong bat buoc khi sua.
  document.getElementById('categoryDescriptionLabel').textContent = category ? 'Mô tả' : 'Mô tả *';
  document.getElementById('categoryModal').style.display = 'flex';
}

async function submitCategory() {
  const id = document.getElementById('categoryId').value;
  const payload = {
    code: document.getElementById('categoryCode').value.trim(),
    name: document.getElementById('categoryName').value.trim(),
    description: document.getElementById('categoryDescription').value.trim() || null
  };
  if (!payload.code || !payload.name || (!id && !payload.description)) {
    alert(id ? 'Vui lòng nhập mã và tên' : 'Vui lòng nhập đầy đủ mã / tên / mô tả');
    return;
  }
  try {
    if (id) {
      await apiFetch(`/api/admin/categories/${encodeURIComponent(id)}`, { method: 'PUT', body: JSON.stringify(payload) });
    } else {
      await apiFetch('/api/admin/categories', { method: 'POST', body: JSON.stringify(payload) });
    }
    document.getElementById('categoryModal').style.display = 'none';
    await loadCategoriesForDropdown();
    if (document.getElementById('tab-categories').classList.contains('active')) loadCategoriesTab();
  } catch (err) {
    alert(err.message);
  }
}

// ---------- Tab Nhom san pham ----------

let categoriesTabCache = [];

async function loadCategoriesTab() {
  const tbody = document.getElementById('categoriesTableBody');
  tbody.innerHTML = '<tr><td colspan="6">Đang tải...</td></tr>';
  try {
    const page = await apiFetch('/api/admin/categories?size=100');
    const categories = (page.content || []).slice()
      .sort((a, b) => (a.priority ?? 9999) - (b.priority ?? 9999));
    categoriesTabCache = categories;
    if (!categories.length) {
      tbody.innerHTML = '<tr><td colspan="6">Chưa có nhóm nào</td></tr>';
      return;
    }
    tbody.innerHTML = categories.map(c => `<tr>
      <td><input type="number" class="cell-input" min="1" data-priority-id="${escapeHtml(c.id)}" value="${c.priority ?? ''}"></td>
      <td>${escapeHtml(c.code)}</td>
      <td>${escapeHtml(c.name)}</td>
      <td class="cell-clip">${escapeHtml(c.description || '-')}</td>
      <td><span class="status-pill ${c.kioskActive ? 'status-ON' : 'status-OFF'}">${c.kioskActive ? 'Hiện' : 'Ẩn'}</span></td>
      <td>
        <button class="btn-secondary btn-small" data-action="toggle" data-id="${escapeHtml(c.id)}">${c.kioskActive ? 'Ẩn đi' : 'Hiển thị'}</button>
        <button class="btn-secondary btn-small" data-action="edit" data-id="${escapeHtml(c.id)}">Sửa</button>
        <button class="btn-danger btn-small" data-action="delete" data-id="${escapeHtml(c.id)}">Xóa</button>
      </td>
    </tr>`).join('');
    tbody.querySelectorAll('button[data-action]').forEach(btn => {
      btn.addEventListener('click', () => handleCategoryAction(btn.dataset.action, btn.dataset.id));
    });
  } catch (err) {
    tbody.innerHTML = '<tr><td colspan="6">Lỗi: ' + escapeHtml(err.message) + '</td></tr>';
  }
}

async function handleCategoryAction(action, id) {
  const category = categoriesTabCache.find(c => c.id === id);
  if (!category) return;
  try {
    if (action === 'edit') {
      openCategoryModal(category);
      return;
    } else if (action === 'toggle') {
      await apiFetch(`/api/admin/categories/${encodeURIComponent(id)}/visibility`, {
        method: 'PATCH', body: JSON.stringify({ visible: !category.kioskActive })
      });
    } else if (action === 'delete') {
      if (!confirm(`Gỡ nhóm "${category.name}" khỏi kênh KIOSK? (Nhóm còn sản phẩm trên kênh này sẽ bị từ chối.)`)) return;
      await apiFetch(`/api/admin/categories/${encodeURIComponent(id)}`, { method: 'DELETE' });
    }
    loadCategoriesTab();
  } catch (err) {
    alert(err.message);
  }
}

async function saveCategoryOrder() {
  const inputs = document.querySelectorAll('#categoriesTableBody input[data-priority-id]');
  const priorities = [];
  for (const input of inputs) {
    const value = Number(input.value);
    if (!Number.isInteger(value) || value < 1) {
      alert('Thứ tự phải là số nguyên từ 1');
      input.focus();
      return;
    }
    priorities.push({ categoryId: input.dataset.priorityId, priority: value });
  }
  if (!priorities.length) return;
  try {
    await apiFetch('/api/admin/categories/priority', { method: 'PUT', body: JSON.stringify(priorities) });
    loadCategoriesTab();
  } catch (err) {
    alert(err.message);
  }
}

// ---------- Banner ----------

let bannersCache = [];

async function loadBanners() {
  const tbody = document.getElementById('bannersTableBody');
  tbody.innerHTML = '<tr><td colspan="7">Đang tải...</td></tr>';
  try {
    const banners = await apiFetch('/api/admin/banners');
    bannersCache = banners;
    if (!banners.length) {
      tbody.innerHTML = '<tr><td colspan="7">Chưa có banner nào</td></tr>';
      return;
    }
    tbody.innerHTML = banners.map(b => `<tr>
      <td>${b.priority}</td>
      <td><img class="thumb" src="${escapeHtml(b.imagePath)}" alt=""></td>
      <td>${escapeHtml(b.code || '-')}</td>
      <td>${escapeHtml(b.name)}</td>
      <td class="cell-clip">${escapeHtml(b.descriptions || '-')}</td>
      <td><span class="status-pill ${b.enabled ? 'status-ON' : 'status-OFF'}">${b.enabled ? 'Đang bật' : 'Đang tắt'}</span></td>
      <td>
        <button class="btn-secondary btn-small" data-action="toggle" data-id="${escapeHtml(b.id)}">${b.enabled ? 'Tắt' : 'Bật'}</button>
        <button class="btn-secondary btn-small" data-action="edit" data-id="${escapeHtml(b.id)}">Sửa</button>
        <button class="btn-danger btn-small" data-action="delete" data-id="${escapeHtml(b.id)}">Xóa</button>
      </td>
    </tr>`).join('');
    tbody.querySelectorAll('button[data-action]').forEach(btn => {
      btn.addEventListener('click', () => handleBannerAction(btn.dataset.action, btn.dataset.id));
    });
  } catch (err) {
    tbody.innerHTML = '<tr><td colspan="7">Lỗi: ' + escapeHtml(err.message) + '</td></tr>';
  }
}

async function handleBannerAction(action, id) {
  try {
    if (action === 'edit') {
      openBannerModal(bannersCache.find(b => b.id === id));
      return;
    } else if (action === 'toggle') {
      await apiFetch(`/api/admin/banners/${encodeURIComponent(id)}/toggle`, { method: 'POST' });
    } else if (action === 'delete') {
      if (!confirm('Xóa banner này?')) return;
      await apiFetch(`/api/admin/banners/${encodeURIComponent(id)}`, { method: 'DELETE' });
    }
    loadBanners();
  } catch (err) {
    alert(err.message);
  }
}

function openBannerModal(banner) {
  document.getElementById('bannerModalTitle').textContent = banner ? 'Sửa banner' : 'Thêm banner';
  document.getElementById('bannerId').value = banner ? banner.id : '';
  document.getElementById('bannerName').value = banner ? (banner.name || '') : '';
  document.getElementById('bannerImagePath').value = banner ? (banner.imagePath || '') : '';
  document.getElementById('bannerCode').value = banner ? (banner.code || '') : '';
  document.getElementById('bannerDescriptions').value = banner ? (banner.descriptions || '') : '';
  document.getElementById('bannerPriority').value = banner ? banner.priority : (bannersCache.length + 1);
  document.getElementById('bannerEnabled').checked = banner ? banner.enabled : true;
  document.getElementById('bannerModal').style.display = 'flex';
}

async function submitBanner() {
  const id = document.getElementById('bannerId').value;
  const payload = {
    name: document.getElementById('bannerName').value.trim(),
    imagePath: document.getElementById('bannerImagePath').value.trim(),
    code: document.getElementById('bannerCode').value.trim() || null,
    descriptions: document.getElementById('bannerDescriptions').value.trim() || null,
    priority: Number(document.getElementById('bannerPriority').value),
    enabled: document.getElementById('bannerEnabled').checked
  };
  if (!payload.name || !payload.imagePath) { alert('Vui lòng nhập tên và URL ảnh'); return; }
  try {
    if (id) {
      await apiFetch(`/api/admin/banners/${encodeURIComponent(id)}`, { method: 'PUT', body: JSON.stringify(payload) });
    } else {
      await apiFetch('/api/admin/banners', { method: 'POST', body: JSON.stringify(payload) });
    }
    document.getElementById('bannerModal').style.display = 'none';
    loadBanners();
  } catch (err) {
    alert(err.message);
  }
}

// ---------- Cua hang (bat/tat theo kenh KIOSK) ----------

let provincesById = null;
let storesPageCache = [];
const STORES_PAGE_SIZE = 20;

function storeKioskActive(s) {
  const cfg = (s.partnerConfigs || []).find(c => c.partnerCode === 'KIOSK');
  return !!(s.isActive && cfg && cfg.isActive);
}

async function loadStores() {
  const tbody = document.getElementById('storesTableBody');
  tbody.innerHTML = '<tr><td colspan="7">Đang tải...</td></tr>';
  try {
    if (!provincesById) {
      const res = await apiFetch('/api/admin/stores/provinces').catch(() => ({ provinces: [] }));
      provincesById = {};
      (res.provinces || []).forEach(p => { provincesById[p.id] = p.name; });
    }
    const qs = new URLSearchParams({ page: pageState.stores, size: STORES_PAGE_SIZE });
    const name = document.getElementById('storeSearch').value.trim();
    if (name) qs.set('name', name);
    const page = await apiFetch('/api/admin/stores?' + qs.toString());
    const stores = page.data || [];
    storesPageCache = stores;
    document.getElementById('storesPageInfo').textContent =
      `Trang ${pageState.stores + 1} / ${Math.max(page.totalPages || 1, 1)} (${page.total || 0} cửa hàng)`;
    if (!stores.length) {
      tbody.innerHTML = '<tr><td colspan="7">Không có cửa hàng nào</td></tr>';
      return;
    }
    tbody.innerHTML = stores.map(s => {
      const active = storeKioskActive(s);
      return `<tr>
        <td>${s.id}</td>
        <td>${escapeHtml(s.storeCode || '-')}</td>
        <td>${escapeHtml(s.name)}</td>
        <td class="cell-clip">${escapeHtml(s.address || '-')}</td>
        <td>${escapeHtml(provincesById[s.provinceId] || '-')}</td>
        <td><span class="status-pill ${active ? 'status-ON' : 'status-OFF'}">${active ? 'Đang bật' : 'Đang tắt'}</span></td>
        <td><button class="btn-secondary btn-small" data-id="${s.id}" data-active="${active}">${active ? 'Tắt' : 'Bật'}</button></td>
      </tr>`;
    }).join('');
    tbody.querySelectorAll('button[data-id]').forEach(btn => {
      btn.addEventListener('click', () => toggleStore(btn.dataset.id, btn.dataset.active !== 'true'));
    });
  } catch (err) {
    tbody.innerHTML = '<tr><td colspan="7">Lỗi: ' + escapeHtml(err.message) + '</td></tr>';
  }
}

async function toggleStore(id, active) {
  try {
    await apiFetch(`/api/admin/stores/${encodeURIComponent(id)}/active`, {
      method: 'PATCH', body: JSON.stringify({ active })
    });
    storesForSelect = null; // danh sach chon cua hang cho may can tai lai
    loadStores();
  } catch (err) {
    alert(err.message);
  }
}

// ---------- Han muc ----------

let limitsCache = [];

async function loadLimits() {
  const tbody = document.getElementById('limitsTableBody');
  tbody.innerHTML = '<tr><td colspan="6">Đang tải...</td></tr>';
  try {
    const page = await apiFetch('/api/admin/limits');
    const limits = page.data || [];
    limitsCache = limits;
    if (!limits.length) {
      tbody.innerHTML = '<tr><td colspan="6">Chưa có hạn mức nào</td></tr>';
      return;
    }
    tbody.innerHTML = limits.map(l => {
      const updated = l.updatedAt || l.createdAt;
      return `<tr>
        <td>${l.id}</td>
        <td>${escapeHtml(l.partnerCode || '-')}</td>
        <td><b>${l.limitQuantity}</b></td>
        <td><span class="status-pill ${l.isActive ? 'status-ON' : 'status-OFF'}">${l.isActive ? 'Đang áp dụng' : 'Tắt'}</span></td>
        <td>${updated ? new Date(updated).toLocaleString('vi-VN') : '-'}</td>
        <td>
          <button class="btn-secondary btn-small" data-action="edit" data-id="${l.id}">Sửa</button>
          <button class="btn-secondary btn-small" data-action="toggle" data-id="${l.id}">${l.isActive ? 'Tắt' : 'Bật'}</button>
        </td>
      </tr>`;
    }).join('');
    tbody.querySelectorAll('button[data-action]').forEach(btn => {
      btn.addEventListener('click', () => handleLimitAction(btn.dataset.action, btn.dataset.id));
    });
  } catch (err) {
    tbody.innerHTML = '<tr><td colspan="6">Lỗi: ' + escapeHtml(err.message) + '</td></tr>';
  }
}

async function handleLimitAction(action, id) {
  const limit = limitsCache.find(l => String(l.id) === id);
  if (!limit) return;
  if (action === 'edit') {
    document.getElementById('limitId').value = id;
    document.getElementById('limitQuantity').value = limit.limitQuantity;
    document.getElementById('limitModal').style.display = 'flex';
    return;
  }
  if (limit.isActive && !confirm('Tắt hạn mức? Khi tắt, App không giới hạn số lượng mỗi giao dịch.')) return;
  try {
    await apiFetch(`/api/admin/limits/${encodeURIComponent(id)}/active`, {
      method: 'PATCH', body: JSON.stringify({ active: !limit.isActive })
    });
    loadLimits();
  } catch (err) {
    alert(err.message);
  }
}

async function submitLimit() {
  const id = document.getElementById('limitId').value;
  const quantity = Number(document.getElementById('limitQuantity').value);
  if (!Number.isInteger(quantity) || quantity < 1) { alert('Hạn mức phải là số nguyên từ 1'); return; }
  try {
    await apiFetch(`/api/admin/limits/${encodeURIComponent(id)}/quantity`, {
      method: 'PATCH', body: JSON.stringify({ limitQuantity: quantity })
    });
    document.getElementById('limitModal').style.display = 'none';
    loadLimits();
  } catch (err) {
    alert(err.message);
  }
}

// ---------- Cau hinh kenh ----------

async function loadConfig() {
  try {
    const c = await apiFetch('/api/admin/channel-config');
    document.getElementById('cfgOpenTime').value = c.openTime || '';
    document.getElementById('cfgCloseTime').value = c.closeTime || '';
    document.getElementById('cfgHotline').value = c.hotline || '';
    document.getElementById('cfgMinAppVersion').value = c.minAppVersion || '';
    document.getElementById('cfgIdleTimeout').value = c.idleTimeoutSeconds;
    document.getElementById('cfgStalePrice').value = c.stalePriceSeconds;
    document.getElementById('cfgPaused').checked = !!c.channelPaused;
  } catch (err) {
    alert(err.message);
  }
}

async function saveConfig() {
  const payload = {
    openTime: document.getElementById('cfgOpenTime').value || null,
    closeTime: document.getElementById('cfgCloseTime').value || null,
    hotline: document.getElementById('cfgHotline').value.trim(),
    minAppVersion: document.getElementById('cfgMinAppVersion').value.trim(),
    idleTimeoutSeconds: Number(document.getElementById('cfgIdleTimeout').value),
    stalePriceSeconds: Number(document.getElementById('cfgStalePrice').value),
    channelPaused: document.getElementById('cfgPaused').checked
  };
  if (payload.channelPaused && !confirm('Tạm dừng bán trên TOÀN BỘ máy kiosk?')) return;
  try {
    await apiFetch('/api/admin/channel-config', { method: 'PUT', body: JSON.stringify(payload) });
    await loadConfig();
    alert('Đã lưu cấu hình');
  } catch (err) {
    alert(err.message);
  }
}

// ---------- Gia (ban ra / mua vao) - ghi truc tiep DB, xem README ----------

let currentPriceProductCode = null;

async function openPriceModal(product) {
  if (!product || !product.code) { alert('Sản phẩm chưa có mã (code)'); return; }
  currentPriceProductCode = product.code;
  document.getElementById('priceModalProductCode').textContent = product.code;
  document.getElementById('priceSell').value = '';
  document.getElementById('priceBuy').value = '';
  document.getElementById('priceDescription').value = '';
  document.getElementById('priceHistoryTableBody').innerHTML = '<tr><td colspan="4">Đang tải...</td></tr>';
  document.getElementById('priceModal').style.display = 'flex';
  try {
    const current = await apiFetch(`/api/admin/prices/${encodeURIComponent(product.code)}`);
    document.getElementById('priceSell').value = current.price != null ? current.price : '';
    document.getElementById('priceBuy').value = current.buyPrice != null ? current.buyPrice : '';
    document.getElementById('priceDescription').value = current.description || '';
  } catch (err) {
    // 404 nghia la chua thiet lap gia - khong phai loi, de trong form la dung.
  }
  loadPriceHistory(product.code);
}

async function loadPriceHistory(productCode) {
  const tbody = document.getElementById('priceHistoryTableBody');
  try {
    const page = await apiFetch(`/api/admin/prices/${encodeURIComponent(productCode)}/history?size=10`);
    const rows = page.content || [];
    tbody.innerHTML = rows.length ? rows.map(renderPriceHistoryRow).join('')
      : '<tr><td colspan="4">Chưa có lịch sử</td></tr>';
  } catch (err) {
    tbody.innerHTML = '<tr><td colspan="4">Lỗi: ' + escapeHtml(err.message) + '</td></tr>';
  }
}

function renderPriceHistoryRow(h) {
  const time = h.changedAt ? new Date(h.changedAt).toLocaleString('vi-VN') : '-';
  return `<tr>
    <td>${time}</td>
    <td>${h.oldPrice != null ? h.oldPrice : '-'} -&gt; ${h.newPrice}</td>
    <td>${h.oldBuyPrice != null ? h.oldBuyPrice : '-'} -&gt; ${h.newBuyPrice != null ? h.newBuyPrice : '-'}</td>
    <td>${escapeHtml(h.changedBy || '-')}</td>
  </tr>`;
}

async function submitPrice() {
  if (!currentPriceProductCode) return;
  const priceRaw = document.getElementById('priceSell').value.trim();
  if (!priceRaw) { alert('Vui lòng nhập giá bán ra'); return; }
  const payload = {
    price: Number(priceRaw),
    buyPrice: document.getElementById('priceBuy').value.trim() ? Number(document.getElementById('priceBuy').value.trim()) : null,
    description: document.getElementById('priceDescription').value.trim() || null
  };
  try {
    await apiFetch(`/api/admin/prices/${encodeURIComponent(currentPriceProductCode)}`, {
      method: 'PUT', body: JSON.stringify(payload)
    });
    loadPriceHistory(currentPriceProductCode);
    alert('Đã lưu giá');
  } catch (err) {
    alert(err.message);
  }
}

// ---------- Ton kho ----------

let currentInventoryId = null;

async function openInventoryModal(product) {
  if (!product || !product.code) { alert('Sản phẩm chưa có mã (code)'); return; }
  document.getElementById('inventoryModalProductCode').textContent = product.code;
  document.getElementById('inventoryOriginal').textContent = '-';
  document.getElementById('inventoryRemaining').textContent = '-';
  document.getElementById('inventoryAvailable').textContent = '-';
  document.getElementById('inventoryQuantity').value = '';
  document.getElementById('inventoryHistoryTableBody').innerHTML = '<tr><td colspan="4">Đang tải...</td></tr>';
  document.getElementById('inventoryModal').style.display = 'flex';
  try {
    const page = await apiFetch('/api/admin/inventory?size=100');
    inventoryCache = page.content || [];
    const inv = inventoryCache.find(i => i.productCode === product.code);
    if (!inv) {
      alert('Sản phẩm này chưa có dòng tồn kho bên admin-portal (thường được tạo cùng lúc với sản phẩm).');
      document.getElementById('inventoryModal').style.display = 'none';
      return;
    }
    currentInventoryId = inv.id;
    document.getElementById('inventoryOriginal').textContent = inv.originalQuantity != null ? inv.originalQuantity : '-';
    document.getElementById('inventoryRemaining').textContent = inv.remainingQuantity != null ? inv.remainingQuantity : '-';
    document.getElementById('inventoryAvailable').textContent = inv.availableQuantity != null ? inv.availableQuantity : '-';
    document.getElementById('inventoryQuantity').value = inv.originalQuantity != null ? inv.originalQuantity : '';
    loadInventoryHistory(inv.id);
  } catch (err) {
    alert(err.message);
    document.getElementById('inventoryModal').style.display = 'none';
  }
}

async function loadInventoryHistory(inventoryId) {
  const tbody = document.getElementById('inventoryHistoryTableBody');
  try {
    const page = await apiFetch(`/api/admin/inventory/${encodeURIComponent(inventoryId)}/history?size=10`);
    const rows = page.content || [];
    tbody.innerHTML = rows.length ? rows.map(renderInventoryHistoryRow).join('')
      : '<tr><td colspan="4">Chưa có lịch sử</td></tr>';
  } catch (err) {
    tbody.innerHTML = '<tr><td colspan="4">Lỗi: ' + escapeHtml(err.message) + '</td></tr>';
  }
}

function renderInventoryHistoryRow(h) {
  const time = h.createdAt ? new Date(h.createdAt).toLocaleString('vi-VN') : '-';
  return `<tr>
    <td>${time}</td>
    <td>${escapeHtml(h.actionType || '-')}</td>
    <td>${h.oldQuantity != null ? h.oldQuantity : '-'} -&gt; ${h.newQuantity != null ? h.newQuantity : '-'}</td>
    <td>${escapeHtml(h.createdBy || '-')}</td>
  </tr>`;
}

async function submitInventory() {
  if (!currentInventoryId) return;
  const raw = document.getElementById('inventoryQuantity').value.trim();
  if (!raw) { alert('Vui lòng nhập định mức tồn kho'); return; }
  try {
    await apiFetch(`/api/admin/inventory/${encodeURIComponent(currentInventoryId)}`, {
      method: 'PUT', body: JSON.stringify({ quantity: Number(raw) })
    });
    loadInventoryHistory(currentInventoryId);
    alert('Đã cập nhật định mức tồn kho');
  } catch (err) {
    alert(err.message);
  }
}

// ---------- Log loi thiet bi (chi doc) ----------

async function loadErrorLogs() {
  const tbody = document.getElementById('errorLogsTableBody');
  tbody.innerHTML = '<tr><td colspan="6">Đang tải...</td></tr>';
  try {
    const deviceCode = document.getElementById('errorLogDeviceCode').value.trim();
    const qs = new URLSearchParams({ page: pageState.errorlogs, size: PAGE_SIZE });
    if (deviceCode) qs.set('deviceCode', deviceCode);
    const pageData = await apiFetch('/api/admin/device-error-logs?' + qs.toString());
    const rows = pageData.content || [];
    if (!rows.length) {
      tbody.innerHTML = '<tr><td colspan="6">Không có log nào</td></tr>';
    } else {
      tbody.innerHTML = rows.map(renderErrorLogRow).join('');
    }
    document.getElementById('errorLogsPageInfo').textContent = `Trang ${pageState.errorlogs + 1} / ${Math.max(pageData.totalPages || 1, 1)}`;
  } catch (err) {
    tbody.innerHTML = '<tr><td colspan="6">Lỗi: ' + escapeHtml(err.message) + '</td></tr>';
  }
}

function renderErrorLogRow(e) {
  const occurred = e.occurredAt ? new Date(e.occurredAt).toLocaleString('vi-VN') : '-';
  const received = e.receivedAt ? new Date(e.receivedAt).toLocaleString('vi-VN') : '-';
  return `<tr>
    <td>${escapeHtml(e.deviceCode || '-')}</td>
    <td>${escapeHtml(e.type || '-')}</td>
    <td>${escapeHtml(e.message || '-')}</td>
    <td class="cell-clip" title="${escapeHtml(e.context || '')}">${escapeHtml(e.context || '-')}</td>
    <td>${occurred}</td>
    <td>${received}</td>
  </tr>`;
}

// ---------- Audit log (chi doc) ----------

async function loadAuditLogs() {
  const tbody = document.getElementById('auditLogsTableBody');
  tbody.innerHTML = '<tr><td colspan="6">Đang tải...</td></tr>';
  try {
    const qs = new URLSearchParams({ page: pageState.auditlogs, size: PAGE_SIZE });
    const pageData = await apiFetch('/api/admin/audit-logs?' + qs.toString());
    const rows = pageData.content || [];
    if (!rows.length) {
      tbody.innerHTML = '<tr><td colspan="6">Không có log nào</td></tr>';
    } else {
      tbody.innerHTML = rows.map(renderAuditLogRow).join('');
    }
    document.getElementById('auditLogsPageInfo').textContent = `Trang ${pageState.auditlogs + 1} / ${Math.max(pageData.totalPages || 1, 1)}`;
  } catch (err) {
    tbody.innerHTML = '<tr><td colspan="6">Lỗi: ' + escapeHtml(err.message) + '</td></tr>';
  }
}

function renderAuditLogRow(a) {
  const created = a.createdAt ? new Date(a.createdAt).toLocaleString('vi-VN') : '-';
  const target = [a.targetType, a.targetId].filter(Boolean).join(' / ');
  return `<tr>
    <td>${created}</td>
    <td>${escapeHtml(a.username || '-')}</td>
    <td>${escapeHtml(a.action || '-')}</td>
    <td>${escapeHtml(target || '-')}</td>
    <td>${escapeHtml(a.detail || '-')}</td>
    <td>${escapeHtml(a.ipAddress || '-')}</td>
  </tr>`;
}

// ---------- Log giao dich / gia (chi doc) ----------

async function loadTxLogs() {
  const tbody = document.getElementById('txLogsTableBody');
  tbody.innerHTML = '<tr><td colspan="5">Đang tải...</td></tr>';
  try {
    const qs = new URLSearchParams({ page: pageState.txlogs, size: PAGE_SIZE });
    const pageData = await apiFetch('/api/admin/transaction-logs?' + qs.toString());
    const rows = pageData.content || [];
    if (!rows.length) {
      tbody.innerHTML = '<tr><td colspan="5">Không có log nào</td></tr>';
    } else {
      tbody.innerHTML = rows.map(renderTxLogRow).join('');
    }
    document.getElementById('txLogsPageInfo').textContent = `Trang ${pageState.txlogs + 1} / ${Math.max(pageData.totalPages || 1, 1)}`;
  } catch (err) {
    tbody.innerHTML = '<tr><td colspan="5">Lỗi: ' + escapeHtml(err.message) + '</td></tr>';
  }
}

function renderTxLogRow(t) {
  const time = t.time ? new Date(t.time).toLocaleString('vi-VN') : '-';
  return `<tr>
    <td>${time}</td>
    <td>${escapeHtml(t.transactionId || '-')}</td>
    <td>${escapeHtml(t.path || '-')}</td>
    <td>${escapeHtml(t.method || '-')}</td>
    <td>${t.duration != null ? t.duration : '-'}</td>
  </tr>`;
}

async function loadPriceLogs() {
  const tbody = document.getElementById('priceLogsTableBody');
  tbody.innerHTML = '<tr><td colspan="5">Đang tải...</td></tr>';
  try {
    const qs = new URLSearchParams({ page: pageState.pricelogs, size: PAGE_SIZE });
    const pageData = await apiFetch('/api/admin/price-update-logs?' + qs.toString());
    const rows = pageData.content || [];
    if (!rows.length) {
      tbody.innerHTML = '<tr><td colspan="5">Không có log nào</td></tr>';
    } else {
      tbody.innerHTML = rows.map(renderPriceLogRow).join('');
    }
    document.getElementById('priceLogsPageInfo').textContent = `Trang ${pageState.pricelogs + 1} / ${Math.max(pageData.totalPages || 1, 1)}`;
  } catch (err) {
    tbody.innerHTML = '<tr><td colspan="5">Lỗi: ' + escapeHtml(err.message) + '</td></tr>';
  }
}

function renderPriceLogRow(p) {
  const created = p.createdAt ? new Date(p.createdAt).toLocaleString('vi-VN') : '-';
  return `<tr>
    <td>${created}</td>
    <td>${escapeHtml(p.productCode || '-')}</td>
    <td>${p.price != null ? p.price : '-'}</td>
    <td>${escapeHtml(p.idRequestUpdate || '-')}</td>
    <td>${escapeHtml(p.description || '-')}</td>
  </tr>`;
}

function escapeHtml(str) {
  if (str === null || str === undefined) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}
