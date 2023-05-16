package com.files.vey.object;

import java.time.ZonedDateTime;

public class ObjectInfo {
    private String name;
    private ZonedDateTime lastModified;
    private long size;

    public ObjectInfo(String name, ZonedDateTime lastModified, long size) {
        this.name = name;
        this.lastModified = lastModified;
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ZonedDateTime getLastModified() {
        return lastModified;
    }

    public void setLastModified(ZonedDateTime lastModified) {
        this.lastModified = lastModified;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}