# 验证记录

当前组件为无限加载动画；验证覆盖启停、外观、尺寸、窗口生命周期和无障碍加载语义。

## 构建与设备测试

- AGP 9.5.1、Gradle 9.9.1、SDK 37，JDK 25.0.4。
- Debug / R8 + 资源压缩 Release 构建通过。
- `app` 与 `besselloadingview` 的 Debug Lint：均无问题。
- 组件 AAR、sources JAR、POM 发布到本机 Maven 仓库通过；未发布到公共仓库。
- API 30、API 37 AVD 各运行 4 项设备测试，均通过。

```sh
./gradlew :app:assembleDebug :app:assembleRelease \
  :app:lintDebug :besselloadingview:lintDebug \
  :besselloadingview:connectedDebugAndroidTest \
  :besselloadingview:publishReleasePublicationToMavenLocal
```

设备测试覆盖：

1. 自动加载、重复 start、stop、隐藏/恢复和 detach 时取消动画；兼容系统已关闭动画的情况。
2. 参数校验，以及添加到窗口前调用启停。
3. `wrap_content`、非正方形尺寸、动态相对 padding、极小空间和透明绘制。
4. 样式/加载状态恢复、保留宿主无障碍标签、不暴露百分比范围。

示例已在 API 37 启动并检查截图。几何审查确认四段曲线保留原始控制点和对角中点关系，按实际极值缩放以避免线条被边缘裁切。

## 性能观测

组件稳定动画帧只更新复用的矩阵并绘制一条缓存 Path。不存在 Bitmap 分配、遮罩合成、独立渲染线程或 Handler 绘制循环。系统动画倍率设为 0 后保留静态画面，倍率恢复后继续动画；测试后恢复 AVD 原设置。

使用带本地调试签名的 Release APK，分别采集 8 秒 `dumpsys gfxinfo`。停止、系统关闭动画和进入后台另采集 2 秒：API 30 的三个静止阶段均新增 0 帧；API 37 的后台阶段新增 0 帧，停止/禁用动画的短窗口仍包含 17/10 个系统或过渡帧。设备测试另直接验证动画对象在隐藏、stop 和 detach 时已取消。

在临时诊断 Activity 中，以相同的 180dp 尺寸分别显示组件和系统 ProgressBar，并通过 `FrameMetrics` 观测约 2 秒：

| AVD | 组件 CPU 绘制中位数 / P95 | 系统参考 CPU 绘制中位数 / P95 |
| --- | --- | --- |
| API 30 | 0.081 / 0.354 ms | 8.791 / 15.761 ms |
| API 37 | 0.060 / 0.537 ms | 6.473 / 15.232 ms |

以上为窗口 `DRAW_DURATION`，不包含 GPU 完成时间，且不是同视觉效果的对比。共享宿主同时运行多个 AVD，组件完整帧耗时 P95 达到 118–151 ms，系统参考也达到 72–74 ms。结果只能说明此次采样中 CPU 录制绘制命令成本较低；不能据此宣称流畅帧率、真机性能或相对加速比例。API 23–29 和真实设备尚未实测。

可在目标设备复查整体帧统计：

```sh
adb shell dumpsys gfxinfo com.bluestrom.gao.besselloadingtest reset
# 让示例持续加载，再读取统计
adb shell dumpsys gfxinfo com.bluestrom.gao.besselloadingtest
```

构建工具的传递依赖存在 Gradle 10 / JDK 后续版本弃用提示，不影响当前固定工具链的构建。GitHub Actions 的结果以远程工作流实际运行状态为准。
