# java-crowd-control [![Build Status](https://img.shields.io/github/workflow/status/qixils/java-crowd-control/Java%20CI%20with%20Maven?event=push)](https://github.com/qixils/java-crowd-control/actions/workflows/maven.yml) [![Release](https://img.shields.io/maven-central/v/dev.qixils.crowdcontrol/java-crowd-control?color=success)](https://search.maven.org/artifact/dev.qixils.crowdcontrol/java-crowd-control) [![javadoc](https://javadoc.io/badge2/dev.qixils.crowdcontrol/java-crowd-control/javadoc.svg)](https://javadoc.io/doc/dev.qixils.crowdcontrol/java-crowd-control?color=success)

A Java library for interacting with a [crowdcontrol.live](https://crowdcontrol.live) server
which allows easy handling of requests, queuing effects to only allow one of a given type to run at once, etc.

## Installation

### Maven

Add to the `dependencies` section of your `pom.xml` file:

```xml
<groupId>dev.qixils.crowdcontrol</groupId>
<artifactId>java-crowd-control</artifactId>
<version>2.1.2</version>
```

### Gradle

Add to the `dependencies` section of your `build.gradle` file:

```gradle
compileOnly 'dev.qixils.crowdcontrol:java-crowd-control:2.1.2'
```

Or, if using Kotlin (`build.gradle.kts`):

```kts
compileOnly("dev.qixils.crowdcontrol:java-crowd-control:2.1.2")
```

## Usage

Basic usage of the library is as follows:

```java
CrowdControl crowdControl = new CrowdControl(ip, port); // create CC instance
crowdControl.registerCheck(this::gameIsLoaded); // prevent commands from executing if the game hasn't loaded
crowdControl.registerHandler("kill_player", request -> { // register handler for the effect "kill_player"
    this.getPlayer().kill(); // kill the active player
    return new Response.Builder(request).type(Response.ResultType.SUCCESS).build(); // return that the player was successfully killed
});
```

On the Crowd Control server side of things, you will need to create a .CS file which defines all of your effects.
You can test this file within the [Crowd Control SDK](https://forum.warp.world/t/how-to-setup-and-use-the-crowd-control-sdk/5121).

```cs
using System;
using System.Collections.Generic;
using CrowdControl.Common;
using CrowdControl.Games.Packs;
using ConnectorType = CrowdControl.Common.ConnectorType;

// you can rename this class to the game you are developing for
public class VideoGameHere: SimpleTCPPack
{
    // IP address and port to listen for connections on
    public override string Host => "127.0.0.1";
    public override ushort Port => 58431;

    public VideoGameHere(IPlayer player, Func<CrowdControlBlock, bool> responseHandler, Action<object> statusUpdateHandler) : base(player, responseHandler, statusUpdateHandler) { }

    // The first 4 parameters come from the CrowdControl team and are only important for officially publishing your effect pack.
    // Ask in #cc-developer on the Warp World Discord server (https://discord.gg/jE7ktx477x) if you would like to publish your pack.
    public override Game Game => new Game(111, "Video Game", "videogame", "PC", ConnectorType.SimpleTCPConnector);

    public override List<Effect> Effects => new List<Effect>
    {
        new Effect("Kill Player", "kill_player")
    };
}
```

### Javadocs

The project's API documentation can be viewed [here](https://javadoc.io/doc/dev.qixils.crowdcontrol/java-crowd-control).

## License

This project is licensed under the MIT license. The license is available to view [here](https://github.com/qixils/java-crowd-control/blob/master/LICENSE).
