package com.isaiahp.jcstress;

import com.isaiahp.concurrent.experiments.VolatileRecord;
import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.JJJJ_Result;

@JCStressTest
@JCStressMeta(StressUtils.class)
@State
public class VolatileRecordStress {

    private static final VolatileRecord VOLATILE_RECORD = new VolatileRecord();
    private static long version = 0;


    @Actor
    public void writer() {
        version = StressUtils.doWrite(VOLATILE_RECORD, version);
    }

    @Actor
    public void reader0(JJJJ_Result r) {
        StressUtils.doRead(r, VOLATILE_RECORD, 0);
    }

    @Actor
    public void reader1(JJJJ_Result r) {
        StressUtils.doRead(r, VOLATILE_RECORD, 1);
    }
}
