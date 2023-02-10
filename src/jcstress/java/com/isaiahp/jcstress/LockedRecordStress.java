package com.isaiahp.jcstress;

import com.isaiahp.concurrent.LockedRecord;
import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.JJJJ_Result;

import static org.openjdk.jcstress.annotations.Expect.*;

@JCStressTest
// These are the test outcomes.
@State
@JCStressMeta(StressUtils.class)
public class LockedRecordStress {

    static final LockedRecord record = new LockedRecord();
    private static long version = 0;


    @Actor
    public void writer() {
        version = StressUtils.doWrite(record, version);
    }

    @Actor
    public void reader0(JJJJ_Result r) {
        StressUtils.doRead(r, record, 0);
    }

    @Actor
    public void reader1(JJJJ_Result r) {

        StressUtils.doRead(r, record, 1);
    }

}
