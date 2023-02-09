package com.isaiahp.jcstress;

import com.isaiahp.concurrent.BrokenOrdering;
import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.JJJ_Result;
import org.openjdk.jcstress.infra.results.ZJJ_Result;

import static org.openjdk.jcstress.annotations.Expect.*;

@JCStressTest
// These are the test outcomes.
@Outcome(id = "false, 0, 0", expect = ACCEPTABLE_INTERESTING, desc = "Read unsuccessful while writer updated")
@Outcome(id = "true, -1, -1", expect = ACCEPTABLE, desc = "Correctly read intact values")
@Outcome(                     expect = FORBIDDEN, desc = "partial or INCORERCT values read")
@State
public class VolatileRecordStress {

    BrokenOrdering.VolatileRecord record = new BrokenOrdering.VolatileRecord();
    static final byte[] result = new byte[16];

    private long read_version;

    @Actor
    public void writer() {
        record.write(-1L, -1L);
    }

    @Actor
    public void reader() {

        read_version = record.read(result);

    }

    @Arbiter
    public void arbiter(ZJJ_Result r) {
        long version = read_version;
        r.r1 = version > 0;

        if (version > 0) {
            for (int i = 0; i < 8; i++) {

                int shift = i * 8;
                r.r2 |= (result[i]  << shift);
            }

            for (int i = 8; i < 16; i++) {
                int shift = (i - 8) * 8;
                r.r3 |= (result[i]  << shift);
            }
        }
        else {
            r.r2 = 0;
            r.r3 = 0;
        }

        //clear
        for (int i = 0; i < 16; i++) {
            result[i] = 0;
        }
    }
}
