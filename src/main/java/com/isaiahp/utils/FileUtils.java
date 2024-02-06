package com.isaiahp.utils;

import org.agrona.LangUtil;

import java.io.FileDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;

public class FileUtils {
    static final Method TRUNCATE_METHOD_HANDLE;
    private static final Field FD;

    static
    {
        try
        {
            final Class<?> fileDispatcheImpleClass = Class.forName("sun.nio.ch.FileDispatcherImpl");
            final Method truncateMethod = getFileChannelMethod(fileDispatcheImpleClass, "truncate0", FileDescriptor.class, long.class);
            TRUNCATE_METHOD_HANDLE = truncateMethod;
            FD = getFileDescriptorField();

            }
        catch (final Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }

    private static Method getFileChannelMethod(
            final Class<?> fileChannelClass, final String name, final Class<?>... parameterTypes)
            throws NoSuchMethodException
    {
        final Method method = fileChannelClass.getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);

        return method;
    }

    public static void truncate(FileChannel fileChannel, long newSize) {
        try {
            final FileDescriptor fd = (FileDescriptor) FD.get(fileChannel);
            int result;
            do {
                result = (int)TRUNCATE_METHOD_HANDLE.invoke(null, fd, newSize);
            }while(result == -3 && fileChannel.isOpen());
        }
        catch (Throwable e) {
            LangUtil.rethrowUnchecked(e);
        }
    }

    private static Field getFileDescriptorField() {
        try {
            Field field = Class.forName("sun.nio.ch.FileChannelImpl").getDeclaredField("fd");
            field.setAccessible(true);
            return field;
        }
        catch (Exception e) {
            LangUtil.rethrowUnchecked(e);
        }
        return null;
    }

}
