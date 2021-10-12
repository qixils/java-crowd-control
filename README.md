# java-crowd-control
A Java library for interacting with a [crowdcontrol.live](https://crowdcontrol.live) server.

## Installation

### Maven

Add to the `dependencies` section of your `pom.xml` file:

```xml
<groupId>dev.qixils.crowdcontrol</groupId>
<artifactId>java-crowd-control</artifactId>
<version>2.1.0</version>
```

### Gradle

Add to the `dependencies` section of your `build.gradle` file:

```gradle
compileOnly 'dev.qixils.crowdcontrol:java-crowd-control:2.1.0'
```

Or, if using Kotlin (`build.gradle.kts`):

```kts
compileOnly("dev.qixils.crowdcontrol:java-crowd-control:2.1.0")
```

## Usage

Basic usage:

```java
CrowdControl crowdControl = new CrowdControl(ip, port); // create CC instance
crowdControl.registerCheck(this::gameIsLoaded); // prevent commands from executing if the game hasn't loaded
crowdControl.registerHandler("kill-player", request -> { // register handler for the effect "kill-player"
    this.getPlayer().kill(); // kill the active player
    return new Response.Builder(request).type(Response.ResultType.SUCCESS).build(); // return that the player was successfully killed
});
```

### Javadocs

The project's API documentation can be viewed [here](https://crowdcontrol.qixils.dev/apidocs/).

## LICENSE

This project is licensed under the MIT license. The license is available to view [here](https://github.com/qixils/java-crowd-control/blob/master/LICENSE).
