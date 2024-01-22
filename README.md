![Build Status](https://github.com/isaiah-perumalla/lockfree4j/actions/workflows/gradle.yml/badge.svg)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](https://raw.githubusercontent.com/isaiah-perumall/lockfree4j/master/LICENSE)

# LockFree4j

Experiment exploring Java memory model effects on different hardware.
Various implementation of lock-free datastructures
## Seqlock based data structures
SeqLock in Java, it mostly there to study the Java memory model.
SeqLock is a type of reader/writer synchronisation structure , it is a useful data structures if read are far more frequent than they are
written. The implementation allows for a single writer thread and many reader threads.
Writer thread is  **wait-free** , while the Reader thread(s) are **lock-free**.
Reader might fail to aquire a consistent view, and should retry. 


## Implementations
There are several implementations, including some [broken versions](https://github.com/isaiah-perumalla/lockfree4j/tree/main/src/main/java/com/isaiahp/concurrent/experiments) , this is there to study and understand the Java memory model .

### Jsctress
included are Jcstress [verification test](https://github.com/isaiah-perumalla/lockfree4j/tree/main/src/jcstress) to ensure correctness of the implementation

### Jmh Benchmarks


### Examine JIT C1/C2 output

```aidl
-XX:+UnlockDiagnosticVMOptions
-XX:+TraceClassLoading
-XX:+LogCompilation
-XX:+PrintAssembly
-XX:PrintAssemblyOptions=intel
-XX:LogFile=jitBrokenlogfile.log
```
