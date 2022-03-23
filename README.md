# CS4262_ChatServer

## Project Setup

Make sure you have gradle on your machine. Original project initializing version is Gradle 7.3.3 with JVM 17.0.3

### Run Project


Initial running
``` 
./gradlew run --args="<server-id> <config-location>"
```

Initial running after failuer 
``` 
./gradlew run --args="<server-id> <config-location> 1"
```



### Build a jar
```
gradle fatJar
```