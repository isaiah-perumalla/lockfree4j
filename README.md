# SeqLock4j

Various implementation of SeqLock in Java, it mostly there to study the Java memory model.
SeqLock is a type of reader/writer synchronisation structure , it is a useful data structures if read are far more frequent than they are
written. The implementation allows for a single writer thread and many reader threads.
Writer thread is  **wait-free** , while the Reader thread(s) are **lock-free**.
Reader might fail to aquire a consistent view, and should retry. 


## Implementations
There are several implementations, including some [broken versions]() , this is there to study and understand the Java memory model .


### Examine JIT C1/C2 output

```aidl
-XX:+UnlockDiagnosticVMOptions
-XX:+TraceClassLoading
-XX:+LogCompilation
-XX:+PrintAssembly
-XX:PrintAssemblyOptions=intel
-XX:LogFile=jitBrokenlogfile.log
```
