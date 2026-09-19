package com.example.calculatorvault;

import java.io.File;

public class VaultItem {

    private final File file;
    private final String name;
    private final String mimeType;
    private final long size;
    private final long date;

    public VaultItem(
            File file,
            String name,
            String mimeType,
            long size,
            long date
    ) {
        this.file = file;
        this.name = name;
        this.mimeType = mimeType;
        this.size = size;
        this.date = date;
    }

    public File getFile() {
        return file;
    }

    public String getName() {
        return name;
    }

    public String getMimeType() {
        return mimeType;
    }

    public long getSize() {
        return size;
    }

    public long getDate() {
        return date;
    }

    public boolean isVideo() {
        return mimeType != null
                && mimeType.startsWith("video/");
    }

    public boolean isImage() {
        return mimeType != null
                && mimeType.startsWith("image/");
    }
}