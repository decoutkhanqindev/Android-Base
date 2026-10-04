# CLAUDE.md

Hướng dẫn cho Claude Code (và dev) khi làm việc trong project này. **Mọi code mới phải theo đúng rule bên dưới — không trôi về pattern Android/Compose chung chung.** Code hiện có mâu thuẫn với rule → theo rule và báo lại.

## Project

**Android-Base** — base project Android để khởi tạo app mới: Jetpack Compose · Clean Architecture · MVI · Navigation 3 · Koin · Coroutines/Flow.

- Package / namespace / applicationId: `com.decoutkhanqindev.android_base` · single module `:app`
- minSdk 26 · compileSdk/targetSdk 37 · Kotlin 2.4.20 · AGP 9.4.1 · Gradle 9.8.0 · JDK 17 (toolchain)
- Theme: `AppTheme` (mặc định như project Android Studio mới tạo) · XML theme `Theme.App`
- **Skeleton**: chỗ project mới phải implement đều có `// TODO` → Android Studio › View › Tool Windows › **TODO**. Tạo project mới từ base: [README.md](README.md).

## Commands

| Mục đích | Lệnh |
|---|---|
| Verify nhanh (ưu tiên) | `./gradlew :app:compileDebugKotlin` |
| Build / cài debug | `./gradlew assembleDebug` · `./gradlew installDebug` |
| Build release (R8 + shrink) | `./gradlew assembleRelease` — cần signing trong `local.properties` (README) |
| Unit test | `./gradlew testDebugUnitTest` · `./gradlew test --tests "*.ClassName"` |
| Lint | `./gradlew lint` |
| Dependency tree | `./gradlew :app:dependencies --configuration releaseRuntimeClasspath` |

> Build treo → `./gradlew --stop`. Đổi tên/di chuyển thư mục `res/` mà build báo thiếu resource dù file vẫn còn → cache Gradle cũ: `./gradlew --stop` rồi `./gradlew clean assembleDebug --no-configuration-cache`.

## Hard rules ⚠️

- **Base tự đủ — chỉ dựa vào repo này.** Code + file này là nguồn sự thật duy nhất, không có project tham khảo nào khác. KHÔNG clone/fetch/đọc repo GitHub hay project khác trong máy để lấy pattern, kể cả khi trùng tên class/thư viện (trừ khi user chỉ định). Pattern chưa có ở đây → hỏi user.
- **Đọc có chủ đích:** mở file mẫu theo bảng [Đọc gì cho task nào](#đọc-gì-cho-task-nào) + file đang sửa; chỉ mở thêm file mà file đó import/gọi tới. Không quét cả repo, không mở `build/`, `.gradle/`, `.kotlin/`, `.idea/`, source thư viện trong Gradle cache (trừ khi compile báo lỗi API).
- **Git — KHÔNG tự chạy khi chưa được phép:** `git commit` · `git push` / `--force` · `git reset --hard` · `git rebase` · `git merge` · `git branch -D`.
- Thêm dependency, permission Manifest, hoặc xoá code/resource đang dùng → hỏi trước.
- Xong việc phải: `./gradlew :app:compileDebugKotlin` pass + chạy [grep kiểm tra](#18-banned-patterns) ra 0 kết quả.

## Đọc gì cho task nào

Mỗi loại task đã có **file mẫu** trong base — đọc file mẫu + mục tương ứng rồi làm theo đúng pattern đó, không tự nghĩ pattern mới. Đường dẫn code tính từ `app/src/main/java/com/decoutkhanqindev/android_base/`; file Gradle tính từ gốc repo.

| Task | File mẫu | Mục |
|---|---|---|
| Thêm / sửa màn | `presentation/screens/main/` (Screen · Content · ViewModel · `state/`) · `presentation/navigation/AppDestinations.kt` · `presentation/navigation/AppNavDisplay.kt` · `di/AppModule.kt` | 10 · 11 · 19 |
| Nghiệp vụ (model → UseCase → Repository) | `domain/{model,repository,usecase}/Placeholder.kt` · `data/repository/Placeholder.kt` · `di/AppModule.kt` | 8 · 9 · 12 |
| Gọi API · DB local · Firebase · widget | công thức ở mục 1.3 · `gradle/libs.versions.toml` · `app/build.gradle.kts` | 1.3 · 7 |
| Lưu cài đặt (prefs) | `data/local/datastore/DataStoreManager.kt` | 2 |
| Ngôn ngữ · màn Language / Onboarding | `presentation/model/LanguageValue.kt` · `data/local/locale/LanguageManager.kt` · `presentation/MainActivity.kt` · `presentation/screens/splash/SplashScreen.kt` | 5 |
| Ads (placement, show trong màn) | `ads/AdsManager.kt` · `ads/ad_unit/AdUnit.kt` + unit cùng định dạng · `ads/composables/` · `presentation/screens/splash/SplashScreen.kt` (mẫu full-screen) · `app/build.gradle.kts` (ad unit id) | 4 |
| Mạng / offline | `data/network/connectivity/NetworkManager.kt` · `presentation/navigation/AppNavDisplay.kt` | 2 · 3 |
| UI dùng chung · theme | `presentation/components/` · `presentation/theme/` | 14 · 15 · 16 |
| Coroutine · bắt lỗi | `utils/CoroutineExt.kt` | 13 |
| Thư viện | `gradle/libs.versions.toml` · `app/build.gradle.kts` | 7 |

Task không có trong bảng (sửa bug, đổi text…) → tìm đúng file bằng grep tên class/hàm/string, không quét cả repo. Cần pattern mà base chưa có → hỏi user.

---

## 1. Kiến trúc tổng quan

Clean Architecture + MVI trong **1 module `:app`** — ranh giới layer giữ bằng **package**, nên rule import bên dưới là bắt buộc (compiler không chặn hộ).

```
com.decoutkhanqindev.android_base/
├── App.kt                         # Application: Timber, Koin, (TODO) SDK khác
├── di/AppModule.kt                # Koin: managerModule · adsModule · repositoryModule · useCaseModule · viewModelModule
├── ads/                           # AdMob + consent UMP (không nằm trong presentation dù có Compose)
│   ├── AdsManager.kt              #   consent, init MobileAds, currentActivity, isAdShowing + các placement `by lazy`
│   ├── ad_unit/                   #   AdUnit (base) · AdUnitState · Banner/Native/Interstitial/Reward/AppOpenAdUnit
│   └── composables/               #   BannerAdView · NativeAdView (+ NativeLayoutType) · AdLoadingDialog
├── domain/                        # Business thuần Kotlin
│   ├── model/ · repository/ · usecase/
├── data/
│   ├── local/datastore/           # DataStoreManager  — prefs app-shell
│   ├── local/locale/              # LanguageManager   — locale/ngôn ngữ
│   ├── network/connectivity/      # NetworkManager    — trạng thái mạng
│   └── repository/                # XxxRepositoryImpl : XxxRepository
├── utils/                         # CoroutineExt · NavExt (navigateTo) · ContextExt (showToast) · Tag
└── presentation/
    ├── MainActivity.kt            # requestConsent, áp locale (LocalConfiguration/LocalResources), AppTheme
    ├── base/BaseViewModel.kt      # MVI <State, Intent, Effect>
    ├── components/                # Modifiers · AppLottie · dialog/NoInternetDialog
    ├── model/                     # UiModel · LanguageValue · AnimationContentKey
    ├── navigation/                # AppDestinations (NavKey) · AppNavDisplay (+ NoInternetDialog)
    ├── screens/<feature>/         # XxxScreen · XxxContent · XxxViewModel · state/{XxxState, XxxIntent, XxxEffect}
    └── theme/                     # Color · Theme · Type (mặc định Android Studio)
```

`Placeholder.kt` trong `domain/*`, `data/repository` chỉ giữ chỗ + TODO → xoá khi layer đó có file thật. `screens/main/` là khung MVI đầy đủ (6 file) — copy làm khung cho màn mới.

### 1.1 Luồng dữ liệu

```
User ─▶ Content ──onIntent(Intent)──▶ ViewModel ──invoke()──▶ UseCase ──▶ Repository (interface, domain)
          ▲                              │                                   │ Impl ở data, tự withContext
          │ state: StateFlow<State> ─────┤                                   ▼
Screen ◀──┴─ effect: SharedFlow<Effect> ─┘                    DataSource · API · DB · Manager

App-shell (ngôn ngữ, cờ lần đầu mở, mạng, ads) ── Manager ──▶ Screen (koinInject) / ViewModel / Repository (inject thẳng)
```

### 1.2 Dependency rule (hướng import giữa các layer)

```
presentation ──▶ domain ◀── data
      │  ╲         ▲         ╱
      │   ╲─────── di ──────╱          utils: layer nào cũng dùng được; utils KHÔNG import layer nào
      └──▶ Manager ở data/ + ads/   (ngoại lệ được chấp nhận — xem mục 2)
```

| Layer | Được import | KHÔNG được import |
|---|---|---|
| `domain` | Kotlin stdlib · kotlinx.coroutines (`Flow`) · `java.time` · Timber · `utils/` | `android.*` · `androidx.*` · Compose · Koin · `data` · `presentation` · `ads` |
| `data` | `domain` · kotlinx.coroutines · AndroidX DataStore · `utils/` | `presentation` · Compose · `ads` |
| `presentation` | `domain` (UseCase, model) · `presentation/*` · `utils/` · AndroidX/Compose · Manager trong `data/` · `ads/` | `data` Impl / Repository / DataSource |
| `ads` | `NetworkManager` (inject trong `AdsManager`) · `AdsManager` (inject trong `AdUnit`) · `utils/` · `presentation/components` + `presentation/theme` (chỉ trong `ads/composables`) | `domain` · ViewModel · screen |
| `di` | mọi layer | — |
| `utils` | Kotlin · AndroidX thuần | mọi package của app |

Hệ quả bắt buộc:
- **Dữ liệu nghiệp vụ**: ViewModel → UseCase → Repository (interface ở `domain`, Impl ở `data`, bind ở `di`). ViewModel không gọi Repository/DataSource.
- **State hạ tầng app-shell** (ngôn ngữ, cờ lần đầu mở, mạng, ads): đi qua **Manager**, inject thẳng nơi dùng — không bọc UseCase/Repository (mục 2).
- Model qua biên layer phải map: DTO/Entity → domain model (mapper ở `data`) → UiModel (`toUiModel()` ở `presentation/model`).

### 1.3 Phần tuỳ project (chưa có trong base — thêm khi cần)

| Cần | Làm |
|---|---|
| Gọi API | Retrofit + OkHttp + converter kotlinx-serialization. `data/network/api/XxxApiService.kt` (`suspend fun`) + DTO `@Serializable` (`toDomain()` cạnh DTO); Koin `single` cho `OkHttpClient` (interceptor) → `Retrofit` (base URL qua `buildConfigField`) → `XxxApiService`. Repository map `HttpException`/`IOException` → exception domain |
| DB local | Room + KSP: `data/local/database/` (`AppDatabase`, `XxxDao`, `XxxEntity` + `toDomain()`); Dao trả `Flow` cho observe, `suspend` cho 1 lần; Koin `single` cho database + từng Dao |
| Nguồn dữ liệu khác (asset, thuật toán…) | `data/source/<tên>/` — `XxxDataSource` + `XxxDataSourceImpl`, chỉ `data/repository` dùng |
| Firebase (Analytics/Crashlytics/Perf) | plugin `google-services` (+ `crashlytics`, `firebase-perf`), Firebase BOM, `app/google-services.json`; bật collection chỉ ở release trong `MainActivity.onCreate`: `Firebase.crashlytics.isCrashlyticsCollectionEnabled = !BuildConfig.DEBUG` (tương tự analytics/perf) trong `lifecycleScope.launch(Dispatchers.IO)` |
| Màn Language / Onboarding | Khung `screens/main/` + luồng ở [mục 5](#5-ngôn-ngữ--locale) (Splash → Language → Onboarding → Main) + ad ở [mục 4](#4-ads--admob--consent-ump) |
| Widget | Glance — `presentation/widget/<tên>/`: `XxxWidget : GlanceAppWidget` + `XxxWidgetReceiver : GlanceAppWidgetReceiver` + `res/xml/<tên>_widget_info.xml` + `<receiver>` trong Manifest |

---

## 2. Manager — hạ tầng app-shell

Manager = hạ tầng runtime **không phải nghiệp vụ** (prefs của app-shell, locale, kết nối mạng). Không có UseCase/Repository bọc ngoài — UseCase chỉ forward 1 dòng là dư thừa.

| Manager | Vị trí | Cung cấp |
|---|---|---|
| `DataStoreManager` | `data/local/datastore/` | `selectedLangCode: StateFlow<String?>`, `isFirstOpen: StateFlow<Boolean?>`, `saveSelectedLangCode()`, `saveIsFirstOpen()` |
| `LanguageManager` | `data/local/locale/` | `deviceLanguageCode()`, `configurationFor(code)`, `resourcesFor(config)`, `displayNameOf(code, displayIn)` |
| `NetworkManager` | `data/network/connectivity/` | `isAvailable: StateFlow<Boolean>` |
| `AdsManager` | `ads/` | consent, init MobileAds, current activity, `isAdShowing`, placement (mục 4) |

Rule:
- Mỗi manager = **1 class cụ thể** `XxxManager` trong `data/<area>/<tên>/` — không interface + `Impl`, không base class chỉ có 1 lớp con. Koin `single { XxxManager(androidApplication()) }`. API chỉ dùng primitive/ISO code (không nhận/trả enum của presentation).
- Manager là **state holder**: tự tạo scope `CoroutineScope(SupervisorJob() + Dispatchers.IO/Default)`, expose `StateFlow` nóng. Không trả `Result` qua biên manager — lỗi xử lý trong manager (`recoverCatching` → giá trị mặc định, `withContextCatching` → log).
- Nơi dùng:
  - Composable: `val xxxManager: XxxManager = koinInject()` ngay tại nơi dùng — **chỉ ở Screen** (hoặc host như `AppNavDisplay`), Content nhận giá trị qua tham số. Không tự viết CompositionLocal để truyền manager. Activity: `by inject()`.
  - ViewModel: inject manager qua constructor (vd refetch khi `selectedLangCode` đổi).
  - Repository: inject manager (vd đọc `selectedLangCode.filterNotNull().first()` — **1 lần mỗi hàm**, không đọc trong vòng map).
- Signal hạ tầng mới (pin, mạng tính phí…) → manager mới cùng pattern, KHÔNG làm domain repository/UseCase/`object` singleton trong `utils/`.

**DataStoreManager**
- **Mỗi key 1 `StateFlow`, chỉ primitive, chỉ giá trị đã lưu** (không gộp data class — `StateFlow` so sánh cả object, consumer phải `distinctUntilChanged` lại). Không enum, không mapper trong store.
- Thêm key: `const val XXX_KEY` + `DEFAULT_XXX` trong `companion object` → `private val xxxKey = xxxPreferencesKey(XXX_KEY)` → `val xxx = xxxKey.asStateFlow(default = DEFAULT_XXX)` → `fun saveXxx(value) { edit { it[xxxKey] = value } }`.
- Flow khởi đầu `null` = "chưa đọc xong" (`SharingStarted.Eagerly`). Cần chờ giá trị thật → `filterNotNull().first()`; UI tự quyết khi `null`.
- `save*` là `fun` thường, launch trên scope của manager → **không bị huỷ khi user rời màn** (không save qua `rememberCoroutineScope`). Không set giá trị tạm — chờ DataStore emit lại.
- Phải là **1 instance** (Koin `single`) — `preferencesDataStore` delegate trùng file sẽ crash.

**NetworkManager** — chi tiết bắt buộc giữ:
- `callbackFlow` quanh `registerDefaultNetworkCallback`; "có mạng" = `NET_CAPABILITY_INTERNET && NET_CAPABILITY_VALIDATED` (Wi-Fi không ra Internet = mất mạng).
- Emit từ `onCapabilitiesChanged` + `onLost` + 1 lần đọc đầu; **không** emit ở `onAvailable` (chưa có capabilities). `onLost` gửi `false` thẳng — **không** đọc `activeNetwork` trong callback (vẫn trả network đang mất).
- `debounce { if (it) 0 else 500ms }` → chuyển Wi-Fi ↔ 4G không nháy dialog; `distinctUntilChanged`; `recoverCatching { emit(true) }` (observer hỏng không được khoá user sau dialog); `stateIn(WhileSubscribed(5_000), initialValue = true)`.
- Cần permission `ACCESS_NETWORK_STATE`.

---

## 3. Mạng — NoInternetDialog

- "Mất mạng" được **quan sát**, không suy ra từ request lỗi. `AppNavDisplay` lấy `NetworkManager` bằng `koinInject()`, collect `isAvailable` và render `if (!isNetworkAvailable) NoInternetDialog()` sau `NavDisplay` → phủ mọi màn.
- `NoInternetDialog` không tắt được (Back/ngoài vùng), 1 nút mở `Settings.Panel.ACTION_INTERNET_CONNECTIVITY` (API 29+) / `ACTION_WIRELESS_SETTINGS`; tự biến mất khi có mạng lại.
- Màn hình không cần lỗi riêng "offline" — request lỗi hiện lỗi chung, dialog là tín hiệu offline duy nhất.
- App chạy được offline → bỏ dòng `NoInternetDialog()` trong `AppNavDisplay`.

---

## 4. Ads — AdMob + consent UMP

### 4.1 AdsManager & consent
- **`AdsManager`** (`KoinComponent` + `ActivityLifecycleCallbacks` + `Tag`; Koin `single { AdsManager() }`, tạo lazy) — **1 class** chứa toàn bộ hạ tầng ads + khai báo placement, không base class. Member của instance:
  - `application`, `networkManager` lấy bằng `by inject()` (không qua constructor);
  - consent: `requestConsent(activity)`, `isConsentGathered`, `canRequestAds`;
  - init MobileAds: `isMobileAdsInitialized` (idempotent qua `AtomicBoolean`);
  - `isNetworkAvailable`, `isAdShowing` (`internal set`), `currentActivity`, lifecycle callback, test device ids, `scope` (`private`, `CoroutineScope(SupervisorJob() + Dispatchers.Main)` — chỉ cho consent/init MobileAds; init tự chuyển sang `Dispatchers.IO` bên trong).
- State nằm trên **instance `AdsManager`** (Koin `single` → 1 instance), không dùng `companion object`. Màn hình đọc `adsManager.isConsentGathered` / `isMobileAdsInitialized` / `canRequestAds`; `adsManager.isAdShowing` là nguồn duy nhất cho "đang có ad full-screen" (dùng cho app-open khi resume…).
- `AdsManager` đăng ký `ActivityLifecycleCallbacks` trong `init` (1 lần — chỉ có 1 `AdsManager`, không unregister) để giữ `currentActivity` (form consent dùng).
- `MainActivity.onCreate` gọi `adsManager.requestConsent(this)` ngay sau `super.onCreate`.
- Consent xin **mỗi lần mở app** (rule của Google) qua `requestConsent(activity)`, cờ `isConsentRequested` theo process (Activity recreate không tạo thêm collector, process mới thì xin lại): collect `networkManager.isAvailable` → mỗi lần có mạng gọi `gatherConsent()` (offline lần đầu tự thử lại khi có mạng). Không dựa vào `onActivityCreated` — AdsManager tạo lazy nên ra đời sau callback đó.
- `gatherConsent`: nếu consent cũ còn hiệu lực (`canRequestAds()`) → init SDK ngay song song; `requestConsentInfoUpdate` → `loadAndShowConsentFormIfRequired`. **Cả nhánh thành công lẫn lỗi** đều về `consentGatheringComplete()` → init nếu `canRequestAds` + bật `isConsentGathered` ⇒ luồng luôn kết thúc, Splash không treo.
- `MobileAds.initialize` chạy `Dispatchers.IO`, idempotent qua `AtomicBoolean`. Test device: hash trong `local.properties` `admob.test.device.ids=hash1,hash2` (không commit); debug thêm `DEBUG_GEOGRAPHY_EEA` để thử form ở mọi nơi.
- API cho màn hình: `isConsentGathered: StateFlow<Boolean>`, `isMobileAdsInitialized: StateFlow<Boolean>`, `canRequestAds: Boolean`, `isAdShowing`. Bảng quyết định (Splash đang làm):

| consentGathered | canRequestAds | initialized | Làm |
|---|---|---|---|
| false | — | — | chờ (form có thể đang hiện) |
| true | false | — | đi tiếp không ads |
| true | true | false | chờ |
| true | true | true | `interSplash.load()` → LOADED: `show` · FAILED: đi tiếp |

- Nội dung form consent cấu hình ở AdMob console (Privacy & messaging) cho App ID thật — chưa publish thì UMP lỗi 3 *publisher misconfiguration* (luồng vẫn đi tiếp). Chưa có mục "quản lý quyền riêng tư" (`privacyOptionsRequirementStatus` / `showPrivacyOptionsForm`) — Google yêu cầu cho user EEA, thêm khi review policy cần.

### 4.2 AdUnit
- `floors: List<Pair<adUnitId, name>>` xếp **high floor trước** (`listOf(HIGH_ID to "inter_home_high", ALL_ID to "inter_home_all")`); placement 1 id = list 1 phần tử. Mỗi `load()` bắt đầu lại từ floor 0; fail → floor kế; hết floor → `FAILED`. Không lặp id trong list.
- `AdUnit(floors)` là `KoinComponent`, lấy `AdsManager` bằng `private val adsManager: AdsManager by inject()` → từ `AdsManager` unit chỉ dùng `isAdShowing` (ghi) và `canRequestAds` / `isNetworkAvailable` (đọc trong `load()`). Constructor chỉ nhận `floors` — không truyền provider/manager/callback.
- Mỗi unit có **scope riêng** `CoroutineScope(SupervisorJob() + Dispatchers.Main)` — Mobile Ads SDK bắt buộc load/show trên **main thread**, tuyệt đối không chạy request trên `Dispatchers.IO`.
- Unit full-screen (Interstitial/Reward/AppOpen) gán `isAdShowing` (property của `AdUnit` trỏ thẳng tới `adsManager.isAdShowing`): `true` khi `onAdShowedFullScreenContent`/`onAdImpression`, `false` khi đóng/lỗi hiển thị — caller không tự bật/tắt `isAdShowing`; callback của `show()` (`onAdClosed`…) chỉ để điều hướng/cập nhật UI.
- `load()`: guard `LOADING`/`LOADED` → no-op (giữ preload) · `resetWaterfall()` · không consent → `FAILED` · không mạng → `FAILED` · request. `NONE`/`FAILED`/`IMPRESSION` đều load lại được. Không có state `NO_NETWORK`, không tự retry khi có mạng — caller gọi `load()` lại.
- ⚠️ Ad full-screen (Interstitial/Reward/AppOpen) **không** gọi `load()` lại khi đang `IMPRESSION` (đang hiện) — ad mới sẽ bị `onAdDismissed…` xoá; cần guard riêng ở caller.
- Mỗi request bọc `withTimeout(AdUnit.LOAD_TIMEOUT = 20s)` — timeout = fail thật. Generation counter bỏ kết quả về muộn.
- `release()`: bỏ kết quả đang chờ (generation), giải phóng object SDK, về `NONE` — unit singleton load lại được ở lần vào màn sau. `destroy()` (terminal): `scope.cancel()` + `release()` — huỷ scope riêng của unit (hiện chưa có chỗ gọi).
- Subclass mới chỉ implement `requestLoad(context, generation)` (callback SDK → `suspendCancellableCoroutine`) + `releaseAd()`; `catch (TimeoutCancellationException)` phải đứng trước `catch (CancellationException) { throw e }`.
- Log: mọi action của unit (Loading/Loaded/Timeout/Failed/Showed/Impression/Closed/Released…) gọi `log("Action")` của `AdUnit` → `inter_splash_all (ca-app-pub-…/…) - Loading` (tag = tên class unit). Không gọi `Timber` trực tiếp trong unit con — log luôn phải kèm ad unit id để biết floor nào đang load/hiện.

### 4.3 Placement & id
- Mỗi placement = 1 `val xxx by lazy { XxxAdUnit(floors = listOf(BuildConfig.XXX_ALL_ID to "xxx_all")) }` trong `AdsManager` (hiện có `interSplash`). **Không** tạo AdUnit inline trong composable.
- Id mỗi placement = `buildConfigField("String", "<PLACEMENT>_ALL_ID", …)` khai báo ở **cả `release {}` (id thật) lẫn `debug {}` (test id Google)** trong `app/build.gradle.kts` → debug không bao giờ hiện/click ad thật. 5 test id chung `BuildConfig.ADMOB_{BANNER,NATIVE,INTERSTITIAL,REWARDED,APP_OPEN}_TEST_ID` để nối placement trước khi có id thật.
- Manifest: meta-data `com.google.android.gms.ads.APPLICATION_ID` (đang là app id mẫu — TODO thay), `AdActivity` dùng `@style/AdTheme`, `NATIVE_AD_DEBUGGER_ENABLED=false`.

### 4.4 Composable ad
- Nhận `adUnit: () -> XxxAdUnit` (lambda). Tự lo load/release bằng **1** `DisposableEffect(adUnit()) { adUnit().load(context); onDispose { adUnit().release() } }` — key là **chính unit** (không phải `Unit`) để đổi unit trong cùng slot thì release đúng unit cũ. Compose màn = preload, không có bước `preload()` riêng. Effect khai báo **trước** các `return` sớm theo state.
- `BannerAdView`: thêm `LifecycleResumeEffect(adUnit())` → `resume()`/`pause()`.
- `NativeAdView(adUnit, layoutType, modifier, isCloseVisible, onCloseClick)`: `NativeLayoutType.{MEDIA_4_3, MEDIA_16_9, FULL_SCREEN}` → layout XML `res/layout/native_ad_*.xml`. Card ẩn khi `NONE`/`FAILED`; `FULL_SCREEN` không ẩn (giữ nút đóng). Màu áp lúc runtime từ `MaterialTheme.colorScheme` (XML chỉ là baseline light, không `values-night`), drawable phải `mutate()`. Thêm layout = 1 entry enum + 1 nhánh `when`. Composable trùng tên class `NativeAdView` của Google là cố ý — không alias.
- `AdLoadingDialog(adUnit)`: chỉ hiện khi unit `LOADING`, không tự load.
- Chỗ đặt: ad full-screen load/show ở **Screen** (cần `LocalActivity.current`); banner/native nằm trong UI thì Screen truyền slot `@Composable () -> Unit` xuống Content (Content không `koinInject()`).
- Splash có dòng `may_contain_ads` dưới thanh loading (policy ad lúc mở app).

---

## 5. Ngôn ngữ / locale

- `LanguageValue` (`presentation/model/`): 64 ngôn ngữ (`code` ISO + `flag`), `DEFAULT = ENGLISH`, `fromCode(code)`, `displayNamesFor(displayIn, languageManager)`, `sortedForDisplay(deviceLanguageCode, displayNames)` (ngôn ngữ máy → English → theo tên), `labelFor(...)`.
- Ngôn ngữ đã chọn lưu dạng ISO code ở `DataStoreManager.selectedLangCode` (mặc định `"en"`). `MainActivity` collect → `languageManager.configurationFor(code)` + `resourcesFor(config)` → provide `LocalConfiguration`/`LocalResources` ⇒ `stringResource()` đổi ngôn ngữ ngay, không recreate Activity.
- Text UI phải qua `stringResource(...)` hoặc `LocalResources.current.getString(...)` — **không** `context.getString`/`activity.getString` (Context của Activity bỏ qua locale override). Toast: `context.showToast(resources.getString(R.string.x))` với `val resources = LocalResources.current`.
- Ngôn ngữ đang áp dụng trong Compose: `LanguageValue.fromCode(LocalConfiguration.current.locales[0].toLanguageTag())`.
- `LocalConfiguration` bị override từ config của Application (theo ngôn ngữ) → **không** đọc kích thước màn hình từ nó; dùng `LocalWindowInfo` / `BoxWithConstraints`.
- Bản dịch: `res/values-<qualifier>/strings.xml`. Code khác qualifier: `id → values-in`, `he → values-iw`, `zh-hk → values-zh-rHK`, `es-la → values-es-rLA`, `pt-br → values-pt-rBR`, `fil → values-b+fil`; còn lại `values-<code>`. `generateLocaleConfig = true` + `res/resources.properties` (`unqualifiedResLocale=en-US`) sinh danh sách ngôn ngữ cho Android 13+. Thiếu bản dịch → rơi về `values/` (English).
- **Giữ `bundle { language { enableSplit = false } }`** trong `app/build.gradle.kts`: phát hành AAB mà bật split thì máy chỉ nhận ngôn ngữ trùng ngôn ngữ hệ thống → đổi sang ngôn ngữ khác trong app sẽ hiện English (lint `AppBundleLocaleChanges`).
- Màn chọn ngôn ngữ (TODO theo project): lựa chọn tạm là state của màn; chỉ nút Done mới gọi `saveSelectedLangCode(language.code)`. Lần đầu mở app (`isFirstOpen == true`) Splash điều hướng tới màn này; xong onboarding gọi `saveIsFirstOpen(false)`.

---

## 6. Quy ước đặt tên

| Thành phần | Quy ước | Ví dụ |
|---|---|---|
| Destination | `XxxDestination` | `MainDestination`, `DetailDestination(id)` |
| Màn | `XxxScreen` · `XxxContent` · `XxxViewModel` | `MainScreen` |
| MVI | `XxxState` · `XxxIntent` · `XxxEffect` | `MainState` |
| UseCase | `VerbNounUseCase` | `GetDailyMetadataUseCase` |
| Repository | `XxxRepository` / `XxxRepositoryImpl` | |
| DataSource | `XxxDataSource` / `XxxDataSourceImpl` | |
| Manager | `XxxManager` (1 class, không `Impl`) | `NetworkManager` |
| Ad unit / placement | `XxxAdUnit` · placement `<format><Place>` · name `"<format>_<place>_<floor>"` | `interSplash`, `"inter_splash_all"` |
| UiModel | `XxxUiModel` + `fun Xxx.toUiModel()` | |
| Enum giá trị UI | `XxxValue` | `LanguageValue` |
| Component bọc thư viện dùng chung | `AppXxx` | `AppLottie` |
| Package | lowercase, nhiều từ → snake_case | `ad_unit` |
| Token màu | PascalCase mô tả giá trị, alpha `<Base>Alpha<percent>` | `WhiteAlpha30` |
| String | snake_case theo nội dung; prefix màn khi trùng/mơ hồ | `no_internet_connection` |
| Hằng số | `UPPER_SNAKE_CASE` `const val` (`companion object` trong class, `private const val` trong file) | `LOAD_TIMEOUT` |

---

## 7. Dependency (thư viện)

### 7.1 Stack

| Nhóm | Thư viện | Rule |
|---|---|---|
| UI | Compose BOM 2026.09.00 · Material3 · material-icons-extended · ui-tooling | Lib Compose **không ghi version** — BOM quyết định |
| Navigation | navigation3-runtime / -ui 1.2.0 · lifecycle-viewmodel-navigation3 2.11.0 | KHÔNG navigation-compose |
| DI | Koin 4.2.2 (`koin-android`, `koin-androidx-compose`) | KHÔNG Hilt/Dagger |
| Async | kotlinx-coroutines-android 1.11.0 | Flow/StateFlow/SharedFlow — KHÔNG LiveData/RxJava |
| Lifecycle | lifecycle-runtime-ktx / -runtime-compose / -viewmodel-compose 2.11.0 · activity-compose 1.13.0 · core-ktx 1.19.1 | |
| Storage | datastore-preferences 1.2.1 | Qua `DataStoreManager` |
| Ads | play-services-ads 25.5.0 · user-messaging-platform 4.0.0 · constraintlayout 2.2.2 (layout NativeAdView) | Qua `AdsManager` |
| Collections | kotlinx-collections-immutable 0.5.2 | `ImmutableList` trong State/UiModel |
| Serialization | plugin `kotlin-serialization` | `@Serializable` cho NavKey |
| Log | Timber 5.0.1 | KHÔNG `Log.*`/`println` |
| Animation | lottie-compose 6.7.1 | Chỉ qua `AppLottie` — không gọi `LottieAnimation` trực tiếp |
| Test | junit 4.13.2 · kotlinx-coroutines-test · androidx.test · compose ui-test | |

### 7.2 Rule thêm / sửa dependency
1. Mọi version ở `gradle/libs.versions.toml`: version key camelCase (`[versions]`), alias kebab-case `group-artifact` (`[libraries]`), plugin ở `[plugins]` → dùng `libs.xxx` / `alias(libs.plugins.xxx)`. **Không inline version.**
2. Lib AndroidX Compose không ghi version (theo BOM). Nâng Compose = nâng BOM.
3. Ưu tiên lib chính chủ (AndroidX, Kotlin, Google); lib mới phải có lý do + hỏi trước; không thêm lib trùng chức năng.
4. R8: không thêm keep rule rộng cho lib đã có consumer rules (Koin, Coroutines, Compose, Lottie, kotlinx.serialization, Navigation 3, AdMob…). `proguard-rules.pro` chỉ chứa rule cần thiết (hiện: strip call static `Timber.v/d/i`). Đổi rule → test release: điều hướng, khôi phục back stack sau process death, load ad.
5. Secret / id theo môi trường (keystore, test device id, API key) → `local.properties` (gitignore) → `localProperties.getProperty("…")` trong `app/build.gradle.kts` → `signingConfigs` / `buildConfigField`. Ad unit id thật khai báo trong `release {}` của `app/build.gradle.kts`.

---

## 8. Domain layer

- **Model** — `data class` bất biến (`val`), thuần Kotlin. Logic thuần của model đặt trong model/`companion object` — VM gọi, không viết lại.
- **Repository** — `interface XxxRepository`. Lấy 1 lần → `suspend fun`; dữ liệu đổi theo thời gian → `fun observeXxx(): Flow<T>`. Default param khai báo ở interface, Impl không khai lại. Không lộ type của data (DTO, Entity).
- **UseCase** — `VerbNounUseCase`, 1 class / 1 nghiệp vụ, constructor nhận interface repository:

```kotlin
class GetXxxUseCase(private val repository: XxxRepository) {
  suspend operator fun invoke(id: Int): Result<Xxx> = suspendRunCatching { repository.getXxx(id) }
}

class ObserveXxxUseCase(private val repository: XxxRepository) {
  operator fun invoke(): Flow<List<Xxx>> = repository.observeXxx()
}
```

| Loại | Chữ ký | Bắt lỗi |
|---|---|---|
| 1 lần (suspend) | `suspend operator fun invoke(...): Result<T>` | `suspendRunCatching { }` — **không** `runCatching` (nuốt `CancellationException`) |
| đồng bộ | `operator fun invoke(...): Result<T>` | `runCatching { }` |
| observe | `operator fun invoke(...): Flow<T>` | **không** bọc `Result` (lỗi Flow là terminal) — VM dùng `collectCatching` |

- UseCase không giữ state, không chọn dispatcher, không biết UI (`Context`, `@StringRes`, UiModel). Logic dùng chung nhiều VM → UseCase; UseCase được gọi UseCase khác.

## 9. Data layer

- `XxxRepositoryImpl(...) : XxxRepository` trong `data/repository/`; nguồn dữ liệu ở `data/local/…`, `data/network/…`, `data/source/…`.
- **Main-safe tại Repository/Manager**: `withContext(Dispatchers.IO)` cho IO, `Dispatchers.Default` cho CPU — thường qua `withContextCatching(context = Dispatchers.IO, block = { … }, catch = { … })`. UseCase/ViewModel không tự `withContext`.
- Repository được ném exception (UseCase bọc `Result`); không trả `null` để báo lỗi. Map exception thư viện (HTTP, IO, DB…) sang exception có nghĩa tại biên data.
- Observe từ callback → `callbackFlow { …; awaitClose { unregister } }` + `distinctUntilChanged()`; flow cần sống tiếp sau lỗi → `recoverCatching { emit(default) }` trước `stateIn`.
- Mapper DTO/Entity → domain đặt trong `data` (extension `toDomain()` cạnh DTO hoặc `object XxxMapper`).

---

## 10. Presentation — MVI

Luồng một chiều: Content gửi `Intent` → ViewModel → `updateState { }` (State) / `sendEffect()` (Effect) → Screen collect → Content render.

### 10.1 `BaseViewModel<S, I, E>`

| API | Dùng để |
|---|---|
| `state: StateFlow<S>` | UI đọc (`collectAsStateWithLifecycle()`) |
| `effect: SharedFlow<E>` | Sự kiện 1 lần (Screen collect trong `LifecycleStartEffect`) |
| `abstract fun onIntent(intent: I)` | Điểm vào DUY NHẤT từ UI |
| `protected fun updateState(block: S.() -> S)` | Đổi state atomic |
| `protected suspend fun sendEffect(effect: E)` | Gọi trong `viewModelScope.launch { }` |

### 10.2 State · Intent · Effect (mỗi loại 1 file trong `screens/<feature>/state/`)

```kotlin
@Immutable
data class XxxState(
    val isLoading: Boolean = true,
    val items: ImmutableList<XxxUiModel> = persistentListOf(),
    val showDeleteDialog: Boolean = false,
    val error: String? = null,
)

sealed interface XxxIntent {
    data object Refresh : XxxIntent
    data class SelectItem(val id: Int) : XxxIntent
    data object ShowDeleteDialog : XxxIntent
    data object DismissDeleteDialog : XxxIntent
}

sealed interface XxxEffect {
    data class NavigateToDetail(val id: Int) : XxxEffect
    data class ShowMessage(@param:StringRes val messageRes: Int) : XxxEffect
}
```

- **State**: `@Immutable data class`, mọi field có default, list = `ImmutableList`, chỉ type stable. Nguồn sự thật duy nhất của UI — dialog/bottom sheet đang mở cũng là field (`showXxx`) + cặp Intent `ShowXxx`/`DismissXxx`.
- **Intent**: `sealed interface`, đặt theo hành động user; `data object` khi không có tham số.
- **Effect**: `sealed interface`, chỉ cho việc 1 lần không thuộc state (navigate, toast, mở Intent hệ thống, show ad). Màn không có effect → `E = Nothing`.
- Effect là `SharedFlow` không replay: phát khi không có collector (app ở nền) sẽ **mất** → thứ bắt buộc user phải thấy thì đưa vào State.

### 10.3 ViewModel

```kotlin
class XxxViewModel(
    private val getXxx: GetXxxUseCase,
    private val observeYyy: ObserveYyyUseCase,
) : BaseViewModel<XxxState, XxxIntent, XxxEffect>(initialState = XxxState()), Tag {

    private var loadJob: Job? = null

    init {
        load()
        viewModelScope.launch {
            observeYyy().collectCatching(
                block = { yyy -> updateState { copy(yyy = yyy.toUiModel()) } },
                catch = { e -> Timber.tag(tag).e(e.stackTraceToString()) },
            )
        }
    }

    override fun onIntent(intent: XxxIntent) {
        Timber.tag(tag).d("onIntent: $intent")
        when (intent) {
            is XxxIntent.Refresh -> load()
            is XxxIntent.SelectItem -> viewModelScope.launch { sendEffect(XxxEffect.NavigateToDetail(intent.id)) }
            is XxxIntent.ShowDeleteDialog -> updateState { copy(showDeleteDialog = true) }
            is XxxIntent.DismissDeleteDialog -> updateState { copy(showDeleteDialog = false) }
        }
    }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            updateState { copy(isLoading = true, error = null) }
            getXxx()
                .onSuccess { data ->
                    updateState { copy(isLoading = false, items = data.map { it.toUiModel() }.toImmutableList()) }
                }
                .onFailure { e -> updateState { copy(isLoading = false, error = e.message) } }
        }
    }
}
```

- Constructor nhận UseCase (+ Manager khi cần state hạ tầng). KHÔNG nhận `Context` Activity, `NavBackStack`, Repository, DataSource, `AdsManager`.
- `onIntent` là cửa duy nhất UI gọi vào; `when (intent)` exhaustive, không `else`. Hàm public khác chỉ để nhận args khởi tạo ([11.4](#114-truyền-args)).
- Chỉ đổi state qua `updateState { copy(...) }`; đọc state hiện tại bằng `state.value`.
- Tác vụ có thể bị gọi chồng → giữ `Job?` và `cancel()` job cũ trước khi launch.
- Reactive UseCase → `collectCatching(block = …, catch = …)` (tham số đặt tên, `block` trước; cần return sớm trong catch → nhãn `catch@`).
- Map domain → UiModel trong VM (`toUiModel()`), không map trong Content. Text hiển thị cho user → `@StringRes` (Effect `ShowMessage` hoặc field `@StringRes` trong State).

### 10.4 Screen vs Content

```kotlin
@Composable
fun XxxScreen(backStack: NavBackStack<NavKey>) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val viewModel: XxxViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    LifecycleStartEffect(viewModel) {
        val job = lifecycleScope.launch {
            viewModel.effect.collect { effect ->
                when (effect) {
                    is XxxEffect.NavigateToDetail -> backStack.add(DetailDestination(effect.id))
                    is XxxEffect.ShowMessage -> context.showToast(resources.getString(effect.messageRes))
                }
            }
        }
        onStopOrDispose { job.cancel() }
    }

    XxxContent(state = state, onIntent = viewModel::onIntent)
}
```

| File | Trách nhiệm | Không được |
|---|---|---|
| `XxxScreen.kt` | Lấy VM (Koin), collect `state` + `effect`, điều hướng, lấy manager bằng `koinInject()`, load/show ad full-screen, side effect, gửi Intent từ callback ngoài UI | Vẽ UI chi tiết |
| `XxxContent.kt` | UI thuần: nhận `state` + `onIntent` (+ slot ad nếu có); sub-composable `private` cùng file | Biết ViewModel/Koin (`koinInject`, `koinViewModel`)/NavBackStack/Activity; gọi UseCase |
| `XxxViewModel.kt` | Logic UI, gọi UseCase, giữ State | Import Compose UI, `Context`, build text hiển thị |

- **Lifecycle effect**: collect effect/flow 1 lần → `LifecycleStartEffect` (mặc định — chạy khi STARTED, huỷ khi STOPPED). Việc gắn với RESUMED (show ad full-screen khi quay lại, `resume()/pause()` banner, reload khi màn resume) → `LifecycleResumeEffect`. Không dùng `repeatOnLifecycle` hay helper tự viết trong composable.
- Màn không có state/logic riêng (Splash) được phép chỉ có Screen + Content.
- Content dài → tách `XxxYyySection.kt` cùng folder màn; dùng ≥ 2 màn → `presentation/components/`.
- Content thuần nên `@Preview` được (preview `private`, bọc `AppTheme`, `XxxState(...)` mẫu, `onIntent = {}`).

### 10.5 UiModel & giá trị UI
- `@Immutable data class XxxUiModel` ở `presentation/model/`, mapper extension cùng file `fun Xxx.toUiModel()`; list con → `.toImmutableList()`.
- Enum giá trị UI `XxxValue` (vd `LanguageValue`) và type UI dùng chung (`AnimationContentKey { Loading, Error, Content }` làm `targetState` cho `AnimatedContent`) cũng ở `presentation/model/`.

---

## 11. Navigation 3

Lib: `navigation3-runtime`, `navigation3-ui`, `lifecycle-viewmodel-navigation3`. KHÔNG navigation-compose (`NavHost`, `NavController`, route string).

### 11.1 Destination
- `presentation/navigation/AppDestinations.kt`: `@Serializable` + `: NavKey`, tên `XxxDestination`. Không args → `data object`; có args → `data class` với field primitive/`@Serializable` (back stack lưu qua process death). Không nhét domain model — chỉ id/primitive.

### 11.2 NavDisplay
- `AppNavDisplay` = `rememberNavBackStack(SplashDestination)` + `NavDisplay(entries = rememberDecoratedNavEntries(...))` với **đủ 2 decorator**: `rememberSaveableStateHolderNavEntryDecorator()` (thiếu → mất `rememberSaveable`) + `rememberViewModelStoreNavEntryDecorator()` (thiếu → ViewModel không clear khi pop).
- Màn mới = thêm `entry<XxxDestination> { dest -> XxxScreen(...) }`. `onBack` chỉ pop khi `backStack.size > 1`.
- Root `AppNavDisplay` chỉ chứa màn full-screen; tab nằm ở NavDisplay lồng ([11.5](#115-nested-navigation-bottom-tab)). `NoInternetDialog` render sau `NavDisplay`.

### 11.3 Điều hướng
**Chỉ Screen (hoặc host NavDisplay) đụng `backStack`** — ViewModel bắn Effect, Screen thực thi.

| Mục đích | Gọi |
|---|---|
| Thay cả stack (Splash → Main, logout, xong onboarding) | `backStack.navigateTo(dest, preserveState = false)` |
| Đổi tab / single-top | `backStack.navigateTo(dest)` — đã có → pop về đó (args khác thì thay); chưa có → stack thành `[root, dest]` |
| Mở màn chi tiết chồng lên | `backStack.add(dest)` |
| Quay lại | `backStack.removeLastOrNull()` |

⚠️ `navigateTo(dest)` (preserveState = true) **xoá mọi màn giữa root và dest** — mở màn chi tiết nhiều tầng dùng `backStack.add`. Double-tap đã được chặn bởi debounce của `Modifier.onClick`.

### 11.4 Truyền args
1. Entry đọc args từ `dest` → Screen: `entry<DetailDestination> { dest -> DetailScreen(id = dest.id) }`.
2. Vào ViewModel: VM gắn entry (`koinViewModel`) → `koinViewModel { parametersOf(id) }` + `viewModel { (id: Int) -> DetailViewModel(id, get()) }`; VM sống lâu hơn entry (`koinActivityViewModel`) → hàm `setXxx(arg)` gọi trong `LaunchedEffect(arg)`.

### 11.5 Nested navigation (bottom tab)

```kotlin
@Composable
fun MainScreen() {
  val backStack = rememberNavBackStack(HomeDestination)

  Scaffold(
    bottomBar = {
      AppBottomNavBar(
        currentDestination = backStack.lastOrNull(),
        onNavigateTo = { backStack.navigateTo(it) },
      )
    },
  ) { innerPadding ->
    MainNavDisplay(backStack = backStack, modifier = Modifier.padding(innerPadding))
  }
}
```

- `MainNavDisplay(backStack)` = `NavDisplay` (đủ 2 decorator) chỉ chứa `entry<>` của tab; `onBack = { if (backStack.size > 1) backStack.removeLastOrNull() }`.
- `enum class Tab(val destination: NavKey, val icon: ImageVector, @param:StringRes val labelRes: Int)` private trong `AppBottomNavBar`; tab đang chọn so theo `::class`.
- Đổi tab pop entry tab khác → VM tab cần giữ state dùng `koinActivityViewModel()`.

### 11.6 ViewModel scope

| Lấy VM bằng | Gắn với | Dùng khi |
|---|---|---|
| `koinViewModel()` | NavEntry — clear khi entry bị pop | Mặc định |
| `koinActivityViewModel()` | Activity | Màn tab giữ state khi đổi tab; VM dùng chung nhiều màn |

---

## 12. DI — Koin

- `di/AppModule.kt`: `managerModule` · `adsModule` · `repositoryModule` · `useCaseModule` · `viewModelModule` → `appModules`, load ở `App.onCreate()` (`startKoin { androidContext(this@App); modules(appModules) }`).

| Loại | Khai báo | Lý do |
|---|---|---|
| Manager | `single { XxxManager(androidApplication()) }` | State holder, 1 instance |
| AdsManager | `single { AdsManager() }` | `Application`/`NetworkManager` lấy trong `AdsManager` qua `by inject()`; consent bắt đầu từ `MainActivity` (`requestConsent`) |
| DataSource / Repository | `single<XxxRepository> { XxxRepositoryImpl(get()) }` | Giữ cache/kết nối |
| UseCase | `factory { GetXxxUseCase(get()) }` | Không state, rẻ |
| ViewModel | `viewModel { XxxViewModel(get()) }` | Theo ViewModelStore |

- Constructor injection ở mọi layer; không `KoinComponent` trong domain/data. Ngoại lệ (chỉ trong `ads/`): `AdsManager` là `KoinComponent` (`by inject()` `Application` + `NetworkManager`), `AdUnit` là `KoinComponent` (`by inject()` `AdsManager`) — placement khai báo gọn `XxxAdUnit(floors = …)`, không truyền dependency qua constructor.
- Activity: `private val x: X by inject()` (`org.koin.android.ext.android.inject`). Composable: manager qua `koinInject()` tại nơi dùng (Screen/host), không truyền manager xuống Content; VM qua `koinViewModel()`/`koinActivityViewModel()` chỉ ở Screen.
- Tên class `XxxRepositoryImpl` / `XxxDataSourceImpl` chỉ xuất hiện trong `di/` và `data/`.

## 13. Coroutines — `utils/CoroutineExt.kt`

| Hàm | Trả về | Bắt | Dùng ở |
|---|---|---|---|
| `suspendRunCatching { }` | `Result<T>` | `Throwable` | UseCase 1 lần |
| `withContextCatching(context, block, catch)` | `T` | `Exception` | Repository / Manager (đổi dispatcher + map/log lỗi) |
| `Flow<T>.collectCatching(block, catch)` | — (terminal) | `Exception` từ upstream **và** thân `block` | ViewModel collect UseCase reactive |
| `Flow<T>.recoverCatching { }` | `Flow<T>` (intermediate) | `Throwable` | Flow phải sống tiếp (trước `stateIn` trong manager) |

- Hậu tố **`-Catching` = rethrow `CancellationException`, bắt phần còn lại**. Vì vậy ViewModel/Repository **không** tự viết `catch (c: CancellationException) { throw c }` — gọi helper. Ngoại lệ duy nhất: ad unit (phải bắt `TimeoutCancellationException` trước).
- Scope: `viewModelScope` (VM) · `LaunchedEffect`/`rememberCoroutineScope` (Compose) · `lifecycleScope` (Activity, `LifecycleStartEffect`) · manager/ad unit tự tạo scope.
- Dispatcher chọn ở Repository/Manager, không ở VM/UseCase.
- Banned: `GlobalScope`, `runBlocking` trong code app, `Thread.sleep`.

## 14. Compose & theme

- **Theme mặc định như project Android Studio mới**: `AppTheme(darkTheme = isSystemInDarkTheme(), dynamicColor = true)` — light/dark theo hệ thống, dynamic color Android 12+, fallback palette Purple trong `Color.kt`; `Type.kt` chỉ override `bodyLarge`. Thay palette/font theo design (TODO).
- UI dùng **role của MaterialTheme**: `MaterialTheme.colorScheme.<role>` (alpha biến thể viết inline `colorScheme.onSurface.copy(alpha = 0.6f)`), `MaterialTheme.typography.<role>`, `MaterialTheme.shapes.<role>`. Màu cố định ngoài scheme (overlay trong suốt…) → token trong `Color.kt` (`WhiteAlpha30`, `BlackAlpha50`). `Color.Transparent` dùng thẳng. Hex chỉ ở `Color.kt` (ngoại lệ: layout/drawable XML của native ad).
- `MainActivity`: `enableEdgeToEdge()`, khoá dọc, `ComposeUiFlags.isBypassUnfocusableComposeViewEnabled = false` (đặt trước `super.onCreate`, giữ nguyên), Manifest `adjustResize` → mỗi màn tự xử lý inset (`Scaffold` innerPadding, `navigationBarsPadding()`, `imePadding()`).
- **Stability**: State/UiModel `@Immutable`; list → `ImmutableList`; truyền `viewModel::onIntent`; không truyền `MutableState`/ViewModel/`NavBackStack` xuống Content.
- `remember { }` cache; `remember(key) { }` khi input là tham số; `derivedStateOf { }` **chỉ** khi input là Compose `State`:
  ```kotlin
  val showFab by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } } // ✅ input là State
  val isEmpty = remember(items) { items.isEmpty() }                                 // ✅ input là tham số
  ```
- Side effect chỉ trong `LaunchedEffect` / `DisposableEffect` / `LifecycleStartEffect` / `LifecycleResumeEffect`; dọn dẹp ở `onDispose` / `onStopOrDispose` / `onPauseOrDispose`.
- State đổi phải có animation (`AnimatedVisibility`, `AnimatedContent`, `animate*AsState`, `Crossfade`), không snap.
- Component nhận `modifier: Modifier = Modifier` (tham số optional đầu tiên) và áp vào node gốc.

## 15. Reuse — có sẵn, dùng trước khi viết mới

| Cần | Dùng | File |
|---|---|---|
| Lớp cơ sở MVI | `BaseViewModel<S, I, E>` | `presentation/base/` |
| Click (scale 0.95 + ripple + **debounce 300ms**, clip khi truyền shape) | `Modifier.onClick(shape = …, ripple = …) { }` | `components/Modifiers.kt` |
| Skeleton loading | `Modifier.shimmerLoading(backgroundColor, shimmerColor, shape, isEnable)` · `Modifier.shimmerHighlight(...)` | `components/Modifiers.kt` |
| Nền mờ dần (sau nút đáy) | `Modifier.blurBackground(alphas = persistentListOf(0f, 0f, 1f, 1f))` | `components/Modifiers.kt` |
| Dialog mất mạng | `NoInternetDialog()` (đã gắn ở `AppNavDisplay`) | `components/dialog/` |
| Animation Lottie (lặp vô hạn) | `AppLottie(resId = R.raw.x, modifier = …)` | `components/AppLottie.kt` |
| Manager trong Compose | `val x: XxxManager = koinInject()` (Screen/host) | `di/AppModule.kt` |
| Banner / Native / loading ad | `BannerAdView` · `NativeAdView` · `AdLoadingDialog` | `ads/composables/` |
| Danh sách ngôn ngữ | `LanguageValue` | `presentation/model/` |
| Key `AnimatedContent` | `AnimationContentKey` | `presentation/model/` |
| Coroutine an toàn huỷ | `suspendRunCatching` · `withContextCatching` · `collectCatching` · `recoverCatching` | `utils/CoroutineExt.kt` |
| Điều hướng | `NavBackStack.navigateTo(dest, preserveState)` | `utils/NavExt.kt` |
| Toast | `context.showToast(message)` | `utils/ContextExt.kt` |
| Tag log | `: Tag` → `Timber.tag(tag)` | `utils/Tag.kt` |
| Khung màn MVI | copy `screens/main/` | `presentation/screens/main/` |

Quy tắc:
- Trước khi viết composable/util mới → kiểm tra bảng trên. Dùng ở ≥ 2 nơi → `components/` (UI) hoặc `utils/` (không UI); 1 nơi → `private` trong file đó.
- Component dùng chung không tự `koinInject`/`koinViewModel`; nhận data + callback qua tham số.
- Không copy-paste: logic giữa ViewModel → UseCase; UI giữa màn → component; giá trị lặp → token/`const val`.
- Không tạo top-level `val` trung gian dùng 1 lần → inline (trừ token theme, `const val` cho magic number, giá trị tính sẵn để khỏi tính lại mỗi frame).

## 16. Resources

| Loại | Vị trí | Quy tắc |
|---|---|---|
| String | `res/values/strings.xml` (English mặc định) + `values-<qualifier>/` | Không hardcode text trong composable → `stringResource(R.string.x, …)`; placeholder `%1$d`/`%1$s`; ngoại lệ: nội suy thuần số/dấu |
| Icon | Material Icons hoặc `res/drawable/ic_<tên>.xml` (vector) | |
| Ảnh | `res/drawable/img_<tên>.webp` | |
| Lottie | `res/raw/<tên>.json` | `AppLottie(resId = R.raw.x, modifier = …)` (lặp vô hạn) |
| Native ad | `res/layout/native_ad_*.xml` + `res/drawable/bg_*` | XML layout DUY NHẤT được phép |
| Launcher icon | `res/mipmap-anydpi/` + `drawable/ic_launcher_*` | Android Studio › New › Image Asset |

## 17. Logging, comment, test

- Class cần log implement `Tag` → `Timber.tag(tag).d/e(...)`; lỗi `Timber.tag(tag).e(throwable.stackTraceToString())`. `Timber.DebugTree` chỉ plant ở debug.
- Không comment mô tả "làm gì"; UI không comment. Chỉ giữ 1 dòng **tại sao** cho invariant không hiển nhiên (vd `onLost` của NetworkManager, `mutate()` drawable) và `// TODO:` cho chỗ project phải làm.
- Test: `app/src/test/java/<package>/…`; `kotlinx-coroutines-test` (`runTest`, `Dispatchers.setMain` cho VM). Fake tại biên **interface Repository** — không mock UseCase/class final; Manager là class cụ thể nên VM phụ thuộc manager test bằng instrumented test. Ưu tiên: UseCase, ViewModel (Intent → State/Effect), mapper.

## 18. Banned patterns

| Banned | Thay bằng |
|---|---|
| Hilt / Dagger · CompositionLocal tự viết để truyền manager · Koin trong Content | Koin `koinInject()` / `koinViewModel()` tại Screen/host |
| LiveData, RxJava | `StateFlow` / `SharedFlow` / `Flow` |
| navigation-compose | Navigation 3 |
| XML layout (trừ native ad) | Compose |
| `GlobalScope`, `runBlocking`, `Thread.sleep` | scope có lifecycle, `delay` |
| `Log.*`, `println` | Timber |
| `List<T>` / `MutableList` trong State/UiModel | `ImmutableList<T>` |
| ViewModel gọi Repository/DataSource/Impl | UseCase (dữ liệu nghiệp vụ) · Manager (hạ tầng) |
| Manager dạng interface + `Impl` · base class chỉ có 1 lớp con (vd `BaseAds`) | 1 class cụ thể (`XxxManager`, `AdsManager`) |
| UseCase/Repository chỉ forward Manager | Inject Manager trực tiếp |
| `runCatching` trong suspend · `catch (CancellationException)` tự viết | `suspendRunCatching` · `withContextCatching` · `collectCatching` · `recoverCatching` |
| `repeatOnLifecycle` / helper observe tự viết trong composable | `LifecycleStartEffect` (mặc định) · `LifecycleResumeEffect` |
| `context.getString` cho text UI | `stringResource` · `LocalResources.current.getString` |
| Hex `Color(0x…)`, `Color.White/Black`, `RoundedCornerShape(n.dp)` trong UI | `MaterialTheme.colorScheme/shapes` · token `Color.kt` |
| Text hardcode trong composable | `strings.xml` |
| `collectAsState()` | `collectAsStateWithLifecycle()` |
| `Modifier.clickable` ngoài `Modifiers.kt` | `Modifier.onClick` |
| `LottieAnimation` ngoài `AppLottie.kt` | `AppLottie` |
| `Timber` trực tiếp trong ad unit con | `log("Action")` của `AdUnit` (tự kèm ad unit id) |
| Tạo `AdUnit` trong composable · ad id hardcode trong code | placement `by lazy` trong `AdsManager` · `BuildConfig.<PLACEMENT>_ALL_ID` |

Kiểm tra trước khi báo xong — mọi lệnh phải ra **0 dòng**:

```bash
grep -rn "Color(0x\|Color\.White\|Color\.Black" app/src/main/java --include="*.kt" | grep -v "/presentation/theme/"
grep -rn "RoundedCornerShape(" app/src/main/java --include="*.kt" | grep -v "/presentation/theme/"
grep -rn "Log\.[vdiwe](\|println(" app/src/main/java --include="*.kt"
grep -rn "GlobalScope\|runBlocking\|Thread\.sleep" app/src/main/java --include="*.kt"
grep -rn "collectAsState()\|repeatOnLifecycle" app/src/main/java --include="*.kt"
grep -rn "koinInject\|koinViewModel\|koinActivityViewModel\|staticCompositionLocalOf" app/src/main/java --include="*Content.kt"
grep -rn "\.clickable(" app/src/main/java --include="*.kt" | grep -v "/components/Modifiers.kt"
grep -rn "LottieAnimation(" app/src/main/java --include="*.kt" | grep -v "/components/AppLottie.kt"
grep -rn "LiveData\|dagger\.hilt\|androidx\.navigation\.compose" app/src/main/java --include="*.kt"
grep -rn "val .*: \(Mutable\)\?List<" app/src/main/java --include="*State.kt" --include="*UiModel.kt"
grep -rn "catch (.*: CancellationException)" app/src/main/java --include="*.kt" | grep -v "/ads/ad_unit/\|/utils/CoroutineExt.kt"
grep -rn "context\.getString\|activity\.getString" app/src/main/java --include="*.kt"
grep -rln "^import android\.\|^import androidx\." app/src/main/java --include="*.kt" | grep "/domain/"
grep -rn "import .*\.data\.repository\.\|import .*Impl$" app/src/main/java --include="*.kt" | grep "/presentation/"
grep -rn "import .*\.domain\.repository\.\|import .*Impl$" app/src/main/java --include="*ViewModel.kt"
grep -rn "Timber" app/src/main/java --include="*AdUnit.kt" | grep -v "/ad_unit/AdUnit.kt"
grep -rn "ManagerImpl\|BaseAds" app/src/main/java --include="*.kt"
```

---

## 19. Checklist thêm feature mới

1. `domain/model/` — domain model.
2. `domain/repository/XxxRepository.kt` — interface.
3. Nguồn dữ liệu (`data/network/…`, `data/local/…`, `data/source/…`) → `data/repository/XxxRepositoryImpl.kt` (main-safe, map → domain).
4. `domain/usecase/` — UseCase ([mục 8](#8-domain-layer)).
5. `di/AppModule.kt` — `repositoryModule` · `useCaseModule` · `viewModelModule`.
6. `presentation/model/XxxUiModel.kt` (+ `toUiModel()`) nếu cần.
7. `presentation/screens/xxx/` — copy khung `screens/main/` (State · Intent · Effect · ViewModel · Content · Screen).
8. `AppDestinations.kt` — `XxxDestination`; đăng ký `entry<XxxDestination>` ở `AppNavDisplay` (hoặc NavDisplay lồng).
9. Ad cho màn: placement `by lazy` trong `AdsManager` + `<PLACEMENT>_ALL_ID` ở `release {}`/`debug {}`; native/banner truyền slot từ Screen.
10. Text vào `strings.xml` (+ bản dịch nếu project có), màu cố định mới vào `Color.kt`.
11. Xoá `Placeholder.kt` của layer vừa có file thật.
12. `./gradlew :app:compileDebugKotlin` + chạy grep [mục 18](#18-banned-patterns).
