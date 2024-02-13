
## Run bench with async profiler
1. build benchmarkjar `JAVA_HOME=/opt/jdks/zulu17 ./gradlew benchmarkJar`
```shell
/opt/jdks/zulu17/bin/java -jar  build/libs/benchmarks.jar f2 wi2 i3 AsciiMapBench.asciiMap -prof async:libPath=/opt/async-prof-3.0/lib/libasyncProfiler.so\;output=flamegraph\;dir=./prof-results

```