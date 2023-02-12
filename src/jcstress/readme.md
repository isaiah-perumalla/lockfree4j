## Examples of broken implementations 

implement share record where single write updates the record and multiple reader read

to run stress test 

```
`./gradlew jcstress --tests VolatileRecordStress`
```

### Test results for UnsafeRecord impl
All test and benchmarks run on a **Bare Metal** intel x86 machine
```aidl
.......... [OK] com.isaiahp.jcstress.UnsafeRecordStress

  Results across all configurations:

          RESULT        SAMPLES     FREQ       EXPECT  DESCRIPTION
  -1, -1, -1, -1              0    0.00%   Acceptable  Correctly read intact values
  -1, -1, -2, -2              0    0.00%   Acceptable  Correctly read intact values
    -1, -1, 0, 0     65,307,366    2.31%   Acceptable  Correctly read intact values
  -2, -2, -1, -1              0    0.00%   Acceptable  Correctly read intact values
  -2, -2, -2, -2              0    0.00%   Acceptable  Correctly read intact values
    -2, -2, 0, 0  2,514,958,799   89.10%   Acceptable  Correctly read intact values
    0, 0, -1, -1              0    0.00%   Acceptable  Correctly read intact values
    0, 0, -2, -2              0    0.00%   Acceptable  Correctly read intact values
      0, 0, 0, 0    242,363,211    8.59%  Interesting  Read unsuccessful while writer updated


  Failed tests: No matches.

  Error tests: No matches.

```
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

