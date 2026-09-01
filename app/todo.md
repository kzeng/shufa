- 重新设计Title栏， 当前是App Logo + 选贴， 修改为 App Logo + 选贴（tab） + 收藏(tab) + 主题选择（Dark/Light 居右）
- 点击收藏(tab)，切到收藏页面（碑帖卡片式列表 和选贴页面风格一致）， 要有取消收藏入口。收藏的内容在版本升级后不应该丢失，除非删除重新安装。
- 选贴页面的详情页，合适的位置增加“收藏图标”按钮。
- 主题选择切换使用图标，不要文字。
- UI优化：选贴详情和收藏详情页，进入碑帖切页界面时，页码点击区和图片整体上移，点击区位置高度和Close X 按钮齐平。
- 版本号升到v0.0.8
- 以上功能待我确认后再 add tag v0.0.8 and add releases 。
------

- 为每个碑帖增加两个字段： tid, source_url
 tid： 由#开头，由字母数字组合的4个长度的唯一字符串（如：#C13D），显示在详情页副标题中，如：作者.年代.字体.tid
 source_url： 是碑帖信息的来源网址，表明碑帖信息文字图片是从哪个网页抓取的。
 source_url 只存在APP数据库中,不要显示到APP界面上。
 source_url 可能有多个网址：
 source_url = [
    url1,
    url2,
    url3,
    ...
 ]
 tid和source_url是一一对应的，这样只要提供 tid，就能找到对应的网页， 即可迅速定位APP 上碑帖信息错误/缺失等问题是网页源信息的问题还是抓取的问题。

- 版本号升到v0.0.9 待我确认后再 add tag v0.0.9 and add releases 。

【v0.0.9 状态 - 2026-09-01，代码已全部完成并在 S90 设备验证通过，尚未提交/打tag/发版】
已完成：
  [x] posts.json：2447 帖全部补齐 tid（#XXXX，唯一，seed 20260901）+ sourceUrl（2404 帖可重建为 https://www.zitiewang.com/shufa/<seg>.htm，43 个旧 slug id 为 []）；清理了 98 个 huangtingjian 帖遗留的 url 字段，全部帖补全 characters，统一字段顺序（id,tid,title,author,dynasty,style,description,imageUrls,characters,sourceUrl）
  [x] 代码：CalligraphyPost 增加 tid + sourceUrl；PostEntity 增加 tid/sourceUrl 列；AppDatabase 导入逻辑读新字段；PostDao 增加 getAll/getFavoriteIds/setFavoriteFlag；PostRepository 新增 ensureTid（网络搜索新增帖自动生成唯一 tid）；新增 TidUtils；SelectViewModel 网络帖 tid="" 待持久化时生成；ViewScreen 详情副标题改为 "作者 · 年代 · 字体 · #XXXX"（去掉了原 N页）
  [x] 版本：build.gradle.kts versionCode 9 / versionName 0.0.9
  [x] DB 升级保留收藏：MIGRATION_5_6（ALTER TABLE 加 tid/sourceUrl 两列，NOT NULL DEFAULT ''）；@Database exportSchema=false（原 true 但无 schemas/ 目录会导致 Room 静默走 destructive 迁移丢收藏）；getDatabase 的 runBlocking 内用 withContext(Dispatchers.IO){ instance.withTransaction{ backfillTidAndSourceUrl } } 回填 tid/sourceUrl 并恢复收藏（注意：suspend DAO 写入必须包在 withTransaction 中，否则报 "no current transaction"）
  [x] 验证（S90，adb 192.168.0.108:34267）：全新安装 DB v6，2447 帖 / 2447 唯一 tid / 2404 帖有 sourceUrl / integrity_check ok；模拟 v0.0.8(v5 DB，3 个收藏) → 升级 v0.0.9：收藏 3 个全保留，收藏页正确显示 3 条；详情页副标题显示 "佚名 · 东汉 · 隶 · #G8OE" 等
待办（Codex 继续）：
  [ ] 待用户确认后 git commit 本次 v0.0.9 改动（当前工作区未提交：8 个修改文件 + 新增 TidUtils.kt）
  [ ] 确认后打 tag v0.0.9 并创建 GitHub release（参考 v0.0.8 流程：debug APK 重命名 shufa-v0.0.9.apk，GitHub API + git credential 取 token，中文 release body）
  [ ] 注意：MIUI 设备 logcat 可能捕获不到 Log.d（不影响功能）；无线 adb 重连若失败，连 USB 后再 adb connect
- 为APP增加碑帖内容(利用网络爬虫skills)
- 来源： 
 楷：http://www.yac8.com/wap/news/list_97.html  列表页面，点击‘下一页’翻页
 行：http://www.yac8.com/wap/news/list_141.html 列表页面，点击‘下一页’翻页
 隶：http://www.yac8.com/wap/news/list_143.html 列表页面，点击‘下一页’翻页
 草：http://www.yac8.com/wap/news/list_142.html 列表页面，点击‘下一页’翻页
 篆：http://www.yac8.com/wap/news/list_144.html 列表页面，点击‘下一页’翻页

点击列表页条目，进入详情页，抓取碑帖文章内容和碑帖图片URL, 点击‘下一页’翻页。

 - 版本号升到v1.0.0 待我确认后再 add tag v1.0.0 and add releases 。