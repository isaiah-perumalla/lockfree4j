## Benchmarks
1. build/create
`./gradlew benchmarkJar`
2. run bench `java -jar ./build/libs/benchmarks.jar <jhm_class>`

### Test machine Specs
```
Vendor ID:               GenuineIntel
Model name:            Intel(R) Xeon(R) E-2236 CPU @ 3.40GHz
CPU family:          6
Model:               158
Thread(s) per core:  2
Core(s) per socket:  6
Socket(s):           1

```

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

#### Volatile Reads on x86
Volatile reads on x86 are just mov instructions and similar to an ordinary read
```aidl
VolatileBenchmark.baseLineRead                 0               N/A  avgt    5    1.850 ±   0.001  ns/op
VolatileBenchmark.baseLineRead                 1               N/A  avgt    5    1.850 ±   0.001  ns/op
VolatileBenchmark.baseLineReadModifyWrite      0               N/A  avgt    5    1.934 ±   0.001  ns/op
VolatileBenchmark.baseLineReadModifyWrite      1               N/A  avgt    5    1.935 ±   0.001  ns/op
VolatileBenchmark.volatileRead                 0               N/A  avgt    5    2.051 ±   0.001  ns/op
VolatileBenchmark.volatileRead                 1               N/A  avgt    5    2.051 ±   0.001  ns/op
VolatileBenchmark.volatileReadModifyWrite      0               N/A  avgt    5    6.153 ±   0.001  ns/op
VolatileBenchmark.volatileReadModifyWrite      1               N/A  avgt    5    6.153 ±   0.001  ns/op

```