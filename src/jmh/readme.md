## Benchmarks
1. build/create
`./gradlew benchmarkJar`
2. run bench `java -jar ./build/libs/benchmarks.jar <jhm_class>`

### Run JIT watch

`./gradlew build run`
JVM flag to log compilation
```
-XX:+UnlockDiagnosticVMOptions
-XX:+TraceClassLoading
-XX:+LogCompilation
-XX:+PrintAssembly
-XX:PrintAssemblyOptions=intel
-XX:LogFile=jitlogfile.log
```