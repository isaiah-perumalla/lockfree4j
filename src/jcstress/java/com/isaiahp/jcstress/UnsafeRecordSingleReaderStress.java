package com.isaiahp.jcstress;

import com.isaiahp.concurrent.experiments.UnsafeRecord;
import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.JJJJ_Result;
import org.openjdk.jcstress.infra.results.JJ_Result;

import static com.isaiahp.jcstress.StressUtils.clearBuffer;
import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

@JCStressTest
@State
@Outcome(id =  "0, 0", expect = ACCEPTABLE, desc = "Correctly read intact values")
@Outcome(id =  "-2, -2", expect = ACCEPTABLE, desc = "Correctly read intact values")
@Outcome(id =  "-1, -1", expect = ACCEPTABLE, desc = "Correctly read intact values")
@Outcome(                expect = FORBIDDEN, desc = "Partial or INCORRECT values read")

public class UnsafeRecordSingleReaderStress {

    static final UnsafeRecord UNSAFE_RECORD = new UnsafeRecord();
    private static long version = 0;
    private static long[] readBuffer = new long[8];

    @Actor
    public void writer() {
        version = StressUtils.doWrite(UNSAFE_RECORD, version);
    }

    @Actor
    public void reader(JJ_Result r) {

        long[] result = readBuffer;
        clearBuffer(result);
        long readVersion = UNSAFE_RECORD.read(result);
        if (readVersion > 0) {
            r.r1 = result[0];
            r.r2 = result[1];
        } else {
            r.r1 = 0;
            r.r2 = 0;
        }
    }

}
