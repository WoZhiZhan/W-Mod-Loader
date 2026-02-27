package com.wzz.w_loader.logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WLogger {

    public enum Level {
        DEBUG(0, "DEBUG"),
        INFO(1, "INFO"),
        WARN(2, "WARN"),
        ERROR(3, "ERROR"),
        FATAL(4, "FATAL");

        final int value;
        final String name;

        Level(int value, String name) {
            this.value = value;
            this.name = name;
        }
    }

    private static class Config {
        static Level MIN_LEVEL = Level.DEBUG;  // 最低日志级别
        static boolean LOG_TO_CONSOLE = true;   // 是否输出到控制台
        static boolean LOG_TO_FILE = true;      // 是否输出到文件
        static String LOG_FILE_PATH = "logs";    // 日志文件路径
        static String LOG_FILE_NAME = "w_loader"; // 日志文件名
        static int MAX_FILE_SIZE = 10 * 1024 * 1024; // 单个日志文件最大大小（10MB）
        static int MAX_BACKUP_FILES = 5;         // 最大备份文件数
        static boolean SHOW_THREAD = true;       // 是否显示线程名
        static boolean SHOW_CLASS = true;         // 是否显示类名
    }

    private static final ConcurrentLinkedQueue<LogEntry> LOG_QUEUE = new ConcurrentLinkedQueue<>();
    private static PrintStream RAW_OUT;
    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();
    private static boolean initialized = false;
    private static PrintWriter fileWriter;
    private static String currentLogFile;
    private static int currentFileSize = 0;

    private static class LogEntry {
        final Level level;
        final String message;
        final Throwable throwable;
        final long timestamp;
        final Thread thread;
        final String className;

        LogEntry(Level level, String message, Throwable throwable, String className) {
            this.level = level;
            this.message = message;
            this.throwable = throwable;
            this.timestamp = System.currentTimeMillis();
            this.thread = Thread.currentThread();
            this.className = className;
        }
    }

    // 初始化日志系统
    public static void init() {
        if (initialized) return;

        // 创建日志目录
        File logDir = new File(Config.LOG_FILE_PATH);
        if (!logDir.exists()) {
            logDir.mkdirs();
        }

        // 启动异步日志处理器
        SCHEDULER.scheduleAtFixedRate(WLogger::processLogQueue, 0, 100, TimeUnit.MILLISECONDS);

        // 注册关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(WLogger::shutdown));

        initialized = true;
        RAW_OUT = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        // 记录启动信息
        info("日志系统初始化成功", WLogger.class.getName());
        info("Java版本: " + System.getProperty("java.version"), WLogger.class.getName());
        info("操作系统: " + System.getProperty("os.name"), WLogger.class.getName());
    }

    // 处理日志队列
    private static void processLogQueue() {
        LogEntry entry;
        while ((entry = LOG_QUEUE.poll()) != null) {
            writeLog(entry);
        }
    }

    // 写入日志
    private static synchronized void writeLog(LogEntry entry) {
        if (entry.level.value < Config.MIN_LEVEL.value) {
            return;
        }

        String formattedLog = formatLog(entry);

        // 输出到控制台
        if (Config.LOG_TO_CONSOLE) {
            printToConsole(entry.level, formattedLog);
        }

        // 输出到文件
        if (Config.LOG_TO_FILE) {
            writeToFile(formattedLog);
        }
    }

    // 格式化日志
    private static String formatLog(LogEntry entry) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        StringBuilder sb = new StringBuilder();

        // 时间戳
        sb.append("[").append(sdf.format(new Date(entry.timestamp))).append("] ");

        // 日志级别
        sb.append("[").append(entry.level.name).append("] ");

        // 线程名
        if (Config.SHOW_THREAD) {
            sb.append("[").append(entry.thread.getName()).append("] ");
        }

        // 类名
        if (Config.SHOW_CLASS && entry.className != null) {
            String simpleClassName = entry.className.substring(entry.className.lastIndexOf('.') + 1);
            sb.append("[").append(simpleClassName).append("] ");
        }

        // 消息
        sb.append(entry.message);

        // 异常信息
        if (entry.throwable != null) {
            sb.append("\n").append(getStackTraceAsString(entry.throwable));
        }

        return sb.toString();
    }

    // 控制台输出（带颜色）
    private static void printToConsole(Level level, String message) {
        String color;
        switch (level) {
            case DEBUG:
                color = "\u001B[36m"; // 青色
                break;
            case INFO:
                color = "\u001B[32m"; // 绿色
                break;
            case WARN:
                color = "\u001B[33m"; // 黄色
                break;
            case ERROR:
            case FATAL:
                color = "\u001B[31m"; // 红色
                break;
            default:
                color = "\u001B[0m"; // 默认
        }

        // Windows CMD可能不支持ANSI颜色，可以在这里判断
        if (System.console() != null && System.getProperty("os.name").toLowerCase().contains("win")) {
            // Windows CMD，不使用颜色
            RAW_OUT.println(message);
        } else {
            RAW_OUT.println(color + message + "\u001B[0m");
        }
    }

    // 写入文件
    private static synchronized void writeToFile(String log) {
        try {
            // 检查是否需要滚动日志文件
            if (currentLogFile == null || (currentFileSize > Config.MAX_FILE_SIZE)) {
                rollLogFile();
            }

            if (fileWriter == null) {
                fileWriter = new PrintWriter(new FileWriter(currentLogFile, true), true);
            }

            fileWriter.println(log);
            fileWriter.flush();
            currentFileSize += log.length() + System.lineSeparator().length();

        } catch (Exception e) {
            System.err.println("写入日志文件失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 滚动日志文件
    private static synchronized void rollLogFile() {
        try {
            // 关闭当前文件
            if (fileWriter != null) {
                fileWriter.close();
                fileWriter = null;
            }

            // 生成新文件名（按日期）
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            String timestamp = sdf.format(new Date());
            currentLogFile = Config.LOG_FILE_PATH + File.separator +
                    Config.LOG_FILE_NAME + "_" + timestamp + ".log";

            // 删除旧的备份文件
            cleanupOldLogs();

            currentFileSize = 0;

        } catch (Exception e) {
            System.err.println("滚动日志文件失败: " + e.getMessage());
        }
    }

    // 清理旧日志
    private static void cleanupOldLogs() {
        File logDir = new File(Config.LOG_FILE_PATH);
        File[] files = logDir.listFiles((dir, name) ->
                name.startsWith(Config.LOG_FILE_NAME) && name.endsWith(".log"));

        if (files != null && files.length > Config.MAX_BACKUP_FILES) {
            // 按修改时间排序，删除最旧的
            java.util.Arrays.sort(files, (f1, f2) ->
                    Long.compare(f2.lastModified(), f1.lastModified()));

            for (int i = Config.MAX_BACKUP_FILES; i < files.length; i++) {
                files[i].delete();
            }
        }
    }

    // 获取异常堆栈
    private static String getStackTraceAsString(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }

    // 关闭日志系统
    public static void shutdown() {
        try {
            // 处理剩余的日志
            processLogQueue();

            // 关闭文件写入器
            if (fileWriter != null) {
                fileWriter.close();
            }

            // 关闭调度器
            SCHEDULER.shutdown();
            SCHEDULER.awaitTermination(1, TimeUnit.SECONDS);

        } catch (Exception e) {
            System.err.println("关闭日志系统失败: " + e.getMessage());
        }
    }

    // ================ 配置方法 ================

    public static void setMinLevel(Level level) {
        Config.MIN_LEVEL = level;
    }

    public static void setLogToConsole(boolean enable) {
        Config.LOG_TO_CONSOLE = enable;
    }

    public static void setLogToFile(boolean enable) {
        Config.LOG_TO_FILE = enable;
    }

    public static void setLogPath(String path) {
        Config.LOG_FILE_PATH = path;
    }

    public static void setLogFileName(String name) {
        Config.LOG_FILE_NAME = name;
    }

    public static void setMaxFileSize(int maxSizeBytes) {
        Config.MAX_FILE_SIZE = maxSizeBytes;
    }

    public static void setMaxBackupFiles(int maxBackup) {
        Config.MAX_BACKUP_FILES = maxBackup;
    }

    // ================ 日志方法 ================

    public static void debug(String message) {
        debug(message, (Throwable) null);
    }

    public static void debug(String message, Object... args) {
        debug(formatMessage(message, args), (Throwable) null);
    }

    public static void debug(String message, Throwable throwable) {
        String className = getCallerClassName();
        LOG_QUEUE.offer(new LogEntry(Level.DEBUG, message, throwable, className));
    }

    public static void info(String message) {
        info(message, (Throwable) null);
    }

    public static void info(String message, Object... args) {
        info(formatMessage(message, args), (Throwable) null);
    }

    public static void info(String message, Throwable throwable) {
        String className = getCallerClassName();
        LOG_QUEUE.offer(new LogEntry(Level.INFO, message, throwable, className));
    }

    public static void warn(String message) {
        warn(message, (Throwable) null);
    }

    public static void warn(String message, Object... args) {
        warn(formatMessage(message, args), (Throwable) null);
    }

    public static void warn(String message, Throwable throwable) {
        String className = getCallerClassName();
        LOG_QUEUE.offer(new LogEntry(Level.WARN, message, throwable, className));
    }

    public static void error(String message) {
        error(message, (Throwable) null);
    }

    public static void error(String message, Object... args) {
        error(formatMessage(message, args), (Throwable) null);
    }

    public static void error(String message, Throwable throwable) {
        String className = getCallerClassName();
        LOG_QUEUE.offer(new LogEntry(Level.ERROR, message, throwable, className));
    }

    public static void fatal(String message) {
        fatal(message, (Throwable) null);
    }

    public static void fatal(String message, Object... args) {
        fatal(formatMessage(message, args), (Throwable) null);
    }

    public static void fatal(String message, Throwable throwable) {
        String className = getCallerClassName();
        LOG_QUEUE.offer(new LogEntry(Level.FATAL, message, throwable, className));
        // 致命错误可能导致程序退出，立即处理队列
        processLogQueue();
    }

    // 获取调用者类名
    private static String getCallerClassName() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        if (stackTrace.length > 4) {
            return stackTrace[4].getClassName();
        }
        return null;
    }

    // 格式化消息（简单替换）
    private static String formatMessage(String message, Object... args) {
        if (args == null || args.length == 0) {
            return message;
        }

        StringBuilder sb = new StringBuilder();
        int argIndex = 0;
        for (int i = 0; i < message.length(); i++) {
            if (message.charAt(i) == '{' && i + 1 < message.length() && message.charAt(i + 1) == '}') {
                if (argIndex < args.length) {
                    sb.append(args[argIndex++]);
                } else {
                    sb.append("{}");
                }
                i++;
            } else {
                sb.append(message.charAt(i));
            }
        }
        return sb.toString();
    }

    public static void println(String message) {
        info(message);
    }

    public static void printf(String format, Object... args) {
        info(String.format(format, args));
    }

    public static WLogger getLogger() {
        return new WLogger();
    }

    public static WLogger getLogger(Class<?> clazz) {
        return new WLogger();
    }

    public static WLogger getLogger(String name) {
        return new WLogger();
    }

    static {
        init();
        WLogger.setMinLevel(Level.DEBUG);
        WLogger.setLogToConsole(true);
        WLogger.setLogToFile(true);
        WLogger.setLogPath("logs");
        WLogger.setMaxFileSize(5 * 1024 * 1024); // 5MB
        WLogger.setMaxBackupFiles(3);
    }
}