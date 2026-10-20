# kiosk-admin

Web quan tri **rieng, nho gon** cho may kiosk (kiosk_device / kiosk_activation_code / kiosk_ad),
tach hoan toan khoi `admin-portal` (cong quan tri to). Khong Vault, khong RabbitMQ, khong Feign,
khong dang nhap bang tai khoan admin-portal.

## Vi sao co project nay

`Backend/admin-portal` la cong quan tri lon quan ly moi thu (san pham, don hang, user, BIDV...).
Module quan ly kiosk moi duoc them gan day vao admin-portal, goi Feign sang `Backend/webview` vi
2 bang `kiosk_device`/`kiosk_activation_code` thuc ra song trong DB cua webview. Nguoi dung muon
mot web quan tri kiosk **rieng, don gian**, khong can dang nhap phuc tap hay cac menu/quyen cua
admin-portal.

`kiosk-admin` ket noi **truc tiep** vao **cung database Postgres** voi `Backend/webview`
(`localdb`), doc/ghi thang cac bang `kiosk_device`, `kiosk_activation_code`, `kiosk_ad`
da co san - khong qua Feign/API noi bo cua webview, khong dong bo du lieu. Ngoai ra con doc them
(chi doc) cac bang `transaction_log`, `price_update_logs` va bang
`kiosk_device_error_log` (xem phan "Cac bang moi" ben duoi).

**Cap nhat quan trong (xem phan "San pham / nhom san pham / ton kho / gia" ben duoi):** bang
`product`/`category` KHONG con duoc kiosk-admin doc/ghi truc tiep nua - tao san pham truc tiep
vao bang `product` KHONG lam san pham xuat hien tren kiosk app thuc (thieu
`partner_product_config` va dong `inventory`, so con lai thuc nam o Redis). Tu ban nay,
san pham/nhom san pham/ton kho duoc quan ly qua goi HTTP sang `Backend/admin-portal` (da co san,
da test, co khoa Redis/kiem tra gio giao dich/dong bo delta an toan) - xem `AdminPortalClient`.
Gia (ban ra/mua vao) la NGOAI LE DUY NHAT van ghi truc tiep vao DB (ly do: he thong thuc khong co
API sua gia cho nguoi).

## Kien truc / quyet dinh ky thuat

- Spring Boot 3.3.7, Java 21 (khop `Backend/webview`).
- `spring.jpa.hibernate.ddl-auto=validate` cho moi entity anh xa vao bang co san
  (`KioskDevice`/`KioskActivationCode`/`KioskAd`/`Product`/`Category`/`TransactionLog`/
  `PriceUpdateLog`) - **Hibernate KHONG duoc phep tu doi schema cua nguoi khac dang dung**.
- Cac bang MOI (`kiosk_admin_user`, `kiosk_device_error_log`, `kiosk_admin_audit_log`) duoc tao
  bang JDBC thuan trong `KioskAdminApplication.main()` (`CREATE TABLE IF NOT EXISTS`, chay TRUOC
  khi Spring khoi tao Hibernate) - can thiet vi `ddl-auto=validate` se kiem tra TOAN BO entity
  ngay luc build `EntityManagerFactory`, qua som de dung `@PostConstruct` hay `ApplicationRunner`
  khac tao bang kip luc.
- Logic sinh/kiem tra ma kich hoat (`KioskDeviceService`, `HashUtil.sha256Hex`) duoc **port 1:1**
  tu `Backend/webview/.../service/kiosk/KioskDeviceAdminService.java` va
  `.../util/KioskSignatureUtil.java#sha256Hex` de tuong thich tuyet doi voi cach webview verify ma
  kich hoat khi may kiosk goi `POST /api/v1/kiosk/devices/enroll`. **KHONG doi thuat toan nay** neu
  khong sua ca ben webview.
- JWT rieng (HS256, jjwt 0.12.6 - cung version voi webview de nhat quan API), secret rieng, KHONG
  dung chung voi webview/admin-portal.
- `JwtAuthFilter` la 1 `OncePerRequestFilter` thu cong (bat chuoc phong cach
  `KioskInternalAuthFilter` ben webview), dang ky qua `FilterRegistrationBean` chi cho
  `/api/admin/*` - khong dung Spring Security day du.
- `SecurityHeadersFilter` la 1 `OncePerRequestFilter` khac, nhung danh dau `@Component` de Spring
  Boot tu dang ky cho TOAN BO path (`/*`) - header bao mat (`X-Content-Type-Options`,
  `X-Frame-Options`, `Referrer-Policy`, `X-XSS-Protection: 0`) nen ap dung ca cho `/`,
  `/api/auth/login`, file tinh..., khong chi `/api/admin/*`.
- Chong brute-force dang nhap: `LoginRateLimiterService` (in-memory, `ConcurrentHashMap`, khong
  them infra moi) - khoa toi da 5 lan sai / 15 phut theo key `username|ip`, kiem tra TRUOC khi
  dong vao DB/BCrypt.
- Audit log (`kiosk_admin_audit_log`, so huu rieng boi app nay) ghi lai moi hanh dong thay doi du
  lieu qua `AuditLogService.record(...)` - goi tu tung controller sau khi mutate thanh cong. Loi
  ghi audit KHONG duoc lam vo request goc (bat Exception, chi log.error).
- `spring-boot-starter-actuator` chi expose `/actuator/health` (xem `application.yml`,
  `management.endpoints.web.exposure.include: health`) - phuc vu Docker `HEALTHCHECK`, KHONG lo
  `/actuator/env`, `/actuator/beans`...
- UI: HTML/CSS/JS thuan, serve tinh tu `src/main/resources/static/`, khong build step.
- Dong goi Docker: multi-stage build giong `Backend/webview/Dockerfile` (builder
  `eclipse-temurin:21-jdk-jammy` + runtime `eclipse-temurin:21-jre-jammy`, non-root `appuser`),
  khac bien: **port mac dinh 8090** (khong phai 8080 nhu webview), healthcheck goi
  `/actuator/health`. Khong co `docker-compose.yml` rieng - app nay ket noi vao Postgres CO SAN
  cua webview, khong tu chay Postgres rieng.

## Cac bang moi (khong ton tai truoc khi co kiosk-admin)

- `kiosk_admin_user`: tai khoan dang nhap cua web nay. So huu rieng, kiosk-admin tu tao bang
  JDBC pre-flight (xem tren).
- `kiosk_device_error_log`: bang **CHUNG voi webview** - `Backend/webview` (sua rieng trong 1
  buoc khac, ngoai pham vi code cua kiosk-admin) la ben **GHI** (may kiosk bao loi len qua API
  cua webview), `kiosk-admin` **CHI DOC** de hien thi man "Log loi thiet bi". Ca 2 ben deu tu tao
  bang nay bang `CREATE TABLE IF NOT EXISTS` (idempotent) de khong phu thuoc thu tu deploy giua 2
  service. Cot: `id, device_code, error_id, error_type, message, occurred_at, context,
  received_at`, unique `(device_code, error_id)`.
- `kiosk_admin_audit_log`: nhat ky hanh dong cua admin, so huu HOAN TOAN boi kiosk-admin - khong
  co service nao khac dong vao. Cot: `id, username, action, target_type, target_id, detail,
  ip_address, created_at`.
- `kiosk_admin_price_history`: lich su sua gia THU CONG, so huu HOAN TOAN boi kiosk-admin. Nam
  trong **DB `gold_management`** (datasource THU 2, khac voi 3 bang tren o `localdb`) - xem phan
  "San pham / nhom san pham / ton kho / gia" ben duoi de biet ly do. Tao qua JDBC pre-flight
  trong `KioskAdminApplication.main()`, cung co che nhung noi GOLD_DB_* thay vi DB_*. Cot: `id,
  product_code, old_price, new_price, old_buy_price, new_buy_price, changed_by, changed_at`.

## San pham / nhom san pham / ton kho / gia - kien truc 2 huong khac nhau

Day la phan THAY DOI LON NHAT so voi ban dau cua kiosk-admin, nen tach rieng giai thich:

**Van de phat hien:** CRUD san pham ban dau cua kiosk-admin tao thang vao bang `product` (shadow
mapping, giong `kiosk_device`/`kiosk_ad`). Nhung khac voi cac bang do, 1 san pham tren kiosk app
THUC can THEM 2 thu nua moi hien ra duoc: 1 dong `partner_product_config` (partner_code=KIOSK,
is_active=true) va 1 dong `inventory` (so con lai THUC nam trong Redis, key
`INVENTORY_QTY:{partnerCode}:{productCode}`, duoc dong bo qua delta bang logic co khoa Redis/kiem
tra gio giao dich trong `Backend/admin-portal`). Tao thang vao `product` se tao ra 1 san pham
"ma" - khong loi, nhung khong bao gio xuat hien tren kiosk thuc.

**Quyet dinh:** KHONG tu viet lai logic do (de an toan ve tien/hang thuc) - kiosk-admin tro thanh
**HTTP CLIENT** cua `Backend/admin-portal` (da co san, da test) cho TOAN BO san
pham/nhom-san-pham/ton-kho. Xem `config/AdminPortalProperties`, `service/AdminPortalAuthClient`
(dang nhap + cache JWT, tu dang nhap lai khi gan het han), `service/AdminPortalClient` (goi
`/api/v1/products`, `/api/v1/product-categories`, `/api/v1/inventories` ben admin-portal, luon
hardcode kenh `partnerCode=KIOSK`). `controller/ProductController`, `controller/CategoryController`
va `controller/InventoryController` (moi) chi la lop proxy mong, KHONG con JPA repository/entity
nao cho `product`/`category` nua (da go bo `entity/Product.java`, `entity/Category.java`,
`repository/ProductRepository.java`, `repository/CategoryRepository.java`).

**Ngoai le DUY NHAT - Gia (ban ra/mua vao):** he thong thuc **KHONG co API sua gia cho nguoi** -
gia chi den tu feed ty gia BTMH tu dong, ghi vao bang `product_prices` (cot: `id, product_code,
price, buy_price, description, id_request_update, price_history_id, created_at, updated_at`).
Nguoi dung van muon co man sua gia THU CONG cho san pham quan ly thu cong (khong co nguon ty gia
tu dong). Vi khong co API nao de goi, `service/ProductPriceService`/`controller/PriceController`
ghi **truc tiep** vao `product_prices` (upsert qua `ON CONFLICT (product_code) DO UPDATE`, UNIQUE
tren `product_code`) roi ghi 1 dong vao `kiosk_admin_price_history` trong CUNG 1 transaction.

**2 database khac nhau - rat quan trong:** trong moi truong dev local, `Backend/admin-portal`
chay tren DB rieng (`gold_management`, container `gold-db`, port 5432), KHAC voi DB cua
`Backend/webview` ma kiosk-admin van dung cho `kiosk_device`/`kiosk_activation_code`/`kiosk_ad`
(`localdb`, container `webview-db`, port 5434). Bang `product`/`category`/`inventory`/
`partner_product_config` **THUC SU nam trong `gold_management`** (da xac nhan truc tiep bang
`\d product_prices` tren ca 2 DB va bang cach tao 1 san pham test qua admin-portal roi kiem tra
no xuat hien trong DB nao). Vi vay `product_prices`/`kiosk_admin_price_history` (2 bang
kiosk-admin dung truc tiep) PHAI nam trong `gold_management`, KHONG phai `localdb` nhu phan con
lai cua app. Ket qua: app nay co **2 Postgres datasource doc lap** - xem
`config/PrimaryPersistenceConfig` (datasource chinh, `localdb`, `@Primary`) va
`golddb/GoldPersistenceConfig` (datasource thu 2, `gold_management`, chi cho
`golddb/entity/ProductPrice.java` va `golddb/entity/PriceHistoryEntity.java`). 2 datasource nay
**hoan toan doc lap** - khong co transaction chung giua 2 ben.

## Cach chay

1. Copy `.env.example` thanh `.env`, dien `DB_PASSWORD` that (xem `Backend/webview/.env` neu test
   local), `GOLD_DB_PASSWORD`/`ADMIN_PORTAL_USERNAME`/`ADMIN_PORTAL_PASSWORD` (xem
   `Backend/admin-portal/.env` neu test local - tai khoan ky thuat nay PHAI da duoc cap quyen
   `partner_code=KIOSK` ben admin-portal truoc) va `KIOSK_JWT_SECRET` rieng (>= 32 byte).
2. Set cac bien moi truong tu `.env` vao shell (khong co thu vien dotenv trong project, giong quy
   uoc cua `Backend/webview`), vi du PowerShell:
   ```powershell
   Get-Content .env | ForEach-Object { if ($_ -match '^([^#=]+)=(.*)$') { [System.Environment]::SetEnvironmentVariable($Matches[1], $Matches[2]) } }
   .\mvnw.cmd spring-boot:run
   ```
   hoac bash:
   ```bash
   set -a; source .env; set +a
   ./mvnw.cmd spring-boot:run
   ```
3. Mo `http://localhost:8090` (hoac `SERVER_PORT` da set).

## Tai khoan dang nhap mac dinh

Luc khoi dong lan dau (bang `kiosk_admin_user` rong), app tu tao 1 tai khoan:

- Username: `kioskadmin`
- Password: **sinh ngau nhien**, chi in ra **console log MOT LAN** luc khoi dong (tim dong
  `KIOSK-ADMIN: da tao tai khoan quan tri mac dinh`). **Ghi lai ngay** - khong luu lai o dau khac,
  khong co cach lay lai qua UI.
- Neu mat mat khau: xoa row trong `kiosk_admin_user` roi restart app de seed lai, hoac tu
  `UPDATE kiosk_admin_user SET password_hash = '<bcrypt hash moi>' WHERE username = 'kioskadmin'`.
- **Doi mat khau ngay sau lan dang nhap dau tien** (chua co man/API doi mat khau trong ban dau
  nay - tu update `password_hash` bang BCrypt neu can).
- Dang nhap sai qua 5 lan trong 15 phut (tinh theo `username + ip`) se bi khoa tam thoi (HTTP
  429) - xem `LoginRateLimiterService`.

## Danh sach API

Tat ca duoi `/api/admin/**` can header `Authorization: Bearer <jwt>` (tru `/api/auth/login`).
`/actuator/health` khong can token (khong nam duoi `/api/admin/*`, chi tra UP/DOWN, khong chi
tiet - dung cho Docker `HEALTHCHECK`).

| Method | Path | Mo ta |
|---|---|---|
| POST | `/api/auth/login` | Dang nhap, tra JWT |
| GET | `/api/admin/devices` | Danh sach may (kiosk-api; kem `online` = dang mo SSE) |
| GET | `/api/admin/devices/{deviceCode}` | Chi tiet 1 may (kiosk-api) |
| POST | `/api/admin/devices` | Tao may moi (PENDING) + phat ma kich hoat. Body `{deviceCode, storeId, storeName}` |
| PUT | `/api/admin/devices/{deviceCode}` | Doi cua hang dat may. Body `{storeId, storeName}` |
| POST | `/api/admin/devices/{deviceCode}/activation-code` | Phat lai ma kich hoat (chi khi PENDING) |
| POST | `/api/admin/devices/{deviceCode}/revoke` | Thu hoi may |
| POST | `/api/admin/devices/{deviceCode}/force-key-rotation` | Ep xoay khoa lan lay token ke tiep (chi khi ACTIVE) |
| DELETE | `/api/admin/devices/{deviceCode}` | Xoa may (va ma kich hoat/khoa/nonce lien quan) |
| GET | `/api/admin/ads` | Danh sach quang cao man cho (kiosk-api) |
| POST | `/api/admin/ads` | Tao quang cao. Body `{name, mediaType, url, order, actionType, productId?, categoryId?, websiteUrl?, displaySeconds, enabled}` - kiosk-api tu tai media tinh checksum/kich thuoc |
| PUT | `/api/admin/ads/{id}` | Sua quang cao |
| DELETE | `/api/admin/ads/{id}` | Xoa quang cao |
| POST | `/api/admin/ads/{id}/toggle` | Bat/tat |
| GET/POST | `/api/admin/banners` | Banner man danh sach san pham (kiosk-api). Body `{code?, name, imagePath, descriptions?, priority, enabled}` |
| PUT/DELETE | `/api/admin/banners/{id}` | Sua / xoa banner |
| POST | `/api/admin/banners/{id}/toggle` | Bat/tat banner |
| GET/PUT | `/api/admin/channel-config` | Cau hinh kenh: gio mo/dong (HH:mm), hotline, idleTimeoutSeconds, stalePriceSeconds, minAppVersion, channelPaused |
| GET | `/api/admin/stores?name=&page=&size=` | Cua hang kenh KIOSK (proxy admin-portal) |
| GET | `/api/admin/stores/provinces` | Tinh/TP (proxy) |
| PATCH | `/api/admin/stores/{id}/active` | Bat/tat cua hang CHI cho kenh KIOSK. Body `{active}` |
| GET | `/api/admin/limits` | Han muc so luong/giao dich kenh KIOSK (proxy) |
| PATCH | `/api/admin/limits/{id}/quantity` | Sua han muc. Body `{limitQuantity}` (> 0) |
| PATCH | `/api/admin/limits/{id}/active` | Bat/tat han muc. Body `{active}` |
| GET | `/api/admin/products?categoryCode=&page=&size=` | Danh sach san pham (proxy admin-portal, kenh KIOSK) |
| GET | `/api/admin/products/{id}` | Chi tiet 1 san pham (proxy) |
| POST | `/api/admin/products` | Tao san pham (proxy, tu gan partnerConfig KIOSK) |
| PUT | `/api/admin/products/{id}` | Sua san pham (proxy) |
| DELETE | `/api/admin/products/{id}` | Xoa san pham (proxy sang admin-portal, KHONG con la soft-delete cua kiosk-admin) |
| POST | `/api/admin/products/{id}/toggle-visibility` | Dao hien/an cho kenh KIOSK (proxy PATCH .../active) |
| GET | `/api/admin/categories?page=&size=` | Danh sach nhom san pham (proxy, kenh KIOSK) |
| POST | `/api/admin/categories` | Tao nhom san pham moi (proxy) |
| PUT | `/api/admin/categories/{id}` | Sua ma/ten/mo ta (giu nguyen hien/an KIOSK; kenh khac khong bi dong toi) |
| PATCH | `/api/admin/categories/{id}/visibility` | Hien/an nhom tren App (chi kenh KIOSK). Body `{visible}` |
| PUT | `/api/admin/categories/priority` | Sap xep nhom. Body `[{categoryId, priority}]` - thu tu dung chung moi kenh |
| DELETE | `/api/admin/categories/{id}` | Go nhom khoi kenh KIOSK |
| GET | `/api/admin/inventory?productName=&page=&size=` | Danh sach ton kho (proxy, kenh KIOSK) |
| PUT | `/api/admin/inventory/{id}` | Cap nhat DINH MUC ton kho trong ngay (proxy, KHONG phai so con lai) |
| GET | `/api/admin/inventory/{id}/history?page=&size=` | Lich su ton kho (proxy) |
| GET | `/api/admin/prices/{productCode}` | Gia hien tai (ghi/doc truc tiep DB `product_prices`, xem phan gia o tren) |
| PUT | `/api/admin/prices/{productCode}` | Sua gia ban ra/mua vao (ghi truc tiep + ghi `kiosk_admin_price_history`) |
| GET | `/api/admin/prices/{productCode}/history?page=&size=` | Lich su sua gia (chi doc `kiosk_admin_price_history`) |
| GET | `/api/admin/device-error-logs?deviceCode=&page=&size=` | Log loi may kiosk (chi doc, tu kiosk-api) |
| GET | `/api/admin/audit-logs?page=&size=` | Nhat ky hanh dong admin (chi doc) |
| GET | `/api/admin/transaction-logs?page=&size=` | Log goi API (chi doc, bang `transaction_log` co san) |
| GET | `/api/admin/price-update-logs?page=&size=` | Log cap nhat gia (chi doc, bang `price_update_logs` co san) |

Cac endpoint phan trang (`page`/`size`) mac dinh `page=0`, `size=50`, `size` bi cap toi da 200 o
tang server de tranh lay qua nhieu du lieu 1 lan.

**Nguon du lieu (tu 2026-10-16):** may, quang cao, banner, cau hinh kenh va log loi may doc/ghi o
**kiosk-api** qua `/internal/ops/*` (header `X-Ops-Key`, cau hinh `KIOSK_API_BASE_URL` /
`KIOSK_API_OPS_KEY`); moi lan ghi kiosk-api tu ban SSE `playlist-changed`/`config-changed` nen App cap
nhat ngay. San pham, gia, ton, nhom, cua hang, han muc la cua **admin-portal**; sua xong
`CatalogChangeNotifyFilter` bao kiosk-api (`/internal/ops/catalog-changed`) de App nhan trong vai giay.
Cac class cu ghi thang DB webview (`KioskAdService`, `KioskDeviceService`, `DeviceErrorLogService`,
`WebviewClient`, entity/repo `Kiosk*`) khong con duoc dung - cho xoa.

Gioi han hien tai: chua sua duoc ten/dia chi cua hang (admin-portal bat buoc phuong/xa hop le ma bang
phuong/xa dang rong) - chi bat/tat theo kenh; thu tu nhom la `priority` dung chung moi kenh.

## LUU Y QUAN TRONG - khong chay song song voi module kiosk cua admin-portal

`kiosk-admin` va module kiosk trong `Backend/admin-portal` (neu chua bi go) deu ghi thang vao
**CUNG bang** `kiosk_device` / `kiosk_activation_code` trong DB `localdb` - **khong co
transaction/lock nao dong bo 2 service khac nhau**. Neu 2 nguoi thao tac dong thoi tu 2 web quan
tri khac nhau (vi du 1 nguoi phat lai ma kich hoat tren admin-portal, nguoi kia tren kiosk-admin
cung luc cho cung 1 may), co the xay ra race condition (VD: ca 2 cung tao ma kich hoat, chi ma moi
nhat con hieu luc nhung nguoi dung khong biet ma nao la ma dang dung).

Day la rui ro nho (thao tac quan tri kiosk khong thuong xuyen dong thoi) chu khong phai bug can
sua ngay - **nguoi dung tu quyet dinh** co go module kiosk cu trong admin-portal hay khong. Neu
chua go, khuyen nghi: chi dung MOT trong hai web de quan ly kiosk tai mot thoi diem.

Tuong tu, bang `product` cung la bang CHUNG voi webview/admin-portal - vi vay `ProductService`
chi **soft delete** (khong bao gio hard-delete) de tranh lam mat du lieu ma cac service khac co
the dang tham chieu toi (vi du don hang cu tro toi `product_id`).

## Da test that (2026-09-23)

- Build: `./mvnw.cmd -q -DskipTests compile` - PASS.
- Chay that voi DB Postgres that (`docker` container `webview-db`, `localhost:5434`, `localdb`).
- Seed tai khoan mac dinh luc khoi dong lan dau - OK, log ra password.
- `POST /api/auth/login` - nhan JWT hop le.
- `POST /api/admin/devices` tao may moi - nhan activation code, ghi vao DB that.
- `GET /api/admin/devices` - thay may vua tao (va cac may test truoc do tu webview/admin-portal,
  xac nhan dung chung 1 DB/bang).
- `GET/POST /api/admin/ads` - CRUD hoat dong tren bang `kiosk_ad` that.
- `DELETE` may va quang cao test - da don dep sau khi test.
- Mo UI bang browser that, dang nhap thanh cong, dashboard (2 tab May Kiosk / Quang cao) render
  dung.

### Cap nhat (2026-10-09) - them San pham / Category / Log loi thiet bi / Audit log / Log giao
dich-gia + hardening bao mat

- Build: `./mvnw.cmd -q -DskipTests compile` - PASS (chua test runtime voi DB that trong lan them
  nay - khong co Postgres trong moi truong lam viec luc build; code da duoc doc lai ky, ten cot
  khop chinh xac voi mo ta schema).
- Them Product/Category/KioskDeviceErrorLog/KioskAdminAuditLog/TransactionLog/PriceUpdateLog
  (entity, repository, service, controller) theo dung pattern co san.
- Them audit log cho moi endpoint mutate hien co (devices, ads) va endpoint moi (products), cong
  voi LOGIN_SUCCESS/LOGIN_FAILURE.
- Sua bug `GlobalExceptionHandler`: khong con lo chi tiet exception ra client (tra message chung
  "Loi he thong, vui long thu lai sau"), them handler rieng cho `MethodArgumentNotValidException`
  tra HTTP 400 dung thay vi roi xuong 500.
- Them `LoginRateLimiterService` (chong brute-force, in-memory) va `SecurityHeadersFilter` (header
  bao mat cho toan bo response).
- Them `spring-boot-starter-actuator`, chi expose `/actuator/health`.
- Them `Dockerfile`/`.dockerignore` (port 8090, healthcheck `/actuator/health`).
- Them 4 tab UI moi (San pham, Log loi thiet bi, Audit log, Log giao dich/gia) vao
  `index.html`/`app.js`/`style.css`, giu dung pattern/khong build step cua UI co san.

### Cap nhat (2026-10-15) - San pham/nhom-san-pham/ton-kho chuyen sang proxy admin-portal + gia
truc tiep DB + 2 datasource

- Go bo hoan toan `entity/Product.java`, `entity/Category.java`, `repository/ProductRepository.java`,
  `repository/CategoryRepository.java`, `service/ProductService.java` (ban cu), `service/CategoryService.java`
  (ban cu) - thay bang ban moi proxy sang `Backend/admin-portal` qua `AdminPortalClient`
  (xem phan "San pham / nhom san pham / ton kho / gia" o tren).
- Them `config/AdminPortalProperties`, `service/AdminPortalAuthClient` (dang nhap + cache JWT),
  `service/AdminPortalClient`, `controller/InventoryController`, `service/InventoryService`.
- Them tinh nang Gia thu cong (ngoai le DUY NHAT ghi truc tiep DB chung): `golddb/entity/ProductPrice.java`,
  `golddb/entity/PriceHistoryEntity.java`, `golddb/repository/ProductPriceRepository.java` (upsert
  qua `ON CONFLICT`), `golddb/repository/PriceHistoryRepository.java`, `service/ProductPriceService.java`,
  `controller/PriceController.java`.
- Them datasource THU 2 (`config/PrimaryPersistenceConfig`, `golddb/GoldPersistenceConfig`) -
  app nay tu nay dung **2 Postgres datasource doc lap** (xem phan ben tren). Them bang bootstrap
  `kiosk_admin_price_history` (DB `gold_management`) vao `KioskAdminApplication.main()`.
- Them bien moi truong `GOLD_DB_*` / `ADMIN_PORTAL_*` vao `application.yml`/`.env.example`.
- Cap nhat UI: tab "San pham" doi form (code/categoryId bat buoc, bo cac truong exchangeRateId/
  serviceProfile/serviceCode khong con dung toi), them nut "Gia"/"Ton kho" tren moi dong va 3
  modal moi (tao nhom san pham, sua gia + lich su, sua dinh muc ton kho + lich su).
- **Da test THAT, end-to-end, voi admin-portal dang chay thuc** (`http://localhost:8386`, tai
  khoan `admin`/`TestAdmin@123` da duoc cap quyen `partner_code=KIOSK`), KHONG chi build:
  - `GET /api/admin/categories`, `/api/admin/products`, `/api/admin/inventory` - tra dung du lieu
    that (san pham test `VT10K001`, nhom `VONGTAY`).
  - `POST /api/admin/categories` - tao nhom moi that qua admin-portal, xac nhan co `id` moi.
  - `PUT /api/admin/inventory/{id}` (quantity 10->11) - xac nhan `originalQuantity`/
    `remainingQuantity` doi dung, VA xac nhan Redis key `INVENTORY_QTY:KIOSK:VT10K001` = 11 qua
    `docker exec gold-redis redis-cli`, VA admin-portal tu ghi 1 dong lich su (`GET
    /api/admin/inventory/{id}/history`).
  - `PUT /api/admin/prices/VT10K001` - xac nhan `product_prices` (DB `gold_management`) doi gia,
    VA 1 dong moi trong `kiosk_admin_price_history` (old/new price/buy_price dung), VA 1 dong
    trong `kiosk_admin_audit_log` (action `PRICE_UPDATE`) - ca 2 co che audit cung ghi dung.
  - `POST /api/admin/products/{id}/toggle-visibility` goi 2 lan: **PHAT HIEN VA SUA 1 BUG THUC**
    trong qua trinh test - admin-portal PATCH `.../active` CHI doi `partnerConfigs[].isActive`
    cho tung kenh, KHONG doi truong `invisible` top-level cua san pham. Logic toggle ban dau doc
    `invisible` top-level de quyet dinh trang thai hien tai -> luon gui `invisible=true`, khong
    bao gio quay lai "hien". Sua: them `ProductResponse.isKioskInvisible()` doc tu
    `partnerConfigs` (tim partnerCode=KIOSK), dung ham nay trong `ProductService#toggleVisibility`
    va trong UI (`p.kioskInvisible` thay cho `p.invisible`). Da test lai: 2 lan toggle lien tiep
    quay dung ve trang thai ban dau.
  - Xac nhan 2 database khac nhau bang `docker exec gold-db psql ...` / `docker exec webview-db
    psql ...`: `product`/`product_prices`/`category`/`inventory`/`partner_product_config` nam
    trong `gold_management` (container `gold-db`, port 5432) - KHONG nam trong `localdb`
    (container `webview-db`, port 5434, noi `kiosk_device`/`kiosk_ad` van o do). Entity
    `ProductPrice` ben `Backend/webview` (neu co) THIEU cot `buy_price` so voi bang thuc trong
    `gold_management` - xac nhan KHONG dung lai entity do.
  - Gap va sua 1 van de moi truong khong lien quan code: loi Postgres `FATAL: invalid value for
    parameter "TimeZone": "Asia/Saigon"` khi chay tren Windows co timezone he thong nay (da co
    ghi chu san trong README, xem phan "Ghi chu Java 21 / mvnw" - chay voi
    `JAVA_TOOL_OPTIONS=-Duser.timezone=UTC`).

## Ghi chu Java 21 / mvnw

`mvnw`/`mvnw.cmd`/`.mvn/` duoc copy tu `Backend/webview` (cung Maven wrapper) - dung
`./mvnw.cmd <goal>` theo dung quy uoc cac repo khac trong BTMH, khong can cai Maven rieng.

Neu gap loi Postgres `FATAL: invalid value for parameter "TimeZone"` luc chay tren may Windows co
timezone he thong la `Asia/Saigon` (Java khong nhan dien alias nay), chay voi
`-Duser.timezone=Asia/Ho_Chi_Minh` (giong Dockerfile cua admin-portal), vi du:
```bash
JAVA_TOOL_OPTIONS="-Duser.timezone=Asia/Ho_Chi_Minh" ./mvnw.cmd spring-boot:run
```
**KHONG dung `UTC`** cho kiosk-admin va admin-portal: `product_prices.updated_at` (cot `timestamp` khong
kem mui gio) duoc ghi bang `now()` theo mui gio phien JDBC, con admin-portal doc lai coi la gio Viet
Nam -> chay UTC thi "Cap nhat luc" tren bang gia App bi lech 7 tieng (da gap that 2026-10-16).