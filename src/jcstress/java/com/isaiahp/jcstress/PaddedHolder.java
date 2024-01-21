package com.isaiahp.jcstress;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

abstract class PaddedValues {
     protected long value0, value1;
     long l0, l1, l2, l3, l4, l5, l6, l7; //64 bytes
    long l8, l9, l10,l11, l12, l13, l14, l15; //64 bytes
}



public class PaddedHolder extends PaddedValues {
    public static final VarHandle PADDED_VH_DONE;
    public static final VarHandle PADDED_VH_VALUE0;
    public static final VarHandle PADDED_VH_VALUE1;

    static {
        try {
            PADDED_VH_DONE = MethodHandles.lookup().findVarHandle(PaddedHolder.class, "done", boolean.class);
            PADDED_VH_VALUE0 = MethodHandles.lookup().findVarHandle(PaddedHolder.class, "value0", long.class);
            PADDED_VH_VALUE1 = MethodHandles.lookup().findVarHandle(PaddedHolder.class, "value1", long.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
     boolean done;
}


