package io.th0rgal.oraxen.utils;

import io.th0rgal.oraxen.utils.logs.Logs;
import java.io.IOException;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;

public class VirtualFile implements Comparable<VirtualFile> {

    private final String parentFolder;
    private final String name;
    private final InputStream inputStream;

    public VirtualFile(String parentFolder, String name, InputStream inputStream) {
        this.parentFolder = OS.getOs().getName().startsWith("Windows")
                ? parentFolder.replace("\\", "/")
                : parentFolder;
        this.name = name;
        this.inputStream = inputStream;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public String getPath() {
        return parentFolder.isEmpty()
                ? name
                : parentFolder + "/" + name;
    }

    public void close() {
        try {
            inputStream.close();
        } catch (IOException e) {
			Logs.logWarning("Could not close virtual file! Path = " + getPath());
        }
    }

    @Override
    public int compareTo(@NotNull VirtualFile other) {
        return other.getPath().compareTo(getPath());
    }

}
