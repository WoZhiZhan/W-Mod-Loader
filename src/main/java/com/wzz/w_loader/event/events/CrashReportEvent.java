package com.wzz.w_loader.event.events;

import com.wzz.w_loader.event.Event;

public class CrashReportEvent extends Event {
    private final Object crashReport;
    private StringBuilder builder;
    private boolean modified = false;
    private boolean cancelled = false;
    private String additionalInfo = "";
    
    public CrashReportEvent(Object crashReport, StringBuilder builder) {
        this.crashReport = crashReport;
        this.builder = builder;
    }
    
    public Object getCrashReport() {
        return crashReport;
    }
    
    public StringBuilder getBuilder() {
        return builder;
    }
    
    public void setBuilder(StringBuilder builder) {
        this.builder = builder;
        this.modified = true;
    }
    
    public boolean isModified() {
        return modified;
    }
    
    public boolean isCancelled() {
        return cancelled;
    }
    
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
    
    public String getAdditionalInfo() {
        return additionalInfo;
    }
    
    public void setAdditionalInfo(String info) {
        this.additionalInfo = info;
        this.modified = true;
    }
    
    /**
     * 添加自定义的崩溃报告信息
     */
    public void addCustomInfo(String key, String value) {
        builder.append("\n-- ").append(key).append(" --\n");
        builder.append(value).append("\n");
        this.modified = true;
    }
}