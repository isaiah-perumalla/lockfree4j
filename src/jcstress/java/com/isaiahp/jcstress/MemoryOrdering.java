package com.isaiahp.jcstress;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.ZJJ_Result;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

import static org.openjdk.jcstress.annotations.Expect.*;

public class MemoryOrdering {
    private static class Holder {
        long value0;
        long value1;
        boolean done;
        static final VarHandle VH_DONE;
        static final VarHandle VH_VALUE0;
        static final VarHandle VH_VALUE1;

        static {
            try {
                VH_DONE = MethodHandles.lookup().findVarHandle(Holder.class, "done", boolean.class);
                VH_VALUE0 = MethodHandles.lookup().findVarHandle(Holder.class, "value0", long.class);
                VH_VALUE1 = MethodHandles.lookup().findVarHandle(Holder.class, "value1", long.class);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }


    }

    @JCStressTest
    @Outcome(id = "false, 0, 0", expect = ACCEPTABLE, desc = "reads early before any writes")
    @Outcome(id = "false, 1, 0", expect = ACCEPTABLE, desc = "write value0 before release")
    @Outcome(id = "true, 0, 0", expect = ACCEPTABLE_INTERESTING, desc = "value0 read before done, reorders of reads after acquireFence is allowed ")
    @Outcome(id = "true, 1, 0", expect = ACCEPTABLE, desc = "release ")
    @Outcome(id = "true, 1, 2", expect = ACCEPTABLE, desc = "ok")
    @Outcome(id = "false, 0, 2", expect = FORBIDDEN, desc = "write to value1, Re-ordered before release padded done")
    @Outcome(id = "false, 1, 2", expect = FORBIDDEN, desc = "write to value1, Re-ordered before release padded done")
    @Outcome( expect = FORBIDDEN, desc = "not allowed")
    @State
    public static class PaddedAcquireReleaseReOrdering {
        private final PaddedHolder paddedHolder = new PaddedHolder();

        @Actor
        public void actor1() {
            paddedHolder.value0 = 1;
            PaddedHolder.PADDED_VH_DONE.setRelease(paddedHolder, true);
            paddedHolder.value1 = 2;
        }

        @Actor
        public void actor2(ZJJ_Result r) {
            final PaddedHolder h1 = this.paddedHolder;
            r.r3 = (long) PaddedHolder.PADDED_VH_VALUE1.get(h1);
            VarHandle.acquireFence();//  read ensure value1 is read first before done
            r.r1 =  (boolean) PaddedHolder.PADDED_VH_DONE.get(h1);
            r.r2 = (long) PaddedHolder.PADDED_VH_VALUE0.get(h1);
        }

    }

    @JCStressTest
    @Outcome(id = "false, 0, 0", expect = ACCEPTABLE, desc = "reads early before any writes")
    @Outcome(id = "false, 1, 0", expect = ACCEPTABLE, desc = "write value0 before release")
    @Outcome(id = "true, 0, 0", expect = ACCEPTABLE_INTERESTING, desc = "value0 read before done, reorders of reads after acquireFence is allowed ")
    @Outcome(id = "true, 1, 0", expect = ACCEPTABLE, desc = "release ")
    @Outcome(id = "true, 1, 2", expect = ACCEPTABLE, desc = "ok")
    @Outcome(id = "false, 0, 2", expect = FORBIDDEN, desc = "write to value1, Re-ordered before release")
    @Outcome(id = "false, 1, 2", expect = FORBIDDEN, desc = "write to value1, Re-ordered before release")
    @Outcome( expect = FORBIDDEN, desc = "not allowed")
    @State
    public static class AcquireReleaseReOrdering {
        private final Holder h1 = new Holder();

        @Actor
        public void writerThread() {
            h1.value0 = 1;
            Holder.VH_DONE.setRelease(h1, true);
            h1.value1 = 2;
        }

        @Actor
        public void readerThread(ZJJ_Result r) {
            final Holder h1 = this.h1;
            r.r3 = (long) Holder.VH_VALUE1.get(h1);

            VarHandle.acquireFence();//  read ensure value1 is read first before done
            r.r1 =  (boolean) Holder.VH_DONE.get(h1);
            r.r2 = (long) Holder.VH_VALUE0.get(h1);
        }

    }

    @JCStressTest
    @Outcome(id = "false, 0, 0", expect = ACCEPTABLE, desc = "reads early before any writes")
    @Outcome(id = "false, 1, 0", expect = ACCEPTABLE, desc = "write value0 before release")
    @Outcome(id = "true, 0, 0", expect = ACCEPTABLE_INTERESTING, desc = "value0 read before done, reorders of reads after acquireFence is allowed ")
    @Outcome(id = "true, 1, 0", expect = ACCEPTABLE, desc = "release ")
    @Outcome(id = "true, 1, 0", expect = ACCEPTABLE, desc = "release ")
    @Outcome(id = "true, 1, 2", expect = ACCEPTABLE, desc = "ok")
    @Outcome(id = "false, 0, 2", expect = FORBIDDEN, desc = "write to value1, Re-ordered before releaseFence")
    @Outcome(id = "false, 1, 2", expect = FORBIDDEN, desc = "write to value1, Re-ordered before releaseFence")
    @Outcome( expect = FORBIDDEN, desc = "not allowed")
    @State
    public static class ReleaseFenceReOrdering {


        private final Holder h1 = new Holder();


        @Actor
        public void writerThread() {
            h1.value0 = 1;
            Holder.VH_DONE.setRelease(h1, true);
            VarHandle.releaseFence();

            h1.value1 = 2;
        }

        @Actor
        public void readerThread(ZJJ_Result r) {
            final Holder h1 = this.h1;

            r.r3 = (long) Holder.VH_VALUE1.get(h1);
            VarHandle.acquireFence();//  read ensure  this is read first before done
            r.r1 =  (boolean) Holder.VH_DONE.get(h1);
            r.r2 = (long) Holder.VH_VALUE0.get(h1);

        }

    }
}