package com.wzz.w_loader.util;

import com.wzz.w_loader.hook.GameLifeCycleHook;
import com.wzz.w_loader.logger.WLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;

import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.FutureTask;

public class MainThreadScheduler {
    private static final Queue<Runnable> CLIENT_TASK_QUEUE = new ConcurrentLinkedQueue<>();
    private static final Queue<Runnable> SERVER_TASK_QUEUE = new ConcurrentLinkedQueue<>();

    // ==================== 客户端主线程调度 ====================
    public static void scheduleOnClientThread(Runnable task) {
        Minecraft mc = GameLifeCycleHook.getMinecraft();
        if (mc == null) {
            WLogger.warn("客户端实例未初始化，任务暂存到队列：{}", task.getClass().getName());
            CLIENT_TASK_QUEUE.offer(task);
            return;
        }

        if (mc.isSameThread()) {
            runTaskSafely(task);
        } else {
            mc.execute(() -> runTaskSafely(task));
        }
    }

    public static <T> T callOnClientThread(Callable<T> task) throws Exception {
        Minecraft mc = GameLifeCycleHook.getMinecraft();
        if (mc == null) {
            throw new IllegalStateException("客户端实例未初始化，无法执行同步任务");
        }

        if (mc.isSameThread()) {
            return task.call();
        } else {
            FutureTask<T> futureTask = new FutureTask<>(task);
            mc.execute(futureTask);
            return futureTask.get();
        }
    }

    // ==================== 服务端主线程调度 ====================
    public static void scheduleOnServerThread(Runnable task) {
        MinecraftServer server = GameLifeCycleHook.getServer();
        if (server == null) {
            WLogger.warn("服务端实例未初始化，任务暂存到队列：{}", task.getClass().getName());
            SERVER_TASK_QUEUE.offer(task);
            return;
        }

        if (server.isSameThread()) {
            runTaskSafely(task);
        } else {
            server.execute(() -> runTaskSafely(task));
        }
    }

    public static <T> T callOnServerThread(Callable<T> task) throws Exception {
        MinecraftServer server = GameLifeCycleHook.getServer();
        if (server == null) {
            throw new IllegalStateException("服务端实例未初始化，无法执行同步任务");
        }

        if (server.isSameThread()) {
            return task.call();
        } else {
            FutureTask<T> futureTask = new FutureTask<>(task);
            server.execute(futureTask);
            return futureTask.get();
        }
    }

    // ==================== 自动识别环境 ====================
    public static void scheduleOnMainThread(Runnable task) {
        if (GameLifeCycleHook.getMinecraft() != null) {
            scheduleOnClientThread(task);
        } else if (GameLifeCycleHook.getServer() != null) {
            scheduleOnServerThread(task);
        } else {
            WLogger.error("无法识别运行环境（客户端/服务端），任务执行失败：{}", task.getClass().getName());
            CLIENT_TASK_QUEUE.offer(task);
        }
    }

    // ==================== 内部工具方法 ====================
    private static void runTaskSafely(Runnable task) {
        try {
            task.run();
        } catch (Exception e) {
            WLogger.error("主线程任务执行失败", e);
        }
    }

    public static void flushPendingTasks() {
        // 执行客户端暂存任务
        Runnable clientTask;
        while ((clientTask = CLIENT_TASK_QUEUE.poll()) != null) {
            scheduleOnClientThread(clientTask);
        }

        // 执行服务端暂存任务
        Runnable serverTask;
        while ((serverTask = SERVER_TASK_QUEUE.poll()) != null) {
            scheduleOnServerThread(serverTask);
        }
    }

    public static boolean isOnClientMainThread() {
        return GameLifeCycleHook.isOnClientMainThread();
    }

    public static boolean isOnServerMainThread() {
        return GameLifeCycleHook.isOnServerMainThread();
    }
}