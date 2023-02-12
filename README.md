# shareit4j



### Examine JIT C1/C2 output

```aidl
-XX:+UnlockDiagnosticVMOptions
-XX:+TraceClassLoading
-XX:+LogCompilation
-XX:+PrintAssembly
-XX:PrintAssemblyOptions=intel
-XX:LogFile=jitBrokenlogfile.log
```