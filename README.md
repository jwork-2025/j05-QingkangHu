[![Review Assignment Due Date](https://classroom.github.com/assets/deadline-readme-button-22041afd0340ce965d47ae6ef1cefeee28c7c493a6346c4f15d667ab976d596c.svg)](https://classroom.github.com/a/iHSjCEgj)
# J05

运行视频见百度网盘链接:https://pan.baidu.com/s/1PDp795kunRdo6fMENdoO2Q?pwd=9emf 

首先本次任务并未全部完成，因为有两个bug还没有解决。

第一个是Enemy死亡之后，会使所有enemy顺序变换，再加上插值，导致敌人的位置异常变换。

第二个是Bullet渲染异常，子弹射速过快的话可能记录不到，而且即使记录到了，由于bug导致无法渲染。

总的来说感觉这次实验超出了我的能力范围，文件读入读出操作不难，但是一大堆没有注释的代码非常难读，而且改了一个地方其他地方也会跟着报错，中间在AI的建议下本来计划实现一个Event作为类似KeyFrame的机制，用于记录一些关键的时间，但是由于实在读不懂loadRecording方法的代码，没法成功还原记录的Event，因此注释掉了该部分代码。

总体而言后续可能会自己实现一个建议的存储方法，例如不利用插值，而利用实时演算，虽然计算会有偏差，但是相比插值而言感觉可读性更高。

限于时间有限，回放实现只完成了Enemy的移动记录。



---

---

本版本采用 LWJGL + OpenGL 实现纯 GPU 渲染，窗口与输入基于 GLFW，文本渲染通过 AWT 字体离屏生成纹理后在 OpenGL 中批量绘制。


## 核心类型与概念

- **Scene（场景）**：一组 `GameObject` 的容器，负责生命周期（`initialize/update/render/clear`）与场景间切换。示例：`MenuScene`, `GameScene`, `ReplayScene`。
- **GameObject（游戏对象）**：由多个 `Component` 组成的实体，管理自身更新与渲染委托。支持自定义 `render()`（如玩家外观组合）。
- **Component（组件）**：面向数据/单体行为的可组合单元，例如：
  - `TransformComponent`：位置/旋转/缩放（本项目主要使用位置与尺寸）
  - `PhysicsComponent`：速度/摩擦/运动学数据（行为由 `PhysicsSystem` 统一处理）
  - `RenderComponent`：基础形状绘制（矩形/圆等，颜色与尺寸）
- **System（系统）**：面向“过程”的批处理逻辑，跨对象统一执行。例如 `PhysicsSystem` 负责所有带 `PhysicsComponent` 的对象物理更新。并行物理计算通过 `ExecutorService` 线程池实现，按批处理提升多核利用。
- **IRenderer/GPURenderer**：渲染后端抽象与 LWJGL 实现，负责窗口/上下文/绘制 API 封装，文本纹理缓存与绘制。
- **EntityFactory**：常用外观/组合的建造器（如 Player、AI 外观），便于游戏与回放共享同一套“预制”。


## 游戏录制/回放机制

- **存储抽象**：`RecordingStorage` 定义录制的读/写/列举接口，默认实现 `FileRecordingStorage`（JSONL 文件）。
- **录制服务**：`RecordingService` 在运行时异步写 JSONL 行：
  - header：窗口大小/版本
  - input：关键输入事件（just pressed）
  - keyframe：周期关键帧（对象位置与可选渲染外观 `rt/w/h/color`）
  - 采用“暖机 + 周期写入 + 结束强制写入”的策略，避免空关键帧
- **回放场景**：`ReplayScene` 读取 JSONL，解析为 keyframe 列表，按时间在相邻关键帧间做线性插值，使用 `EntityFactory`/`RenderComponent` 恢复外观并渲染。


## 编译与运行

1) 下载 LWJGL 依赖与原生库（按平台自动处理）

```bash
./download_lwjgl.sh
```

2) 编译并启动（脚本会自动编译 src/main/java 下所有源码并运行）

```bash
./run.sh
```


## 作业要求

- 参考本仓库代码，完善你自己的游戏：

- 为你的游戏设计并实现“存档与回放”功能：
  - 存档：定义存储抽象（文件/网络/内存均可），录制关键帧 + 输入/事件
  - 回放：读取存档，恢复对象状态并插值渲染，保证外观与行为可见且稳定

提示：请尽量保持模块解耦（渲染/输入/逻辑/存储）。

**重要提醒：尽量手写代码，不依赖自动生成，考试会考！**