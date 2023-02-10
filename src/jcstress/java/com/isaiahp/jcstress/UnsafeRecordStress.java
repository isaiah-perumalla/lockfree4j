package com.isaiahp.jcstress;

import com.isaiahp.concurrent.UnsafeRecord;
import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.JJJJ_Result;

@JCStressTest
@JCStressMeta(StressUtils.class)
@State
public class UnsafeRecordStress {

    static final UnsafeRecord UNSAFE_RECORD = new UnsafeRecord();
    private static long version = 0;


    @Actor
    public void writer() {
        version = StressUtils.doWrite(UNSAFE_RECORD, version);
    }

    @Actor
    public void reader0(JJJJ_Result r) {
        StressUtils.doRead(r, UNSAFE_RECORD, 0);
    }

    @Actor
    public void reader1(JJJJ_Result r) {
        StressUtils.doRead(r, UNSAFE_RECORD, 1);
    }
}
