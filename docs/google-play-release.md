# Google Play 发布指南

正式包名为 `cn.mitoto.shufa`。这是 Play Console 中不可变的 applicationId；Kotlin
源码 namespace 仍是 `com.example.shufa`，不影响 Play 发布。

## 自动化内容

- `scripts/build-release.sh` 从 CI Secret 临时解密 upload keystore，构建并验证签名的
  `app-release.aab`，随后复制到 `dist/`。
- `scripts/publish-play.sh` 和 `fastlane/` 使用 Google Play Developer API 上传已有 AAB。
  默认目标为 internal testing；production 必须设置显式批准变量。
- `.github/workflows/google-play-release.yml` 在 GitHub Actions 的 Ubuntu 22.04 runner
  上以 **Run workflow** 手工触发，提供构建产物和可选的 Play 发布。

构建与发布故意分开：可先用 `publish=false` 构建并下载 AAB，再做人工验收。首次上传前，
请确保 versionCode 高于 Play Console 中所有已上传版本。

## 一次性 Google Play Console 设置

1. 在 Play Console 创建 applicationId 为 `cn.mitoto.shufa` 的应用，并完成首次上架必填
   的商店资料、隐私政策、Data safety、内容分级、目标受众和 App access。自动化脚本只管理
   AAB 与 release，不会跳过审核资料。
2. 在 Google Cloud 创建 service account 并启用 **Google Play Android Developer API**。
   在 Play Console 的 **Users and permissions** 中邀请其 service-account email 并只授予
   所需权限：内部/封闭测试使用 "Manage testing track releases"；生产发布另需
   "Release to production"。
3. 在 GitHub repository Settings -> Environments 创建 `google-play` 与
   `google-play-production`。为后者配置 required reviewers，并在相应 Environment 中添加：

   - `ANDROID_KEYSTORE_BASE64`：Ubuntu 上执行 `base64 -w 0 upload-keystore.jks`
   - `ANDROID_KEYSTORE_PASSWORD`
   - `ANDROID_KEY_ALIAS`
   - `ANDROID_KEY_PASSWORD`
   - `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON_BASE64`：`base64 -w 0 play-service-account.json`

macOS 编码可使用 `base64 < file | tr -d '\n'`。不要提交 `.jks`、service-account JSON 或
任何 Secret 值。

## GitHub Actions 发布

从 Actions -> **Build and publish to Google Play** -> **Run workflow** 开始：

1. 输入递增的 `version_code` 和用户可见的 `version_name`。
2. 首次选择 `internal`，并选择 `validate_only=true` 验证 Play 的服务端校验。
3. 验证成功后，使用相同或更新的版本值、`validate_only=false` 执行真正上传。
4. 仅在已完成测试并经 `google-play-production` reviewer 批准后选择 `production`。需要灰度时，
   选择 `inProgress` 并填写 `user_fraction`（如 `0.1`）。

工作流默认只支持 internal、beta、production；需使用自定义 closed track 时，使用下方命令行方式。

## 本地或其他 CI 调用

```bash
export ANDROID_KEYSTORE_BASE64="..."
export ANDROID_KEYSTORE_PASSWORD="..."
export ANDROID_KEY_ALIAS="..."
export ANDROID_KEY_PASSWORD="..."
export RELEASE_VERSION_CODE=12
export RELEASE_VERSION_NAME=1.0.2
bash scripts/build-release.sh

bundle install
export GOOGLE_PLAY_SERVICE_ACCOUNT_JSON_BASE64="..."
export AAB_PATH="$PWD/dist/shufa-1.0.2-12.aab"
export PLAY_TRACK=internal
export PLAY_VALIDATE_ONLY=true
bash scripts/publish-play.sh
```

验证完成后将 `PLAY_VALIDATE_ONLY=false` 再次运行以提交 release。生产轨道还需要
`PLAY_PRODUCTION_APPROVED=yes`；灰度发布需要 `PLAY_RELEASE_STATUS=inProgress` 与
`PLAY_USER_FRACTION=0.1`。

## 参考

- [Google Play Developer API - Getting started](https://developers.google.com/android-publisher/getting_started)
- [fastlane upload_to_play_store action](https://docs.fastlane.tools/actions/upload_to_play_store/)
