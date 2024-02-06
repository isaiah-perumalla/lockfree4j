package com.isaiahp.shm;

import com.isaiahp.utils.FileUtils;
import org.agrona.IoUtil;
import org.agrona.LangUtil;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class MMapFile {

    private final long size;
    private FileChannel fileChannel;
    private final long bufferAddress;
    private final Path shmFileName;

    private MMapFile(FileChannel fileChannel, long bufferAddress, Path shmFileName, long size) {
        this.fileChannel = fileChannel;
        this.bufferAddress = bufferAddress;
        this.shmFileName = shmFileName;
        this.size = size;
    }


    public static MMapFile create(boolean readOnly, Path path, long size) {
        final FileChannel.MapMode mode = readOnly ? FileChannel.MapMode.READ_ONLY : FileChannel.MapMode.READ_WRITE;
        FileChannel channel = openFileChannel(path, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
        FileUtils.truncate(channel, size);
        if (mode == FileChannel.MapMode.READ_WRITE) {
            tryLockFile(channel, path);
        }

        final long bufferAddress = IoUtil.map(channel, mode, 0, size);
        return new MMapFile(channel, bufferAddress, path, size);
    }

    private static void tryLockFile(FileChannel channel, Path path) {
        FileLock fileLock;
        try {
            fileLock = channel.tryLock();
        }
        catch (IOException e) {
            throw new RuntimeException("failed to acquire lock on file " + path.getFileName(), e);
        }
        if (fileLock == null) {
            throw new RuntimeException("failed to acquire lock on file " + path.getFileName());
        }
    }


    public static FileChannel openFileChannel(Path p, OpenOption ... options) {
        try {
            return FileChannel.open(p, options);
        }
        catch (IOException e) {
            LangUtil.rethrowUnchecked(e);
        }
        return null;
    }

    public void close() {
        if (fileChannel != null) {
            if (bufferAddress != -1) {
                try {
                    IoUtil.unmap(fileChannel, bufferAddress, size);
                }
                catch (Exception e) {}
            }
        }
        try {
            fileChannel.close();
        }
        catch (Exception e) {}
        finally {
            fileChannel = null;
        }
    }

    public long getBufferAddress() {
        return bufferAddress;
    }
}
