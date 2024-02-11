
## Run bench with async profiler
```shell
opt/jdks/zulu17/bin/java -jar  build/libs/benchmarks.jar f2 wi2 i3 AsciiMapBench.asciiMap -prof async:libPath=/opt/async-prof-3.0/lib/libasyncProfiler.so\;output=flamegraph\;dir=./prof-results

```