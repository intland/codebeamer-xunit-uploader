# codeBeamer xUnit Importer Plugin

This is the repository for the xUnit Uploader Plugin for [codeBeamer ALM](https://intland.com)

## Debugging

Example:

```
export GRADLE_OPTS=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005
./gradlew -Dstapler.jelly.noCache=false server --no-daemon -Djenkins.httpPort=8081
```

Then setup a remote-debugging session.
