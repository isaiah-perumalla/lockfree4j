package com.isaiahp.jcstress;
import com.isaiahp.concurrent.BrokenOrdering;
import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.*;

import static org.openjdk.jcstress.annotations.Expect.*;
// Mark the class as JCStress test.
@JCStressTest

// These are the test outcomes.
@Outcome(id = "false, 0, 0", expect = ACCEPTABLE_INTERESTING, desc = "Read unsuccessful while writer updated")
@Outcome(id = "true, -1, -1", expect = ACCEPTABLE, desc = "Correctly read intact values")
@Outcome(                     expect = FORBIDDEN, desc = "partial or INCORERCT values read")

// This is a state object
@State

public class SimpleStress {

        BrokenOrdering.Record record = new BrokenOrdering.Record();
        static final byte[] result = new byte[16];
    private boolean read_success;

    @Actor
        public void writer() {
            record.write(-1L, -1L);
        }

        @Actor
        public void reader() {

            read_success = record.read(result) != -1;

        }

    @Arbiter
    public void arbiter(ZJJ_Result r) {
        r.r1 = read_success;

        if (read_success) {
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
