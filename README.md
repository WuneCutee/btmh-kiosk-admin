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
(`localdb`), doc/ghi thang cac bang `kiosk_device`, `kiosk_activation_code`, `kiosk_ad` da co san -
khong qua Feign/API noi bo cua webview, khong dong bo du lieu.

## Kien truc / quyet dinh ky thuat

- Spring Boot 3.3.7, Java 21 (khop `Backend/webview`).
- `spring.jpa.hibernate.ddl-auto=validate` cho 3 entity anh xa vao bang co san
  (`KioskDevice`/`KioskActivationCode`/`KioskAd`) - **Hibernate KHONG duoc phep tu doi schema cua
  nguoi khac dang dung**.
- Bang MOI `kiosk_admin_user` (chi de luu tai khoan dang nhap cua web nay) duoc tao bang JDBC
  thuan trong `KioskAdminApplication.main()` (`CREATE TABLE IF NOT EXISTS`, chay TRUOC khi Spring
  khoi tao Hibernate) - can thiet vi `ddl-auto=validate` se kiem tra TOAN BO entity (ke ca
  `KioskAdminUser`) ngay luc build `EntityManagerFactory`, qua som de dung `@PostConstruct` hay
  `ApplicationRunner` khac tao bang kip luc.
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
- UI: HTML/CSS/JS thuan, serve tinh tu `src/main/resources/static/`, khong build step.

## Cach chay

1. Copy `.env.example` thanh `.env`, dien `DB_PASSWORD` that (xem `Backend/webview/.env` neu test
   local) va `KIOSK_JWT_SECRET` rieng (>= 32 byte).
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

## Danh sach API

Tat ca duoi `/api/admin/**` can header `Authorization: Bearer <jwt>` (tru `/api/auth/login`).

| Method | Path | Mo ta |
|---|---|---|
| POST | `/api/auth/login` | Dang nhap, tra JWT |
| GET | `/api/admin/devices` | Danh sach may |
| GET | `/api/admin/devices/{deviceCode}` | Chi tiet 1 may |
| POST | `/api/admin/devices` | Tao may moi (PENDING) + phat ma kich hoat |
| POST | `/api/admin/devices/{deviceCode}/activation-code` | Phat lai ma kich hoat (chi khi PENDING) |
| POST | `/api/admin/devices/{deviceCode}/revoke` | Thu hoi may |
| POST | `/api/admin/devices/{deviceCode}/force-key-rotation` | Ep xoay khoa lan lay token ke tiep |
| DELETE | `/api/admin/devices/{deviceCode}` | Xoa may (va cac ma kich hoat lien quan) |
| GET | `/api/admin/ads` | Danh sach quang cao (sap theo displayOrder) |
| POST | `/api/admin/ads` | Tao quang cao |
| PUT | `/api/admin/ads/{id}` | Sua quang cao |
| DELETE | `/api/admin/ads/{id}` | Xoa quang cao |
| POST | `/api/admin/ads/{id}/toggle` | Dao trang thai ON/OFF |

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

## Ghi chu Java 21 / mvnw

`mvnw`/`mvnw.cmd`/`.mvn/` duoc copy tu `Backend/webview` (cung Maven wrapper) - dung
`./mvnw.cmd <goal>` theo dung quy uoc cac repo khac trong BTMH, khong can cai Maven rieng.

Neu gap loi Postgres `FATAL: invalid value for parameter "TimeZone"` luc chay tren may Windows co
timezone he thong la `Asia/Saigon` (Java khong nhan dien alias nay), chay voi
`-Duser.timezone=UTC`, vi du:
```bash
JAVA_TOOL_OPTIONS="-Duser.timezone=UTC" ./mvnw.cmd spring-boot:run
```
