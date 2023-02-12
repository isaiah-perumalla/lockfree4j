package com.isaiahp.jcstress;

import com.isaiahp.concurrent.experiments.SingleWriterRecord;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.infra.results.JJJJ_Result;

import static org.openjdk.jcstress.annotations.Expect.*;

// These are the test outcomes.
@Outcome(id =  "0, 0, 0, 0", expect = ACCEPTABLE_INTERESTING, desc = "Read unsuccessful while writer updated")
@Outcome(id =  "0, 0, -1, -1", expect = ACCEPTABLE, desc = "Correctly read intact values")
@Outcome(id =  "0, 0, -2, -2", expect = ACCEPTABLE, desc = "Correctly read intact values")

@Outcome(id =  "-1, -1, 0, 0", expect = ACCEPTABLE, desc = "Correctly read intact values")
@Outcome(id =  "-1, -1, -1, -1", expect = ACCEPTABLE, desc = "Correctly read intact values")
@Outcome(id =  "-1, -1, -2, -2", expect = ACCEPTABLE, desc = "Correctly read intact values")

@Outcome(id =  "-2, -2, 0, 0", expect = ACCEPTABLE, desc = "Correctly read intact values")
@Outcome(id =  "-2, -2, -1, -1", expect = ACCEPTABLE, desc = "Correctly read intact values")
@Outcome(id =  "-2, -2, -2, -2", expect = ACCEPTABLE, desc = "Correctly read intact values")

@Outcome(                     expect = FORBIDDEN, desc = "Partial or INCORRECT values read")

public class StressUtils {
    private static final long[] result0 = new long[16];
    private static final long[] result1 = new long[16];

    public static long doWrite(SingleWriterRecord record, long v) {
        if (((v >> 1) & 1) == 0) {
        return record.write(-1L, -1L);
    }
    else {
        return record.write(-2L, -2L);
    }

    }

    static void doRead(JJJJ_Result r, SingleWriterRecord singleWriterRecord, int actor) {


        if (actor == 0) {
            long[] result = result0;
            clearBuffer(result);
            long readVersion = singleWriterRecord.read(result);
            if (readVersion > 0) {
                r.r1 = result[0];
                r.r2 = result[1];
            } else {
                r.r1 = 0;
                r.r2 = 0;
            }
        }
        else {
            long[] result = result1;
            long readVersion = singleWriterRecord.read(result);
            clearBuffer(result);
            if (readVersion > 0) {
                r.r3 = result[0];
                r.r4 = result[1];
            } else {
                r.r3 = 0;
                r.r4 = 0;
            }
        }
    }

    static void clearBuffer(long[] buffer) {
        //clear
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = 0;
        }
    }
}
