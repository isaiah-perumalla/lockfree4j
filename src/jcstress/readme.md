## Examples of broken implementations 

implement share record where single write updates the record and multiple reader read

to run stress test 

```
./gradlew jcstress --test VolatileRecordStress
```