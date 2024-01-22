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