# W Mod Loader 文档

一个为高版本 Minecraft（>= 26.1）服务的 Mod API。

基于 ASM 字节码操作，W Loader 在游戏运行时动态注入代码实现逻辑。

## 📦 开始使用

### 安装

使用 W Loader 需要在 Java 虚拟机参数填写 `-javaagent` 参数。

例如，如果 Jar 路径为 `D:\Download\Snapshot 2.2.1\.minecraft\w_loader-1.2.jar`，你需要填写：
```bash
-javaagent:"D:\Download\Snapshot 2.2.1\.minecraft\w_loader-1.2.jar"
```

### 安装模组

启动游戏后会在同路径生成一个 `mods` 文件夹，将模组放入文件夹即可。

## 🚀 模组开发

### build.gradle 配置

#### 依赖配置

在 `dependencies` 添加 W Loader 依赖：

```gradle
dependencies {
    implementation files("library/26.1-snapshot-8.jar") // Minecraft 本体 Jar
    compileOnly 'com.wzz:w-loader-api:1.2'
    implementation 'com.mojang:datafixerupper:6.0.8'
    implementation 'com.mojang:brigadier:1.3.10'
    implementation 'com.mojang:authlib:7.0.61'
    implementation 'org.joml:joml:1.10.8'
}
```

> **注意**：需要添加 Minecraft 本体 Jar，可以直接将游戏本体拿过来。

#### 仓库配置

```gradle
repositories {
    mavenCentral()
    google()
    maven {
        url = "https://maven.minecraftforge.net/"
    }
    maven {
        name = "SpongePowered"
        url = "https://repo.spongepowered.org/repository/maven-public/"
    }
    maven {
        url = uri("https://maven.fabricmc.net/")
    }
    maven {
        url "https://wozhizhan.github.io/W-Mod-Loader/repository/"
    }
}
```

> Forge/Fabric 的下载源是为了能够正确下载所需依赖。

## 📝 编写模组

### mod.json 配置

在 Jar 根目录创建一个名为 `mod.json` 的文件：

```json
{
  "name": "模组名字",
  "version": "1.0",
  "main": "com.wzz.example.ModMain",
  "mod_id": "模组的ID（用小写，不能用空格）",
  "description": "mod介绍"
}
```

| 字段 | 说明 |
|------|------|
| name | 模组名字 |
| version | 模组版本 |
| main | 模组主类（全限定名） |
| mod_id | 模组的ID（小写，无空格） |
| description | 模组介绍 |

### 主类开发

主类需要实现 `com.wzz.w_loader.annotation.WMod` 接口，以下是示例代码：

```java
@WMod(modId = "test", name = "Forever test Sword")
public class ModMain {
 
    // 监听事件：服务器时刻更新
    @Subscribe
    public void onServerTick(ServerTickEvent event) {
        for (ServerPlayer serverPlayer : event.getServer().getPlayerList().getPlayers()) {
            serverPlayer.hurtServer(serverPlayer.level(), serverPlayer.level().damageSources().fellOutOfWorld(), 1f);
            // 对所有玩家造成 1 点虚空伤害
        }
    }
     
    // 监听事件：注册
    @Subscribe
    public void onRegister(RegistryEvent event) {
        // 注册一个测试物品
        Registries.ITEMS.register("test:test", () -> new Item(new Item.Properties().durability(0)
                .setId(Registries.ITEMS.key("test:test"))) {
           
            @Override
            public void hurtEnemy(ItemStack itemStack, LivingEntity mob, LivingEntity attacker) {
                mob.discard();
                super.hurtEnemy(itemStack, mob, attacker);
            }
        });
    }
     
    // 监听事件：创造栏
    @SuppressWarnings("unchecked")
    @Subscribe
    public void onCreativeTab(CreativeTabEvent event) throws Exception {
        // 将测试物品加入 Combat 创造栏
        Item sword = Registries.ITEMS.get("test:test").orElse(null);
        if (sword == null) return;
        
        Field field = CreativeModeTabs.class.getDeclaredField("COMBAT");
        field.setAccessible(true);
        ResourceKey<CreativeModeTab> tab = (ResourceKey<CreativeModeTab>) field.get(null);
        event.addToTab(tab, sword);
    }
}
```

## 📚 注册系统

由于 W Loader 正在开始阶段，API 并不完善，注册代码只能写在模组主类。监听 `com.wzz.w_loader.event.events.RegistryEvent` 事件，在此注册内容。

### 可注册的内容

```java
Registries.ITEMS          // 注册物品
Registries.BLOCKS         // 注册方块
Registries.TABS           // 注册创造栏
Registries.ENTITIES       // 注册实体
Registries.MOB_EFFECTS    // 注册药水效果
Registries.MENUS          // 注册菜单
Registries.PARTICLES      // 注册粒子
Registries.BLOCK_ENTITIES // 注册方块实体
Registries.ATTRIBUTES     // 注册属性
Registries.RECIPES        // 注册配方
Registries.SOUNDS         // 注册声音
```

### 特殊注册事件

```java
EntityAttributeCreationEvent   // 注册实体属性
EntityRegisterRenderersEvent   // 注册实体/方块实体渲染
```

> 资源文件放置的位置与 Forge/Fabric 相同。

## 🎯 事件系统

W Loader 提供丰富的事件系统，使用 `@Subscribe` 注解监听事件。

### 事件列表

#### 世界与实体事件
```java
com.wzz.w_loader.event.events.BlockBreakEvent                 // 破坏方块事件
com.wzz.w_loader.event.events.SetBlockEvent                   // 放置方块事件
com.wzz.w_loader.event.events.SetBlockEntityEvent             // 设置方块实体事件
com.wzz.w_loader.event.events.ExplosionEvent                  // 爆炸事件
com.wzz.w_loader.event.events.LevelEvent.Load                 // 世界加载事件
com.wzz.w_loader.event.events.LevelEvent.Unload               // 世界卸载事件
```

#### 实体生命周期
```java
com.wzz.w_loader.event.events.EntityJoinLevelEvent            // 实体加入世界事件
com.wzz.w_loader.event.events.EntityRemoveEvent               // 实体清除事件
com.wzz.w_loader.event.events.EntityDiscardEvent              // 实体被丢弃事件
com.wzz.w_loader.event.events.EntityKillEvent                 // 实体Kill事件
com.wzz.w_loader.event.events.EntityTickEvent                 // 实体时刻更新事件
com.wzz.w_loader.event.events.EntityPushEvent                 // 实体挤压事件
com.wzz.w_loader.event.events.EntitySetLevelEvent             // 实体设置世界事件
com.wzz.w_loader.event.events.EntitySetPosEvent               // 实体设置坐标事件
```

#### 生物事件
```java
com.wzz.w_loader.event.events.LivingAttackEvent               // 实体攻击事件
com.wzz.w_loader.event.events.LivingDeathEvent                // 实体死亡事件
com.wzz.w_loader.event.events.LivingHurtEvent                 // 实体受伤事件
com.wzz.w_loader.event.events.LivingHealEvent                 // 实体治疗事件
com.wzz.w_loader.event.events.LivingSetHealthEvent            // 实体设置生命值事件
com.wzz.w_loader.event.events.LivingGetHealthEvent            // 实体获取生命返回值事件
com.wzz.w_loader.event.events.LivingFallEvent                 // 实体摔落事件
com.wzz.w_loader.event.events.LivingJumpEvent                 // 实体跳跃事件
com.wzz.w_loader.event.events.LivingSwingEvent                // 实体挥手事件
com.wzz.w_loader.event.events.LivingTickEvent                 // 实体时刻更新事件
com.wzz.w_loader.event.events.LivingKnockBackEvent            // 实体击退事件
com.wzz.w_loader.event.events.LivingPushEntitiesEvent         // 实体挤压附近实体事件
com.wzz.w_loader.event.events.LivingDropEvent                 // 实体掉落物品事件
com.wzz.w_loader.event.events.LivingDropExperienceEvent       // 实体掉落经验事件
com.wzz.w_loader.event.events.LivingHurtEquipmentEvent        // 实体盔甲掉耐久事件
com.wzz.w_loader.event.events.LivingAddEffectEvent            // 实体添加药水效果事件
com.wzz.w_loader.event.events.LivingRemoveEffectEvent         // 实体药水效果清除事件
com.wzz.w_loader.event.events.LivingStartUsingItemEvent       // 实体开始使用物品事件
```

#### 玩家事件
```java
com.wzz.w_loader.event.events.PlayerJoinEvent                 // 玩家加入服务器事件
com.wzz.w_loader.event.events.PlayerChatEvent                 // 玩家聊天事件
com.wzz.w_loader.event.events.PlayerGameModeChangeEvent       // 玩家模式切换事件
com.wzz.w_loader.event.events.PlayerRightClickBlockEvent      // 玩家右键方块事件
com.wzz.w_loader.event.events.PlayerUseItemEvent              // 玩家使用物品事件
com.wzz.w_loader.event.events.ItemEntityTouchEvent            // 物品捡起事件
```

#### GUI 事件
```java
com.wzz.w_loader.event.events.ScreenOpenEvent                 // GUI打开事件
com.wzz.w_loader.event.events.ScreenCloseEvent                // GUI关闭事件
com.wzz.w_loader.event.events.ScreenInitEvent                 // GUI初始化事件
com.wzz.w_loader.event.events.ScreenRenderEvent               // GUI渲染事件
com.wzz.w_loader.event.events.GuiRenderEvent                  // GUI（叠加层）渲染事件
com.wzz.w_loader.event.events.RenderTooltipEvent              // 渲染物品提示框事件
com.wzz.w_loader.event.events.RenderTooltipBackgroundEvent    // 渲染提示框背景事件
```

#### Tick 事件
```java
com.wzz.w_loader.event.events.ServerTickEvent                 // 服务器时刻更新事件
com.wzz.w_loader.event.events.ClientTickEvent                 // 客户端时刻更新事件
com.wzz.w_loader.event.events.RenderTickEvent                 // 渲染更新时刻事件
com.wzz.w_loader.event.events.ServerTickChunkEvent            // 服务器区块刻更新事件
```

#### 注册与系统事件
```java
com.wzz.w_loader.event.events.RegistryEvent                   // 注册事件
com.wzz.w_loader.event.events.CreativeTabEvent                // 创造栏事件
com.wzz.w_loader.event.events.EntityAttributeCreationEvent    // 实体属性注册事件
com.wzz.w_loader.event.events.EntityRegisterRenderersEvent    // 实体渲染注册事件
com.wzz.w_loader.event.events.ServerStartingEvent             // 服务器运行时事件
com.wzz.w_loader.event.events.CrashReportEvent                // 崩溃日志事件
com.wzz.w_loader.event.events.ServerWakeUpAllPlayersEvent     // 服务器唤醒所有玩家事件
com.wzz.w_loader.event.events.TotemTriggerEvent               // 不死图腾触发事件
com.wzz.w_loader.event.events.HandleEntityEvent               // 实体事件包处理事件
com.wzz.w_loader.event.events.TransformEvent                  // 类转换事件（非特殊要求不要监听这个事件）
```

### 事件使用

#### 静态监听
使用 `@Subscribe` 注解标记方法：

```java
@Subscribe
public void onPlayerJoin(PlayerJoinEvent event) {
    System.out.println("玩家 " + event.getPlayer().getName() + " 加入了游戏");
}
```

#### 动态注册/注销

```java
// 注册监听器
com.wzz.w_loader.event.EventBus.INSTANCE.register(listener);

// 注销监听器
com.wzz.w_loader.event.EventBus.INSTANCE.unregister(listener);
```

## 🔧 字节码转换

### 注册转换器

```java
com.wzz.w_loader.transform.TransformerRegistry.getInstance()
    .register(new IClassTransformer() {
        @Override
        public String targetClass() {
            return "net/minecraft/world/entity/player/Player";
        }
        
        @Override
        public byte[] transform(String className, byte[] bytes) {
            // 使用 ASM 修改字节码
            return modifiedBytes;
        }
    });
```

### AccessTransformer

W Loader 支持 AccessTransformer 修改访问修饰符。

在 `META-INF` 文件夹下创建 `at.cfg` 文件：

```cfg
# W-Loader Access Transformer
# 格式：
#   <访问级别> <完整类名>
#   <访问级别> <完整类名>.<字段名>
#   <访问级别> <完整类名>.<方法名><描述符>
#
# 访问级别：public / protected / private / public-f（public且去final）

# 示例
public net.minecraft.world.item.CreativeModeTabs.BUILDING_BLOCKS
public net.minecraft.world.item.CreativeModeTabs.COLORED_BLOCKS
public com.wzz.w_loader.event.events.EntityRegisterRenderersEvent.registerEntityRenderer(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/client/renderer/entity/EntityRendererProvider;)V
public com.wzz.w_loader.event.events.EntityRegisterRenderersEvent.registerBlockEntityRenderer(Lnet/minecraft/world/level/block/entity/BlockEntityType;Lnet/minecraft/client/renderer/blockentity/BlockEntityRendererProvider;)V
```

## 💡 开发工具

### IntelliJ IDEA 插件

使用 [W Loader At IDEA] 插件可以让 AccessTransformer 使用更顺手。

## 📖 示例代码

完整的示例代码请看教程栏目。

## ⚠️ 注意事项

1. **实体注册**：`Registries.ENTITIES.register` 请放在模组主类无参构造函数执行。
2. **TransformEvent**：非特殊要求不要监听这个事件。
3. **Minecraft 本体**：需要手动添加 Minecraft 本体 Jar 作为依赖。
