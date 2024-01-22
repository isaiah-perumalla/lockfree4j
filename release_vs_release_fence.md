## VarHandle setRelease vs VarHandle.releaseFence()

varHandle instance method `setRelease` the documentation says the following
*Sets the value of a variable to the newValue, and ensures that prior loads and stores are not reordered after this access.*

VarHandle static method `public static void releaseFence()` the documentation says the following

*Ensures that loads and stores before the fence will not be reordered with stores after the fence.*

1. varHandle instance method `setRelease` works with reference to a variable
2. `public static void releaseFence()` is a memory barrier

To explore the difference why it is important consider the example below

in the example below we can be sure all *stores*/*writes* prior to `Holder.VH_DONE.setRelease(h1, true);`
will happen-before the write to h1.done.

what this means is if a reader thread observed `h1.done = true` then it will also see `h1.value0 = 1`


```java
public void writerThread() {
            h1.value0 = 1;
            //h1.done = true
            Holder.VH_DONE.setRelease(h1, true);
            
            h1.value1 = 2;
        }

```
**can `h1.value1 = 2` happend before, `h1.done = true` ?**
According to the memory model this is possible
it is possible for another thread to observe the following values

```
h1.value0 = 1,
h1.value1 = 2
h1.done = false
```

### A passing JCStress test does not mean everthing is correct

on x86 machine i was not able to have the reader thread observe the result below
```
h1.value0 = 1,
h1.value1 = 2
h1.done = false
```
JCstress snippet
```java
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
```

### Results

1. results on x86 laptop
```agsl
Architecture:            x86_64

Model name:            Intel(R) Core(TM) i7-3537U CPU @ 2.00GHz
Thread(s) per core:  2
Core(s) per socket:  2
Socket(s):           1

```

```agsl
RUN RESULTS:
  Interesting tests: 1 matching test results.

.......... [OK] com.isaiahp.jcstress.MemoryOrdering.AcquireReleaseReOrdering

  Results across all configurations:

       RESULT      SAMPLES     FREQ       EXPECT  DESCRIPTION
  false, 0, 0   51,673,898    5.39%   Acceptable  reads early before any writes
  false, 0, 2            0    0.00%    Forbidden  write to value1, Re-ordered before release
  false, 1, 0      524,829    0.05%   Acceptable  write value0 before release
  false, 1, 2            0    0.00%    Forbidden  write to value1, Re-ordered before release
   true, 0, 0    4,127,172    0.43%  Interesting  value0 read before done, reorders of reads after acquireF...
   true, 1, 0    6,767,246    0.71%   Acceptable  release
   true, 1, 2  895,843,943   93.42%   Acceptable  ok

```

2. result on Arm
```agsl
Architecture:            aarch64
Vendor ID:               ARM
  Model name:            Cortex-A72
    Model:               3
    Thread(s) per core:  1
    Core(s) per cluster: 4
    Socket(s):           -
    Cluster(s):          1

```

```agsl
...... [FAILED] com.isaiahp.jcstress.MemoryOrdering.AcquireReleaseReOrdering

  Results across all configurations:

       RESULT     SAMPLES     FREQ       EXPECT  DESCRIPTION
  false, 0, 0     344,696    1.01%   Acceptable  reads early before any writes
  false, 0, 2           0    0.00%    Forbidden  write to value1, Re-ordered before release
  false, 1, 0      20,160    0.06%   Acceptable  write value0 before release
  false, 1, 2       1,367   <0.01%    Forbidden  write to value1, Re-ordered before release
   true, 0, 0         561   <0.01%  Interesting  value0 read before done, reorders of reads after acquireF...
   true, 1, 0      52,257    0.15%   Acceptable  release
   true, 1, 2  33,819,423   98.78%   Acceptable  ok


```

3. x86 server
```agsl
Architecture:                       x86_64
CPU op-mode(s):                     32-bit, 64-bit
Address sizes:                      46 bits physical, 48 bits virtual
Byte Order:                         Little Endian
CPU(s):                             48
On-line CPU(s) list:                0-47
Vendor ID:                          GenuineIntel
Model name:                         Intel(R) Xeon(R) Silver 4214R CPU @ 2.40GHz
CPU family:                         6
Model:                              85
Thread(s) per core:                 2
Core(s) per socket:                 12
Socket(s):                          2
Stepping:                           7

```

results

```agsl
 JVM args: [-XX:+UnlockDiagnosticVMOptions, -XX:+WhiteBoxAPI, -XX:-RestrictContended, -Dfile.encoding=UTF-8, -Duser.country, -Duser.language=en, -Duser.variant, -XX:-TieredCompilation, -XX:+StressLCM, -XX:+StressGCM, -XX:+StressIGVN, -XX:+StressCCP, -XX:StressSeed=1644101404]
  Fork: #1

       RESULT     SAMPLES     FREQ       EXPECT  DESCRIPTION
  false, 0, 0   4,807,735   10.08%   Acceptable  reads early before any writes
  false, 0, 2           0    0.00%    Forbidden  write to value1, Re-ordered before releaseFence
  false, 1, 0      17,226    0.04%   Acceptable  write value0 before release
  false, 1, 2           0    0.00%    Forbidden  write to value1, Re-ordered before releaseFence
   true, 0, 0      33,121    0.07%  Interesting  value0 read before done, reorders of reads after acquireF...
   true, 1, 0           0    0.00%   Acceptable  release
   true, 1, 2  42,836,338   89.81%   Acceptable  ok

(ETA: in 00:00:02; at Mon, 2024-01-22 08:04:57)
(Sampling Rate: 43.03 M/sec)
(JVMs: 0 starting, 7 running, 0 finishing)
(CPUs: 48 configured, 28 allocated)
(Results: 72 planned; 65 passed, 0 failed, 0 soft errs, 0 hard errs)


.......... [OK] com.isaiahp.jcstress.MemoryOrdering.PaddedAcquireReleaseReOrdering

```