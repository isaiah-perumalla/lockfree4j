package com.isaiahp.jcstress;

import com.isaiahp.concurrent.BrokenOrdering;
import com.isaiahp.concurrent.SingleWriterRecord;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressMeta;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.JJJJ_Result;

// Mark the class as JCStress test.
@JCStressTest


// This is a state object
@State
@JCStressMeta(StressUtils.class)
public class BrokenRecordStress {

        static final BrokenOrdering.BrokenSingleWriterRecord BROKEN_RECORD = new BrokenOrdering.BrokenSingleWriterRecord();
        private static long version;

        @Actor
        public void writer() {
            SingleWriterRecord record = BROKEN_RECORD;
            version = StressUtils.doWrite(record, version);
        }

    @Actor
    public void reader0(JJJJ_Result r) {
        StressUtils.doRead(r, BROKEN_RECORD, 0);
    }

    @Actor
    public void reader1(JJJJ_Result r) {
        StressUtils.doRead(r, BROKEN_RECORD, 1);
    }

}
