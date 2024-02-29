# Introduction

This repository consists of several Java libraries. Most of which are intended for use by other Java
software. They are available in the Maven Central Repositories which allows for easily importing the
library into your build automation software of choice.

_Tip: GitHub has a handy Table of Contents feature available by clicking the list icon on the top
left of this document._

## Logging

The libraries maintained in this repository utilize the SLF4J logging framework. It provides
no logging implementation by default, meaning that logs will be silently ignored. To enable logging,
you must add a logging implementation to your class path which is typically done by adding the
implementation to your project's dependencies in Maven or Gradle. Popular implementations include
[Logback](https://logback.qos.ch/),
[log4j2](https://logging.apache.org/log4j/2.x/index.html),
[slf4j-simple](https://mvnrepository.com/artifact/org.slf4j/slf4j-simple),
and SLF4J's adapter for the built-in
[java.util.logging](https://mvnrepository.com/artifact/org.slf4j/slf4j-jdk14) framework.

# Receiver [![javadoc](https://javadoc.io/badge2/dev.qixils.crowdcontrol/crowd-control-receiver/javadoc.svg)](https://javadoc.io/doc/dev.qixils.crowdcontrol/crowd-control-receiver?color=success)

The **Receiver** library is the main library of the project. It allows your video game (or
modification) to receive effect requests from a streamer using the
[Crowd Control](https://crowdcontrol.live) desktop application. The set of effects supported by your
game is known as your "Effect Pack." Effect packs are written in C# and loaded into the Crowd
Control desktop app. The desktop app then communicates with your game which receives messages
through the receiver library.

Testing of effect packs is generally performed using the
[Crowd Control SDK](https://forum.warp.world/t/how-to-setup-and-use-the-crowd-control-sdk/5121).

## Installation

<details>
<summary>Maven</summary>

Add to the `dependencies` section of your `pom.xml` file:

```xml
<dependency>
    <groupId>dev.qixils.crowdcontrol</groupId>
    <artifactId>crowd-control-receiver</artifactId>
    <version>3.9.1</version>
</dependency>
```

</details>

<details>
<summary>Gradle</summary>

Add to the `dependencies` section of your `build.gradle` file:

```gradle
compileOnly 'dev.qixils.crowdcontrol:crowd-control-receiver:3.9.1'
```

Or, if using Kotlin (`build.gradle.kts`):

```kts
compileOnly("dev.qixils.crowdcontrol:crowd-control-receiver:3.9.1")
```

</details>

## Usage

To start, you must create a
[CrowdControl](https://javadoc.io/doc/dev.qixils.crowdcontrol/crowd-control-receiver/latest/dev/qixils/crowdcontrol/CrowdControl.html)
class using one of the two builder methods. This differs depending on which Crowd Control connection
type you wish to use. The two connection types are outlined below.

You will also need to create a `.cs` file which holds a list of all of your available effects as
well as information about how the Crowd Control desktop app should connect to your application.
Example files for each connector type are provided below.

### Client Mode

This builder corresponds with the default `SimpleTCPConnector` used by Crowd Control. In this mode,
the library runs as a client, meaning it receives commands from a central server that it connects
to. This central server is (usually) the streamer's Crowd Control desktop application running on a
local port.

To use this connector type, use the Crowd Control
[client builder](https://javadoc.io/doc/dev.qixils.crowdcontrol/crowd-control-receiver/latest/dev/qixils/crowdcontrol/CrowdControlClientBuilder.html):

```java
import dev.qixils.crowdcontrol.CrowdControl;
// [...]
String ip = this.getIP(); // this should ideally be configurable by the streamer
CrowdControl crowdControl = CrowdControl.client()
        .ip(ip)
        .port(58429) // this should match what's in your .cs file
        .build();
```

You must also use the `SimpleTCPConnector` type inside your effect pack's C# file that gets loaded
into the Crowd Control app. Example:

<!-- TODO: Update for CC 2.0 -->

```cs
using System;
using System.Collections.Generic;
using CrowdControl.Common;

namespace CrowdControl.Games.Packs
{
    public class VideoGameNameHere : SimpleTCPPack
    {
        public override string Host => "127.0.0.1";

        public override ushort Port => 58429;

        public VideoGameNameHere(IPlayer player, Func<CrowdControlBlock, bool> responseHandler, Action<object> statusUpdateHandler) : base(player, responseHandler, statusUpdateHandler) { }

        // The first three fields of the Game constructor will be assigned to you by the Crowd Control staff.
        // For more information, join the Discord server: https://discord.gg/DhnfpEqmtn
        public override Game Game => new Game(999, "Video Game Name Here", "VideoGameNameHere", "PC", ConnectorType.SimpleTCPConnector);

        // Define all effects used in your effect pack here
        public override List<Effect> Effects => new List<Effect>
        {
            // define "Miscellaneous" folder of effects
            new Effect("Miscellaneous", "miscellaneous", ItemKind.Folder),

            // define effects inside this folder
            new Effect("Display Name 1", "effect_key_1", "miscellaneous"),
            new Effect("Display Name 2", "effect_key_2", "miscellaneous"),
        };
    }
}
```

### Server Mode

This builder corresponds with the `SimpleTCPClientConnector`. In this mode, the library runs as a
server, meaning it can establish connections with multiple clients (streamers) and process effects
for each streamer individually. This may be ideal for multiplayer game servers like Minecraft.

To use this connector type, use the Crowd Control
[server builder](https://javadoc.io/doc/dev.qixils.crowdcontrol/crowd-control-receiver/latest/dev/qixils/crowdcontrol/CrowdControlServerBuilder.html):

<!-- TODO: update example to use new configurable host/port system -->

```java
import dev.qixils.crowdcontrol.CrowdControl;
// [...]
String password = this.getPassword(); // this should ideally be configurable by the server host
CrowdControl crowdControl = CrowdControl.server()
        .password(password)
        .port(58429) // this should match what's in your .cs file
        .build();
```

You must also define several variables inside your effect pack's C# file that gets loaded into the
Crowd Control app. Example:

```cs
using System;
using System.Collections.Generic;
using ConnectorLib;
using CrowdControl.Common;
using CrowdControl.Games.Packs;
using ConnectorType = CrowdControl.Common.ConnectorType;

namespace CrowdControl.Games.Packs
{
    public class VideoGameNameHere : SimpleTCPPack<SimpleTCPClientConnector>
    {
        public override string Host => "127.0.0.1";

        public override ushort Port => 58429;

        public override ISimpleTCPPack.AuthenticationType AuthenticationMode => ISimpleTCPPack.AuthenticationType.SimpleTCPSendKey;

        public override ISimpleTCPPack.DigestAlgorithm AuthenticationHashMode => ISimpleTCPPack.DigestAlgorithm.SHA_512;

        public VideoGameNameHere(IPlayer player, Func<CrowdControlBlock, bool> responseHandler, Action<object> statusUpdateHandler) : base(player, responseHandler, statusUpdateHandler) { }

        // The first three fields of the Game constructor will be assigned to you by the Crowd Control staff.
        // For more information, join the Discord server: https://discord.gg/DhnfpEqmtn
        public override Game Game => new Game(999, "Video Game Name Here", "VideoGameNameHere", "PC", ConnectorType.SimpleTCPClientConnector);

        // Define all effects used in your effect pack here
        public override List<Effect> Effects => new List<Effect>
        {
            // define "Miscellaneous" folder of effects
            new Effect("Miscellaneous", "miscellaneous", ItemKind.Folder),

            // define effects inside this folder
            new Effect("Display Name 1", "effect_key_1", "miscellaneous"),
            new Effect("Display Name 2", "effect_key_2", "miscellaneous"),
        };
    }
}
```

### General Usage

Once you've created your CrowdControl instance, you can start registering handlers for each of your
effects.

#### Registering Individual Handlers

Two overloaded methods are provided for registering an effect handler. You can either provide a
[`Function<Request,Response>`](https://javadoc.io/doc/dev.qixils.crowdcontrol/crowd-control-receiver/latest/dev/qixils/crowdcontrol/CrowdControl.html#registerHandler(java.lang.String,java.util.function.Function))
which is a function that takes in a
[Request](https://javadoc.io/doc/dev.qixils.crowdcontrol/crowd-control-pojos/latest/dev/qixils/crowdcontrol/socket/Request.html)
and returns a
[Response](https://javadoc.io/doc/dev.qixils.crowdcontrol/crowd-control-pojos/latest/dev/qixils/crowdcontrol/socket/Response.html),
or a
[`Consumer<Request>`](https://javadoc.io/doc/dev.qixils.crowdcontrol/crowd-control-receiver/latest/dev/qixils/crowdcontrol/CrowdControl.html#registerHandler(java.lang.String,java.util.function.Consumer))
which takes in a
[Request](https://javadoc.io/doc/dev.qixils.crowdcontrol/crowd-control-pojos/latest/dev/qixils/crowdcontrol/socket/Request.html)
and returns nothing. As these functions are called on an asynchronous thread, the latter is usually
used for effects that require an action to be run on the main thread. It is expected that
[`Consumer<Request>`](https://javadoc.io/doc/dev.qixils.crowdcontrol/crowd-control-receiver/latest/dev/qixils/crowdcontrol/CrowdControl.html#registerHandler(java.lang.String,java.util.function.Consumer))
handlers will eventually call
[`Response#send`](https://javadoc.io/doc/dev.qixils.crowdcontrol/crowd-control-pojos/latest/dev/qixils/crowdcontrol/socket/Response.html#send())
to manually issue a
[Response](https://javadoc.io/doc/dev.qixils.crowdcontrol/crowd-control-pojos/latest/dev/qixils/crowdcontrol/socket/Response.html)
to the requesting client.

#### Registering Handler Classes

Those who prefer working with annotated methods can use
[`#registerHandlers(Object)`](https://javadoc.io/doc/dev.qixils.crowdcontrol/crowd-control-receiver/latest/dev/qixils/crowdcontrol/CrowdControl.html#registerHandlers(java.lang.Object))
to automatically register effect handlers for every appropriately annotated method inside a class.
All methods that are annotated with
[@Subscribe](https://javadoc.io/doc/dev.qixils.crowdcontrol/crowd-control-receiver/latest/dev/qixils/crowdcontrol/Subscribe.html);
return
[Response](https://javadoc.io/doc/dev.qixils.crowdcontrol/crowd-control-pojos/latest/dev/qixils/crowdcontrol/socket/Response.html),
[Response.Builder](https://javadoc.io/doc/dev.qixils.crowdcontrol/crowd-control-pojos/latest/dev/qixils/crowdcontrol/socket/Response.Builder.html),
or Void (for synchronous effects that will manually call
[`Response#send`](https://javadoc.io/doc/dev.qixils.crowdcontrol/crowd-control-pojos/latest/dev/qixils/crowdcontrol/socket/Response.html#send()));
have a single parameter that accepts only
[Request](https://javadoc.io/doc/dev.qixils.crowdcontrol/crowd-control-pojos/latest/dev/qixils/crowdcontrol/socket/Request.html);
and are public will be registered as effect handlers.

For more information, please view
[the method's javadocs](https://javadoc.io/doc/dev.qixils.crowdcontrol/crowd-control-receiver/latest/dev/qixils/crowdcontrol/CrowdControl.html#registerHandlers(java.lang.Object)).

#### Registering Checks

Checks are functions that are called every time a
[Request](https://javadoc.io/doc/dev.qixils.crowdcontrol/crowd-control-pojos/latest/dev/qixils/crowdcontrol/socket/Request.html)
is received which may or may not have knowledge of the Request. They are used to prevent the
execution of effects if your game has not yet loaded or if your players have not yet connected.
These can be registered using
[`#registerCheck(Supplier<CheckResult>)`](https://javadoc.io/doc/dev.qixils.crowdcontrol/crowd-control-receiver/latest/dev/qixils/crowdcontrol/CrowdControl.html#registerCheck(java.util.function.Supplier))
and
[`#registerCheck(Function<Request,CheckResult>)`](https://javadoc.io/doc/dev.qixils.crowdcontrol/crowd-control-receiver/latest/dev/qixils/crowdcontrol/CrowdControl.html#registerCheck(java.util.function.Function))

#### Further Reading

The documentation for all classes and methods may be found
[here](https://javadoc.io/doc/dev.qixils.crowdcontrol/crowd-control-receiver/latest/index.html).

### Example

An example project integrating the receiver library can be found
[here](https://github.com/qixils/java-crowd-control/blob/master/receiver/src/test/java/dev/qixils/crowdcontrol/ExampleProject.java).

# Sender [![javadoc](https://javadoc.io/badge2/dev.qixils.crowdcontrol/crowd-control-sender/javadoc.svg)](https://javadoc.io/doc/dev.qixils.crowdcontrol/crowd-control-sender?color=success)

The **Sender** library allows you to simulate the Crowd Control desktop application by sending
effect requests to a video game server or clients. This was primarily developed as an internal tool
to test the **Receiver** library, though it can be used for numerous other applications.

## Installation

<details>
<summary>Maven</summary>

Add to the `dependencies` section of your `pom.xml` file:

```xml
<dependency>
    <groupId>dev.qixils.crowdcontrol</groupId>
    <artifactId>crowd-control-sender</artifactId>
    <version>3.9.1</version>
</dependency>
```

</details>

<details>
<summary>Gradle</summary>

Add to the `dependencies` section of your `build.gradle` file:

```gradle
compileOnly 'dev.qixils.crowdcontrol:crowd-control-sender:3.9.1'
```

Or, if using Kotlin (`build.gradle.kts`):

```kts
compileOnly("dev.qixils.crowdcontrol:crowd-control-sender:3.9.1")
```

</details>

## Usage

This section assumes you have read the above documentation for the **Receiver** library and are
familiar with the two different types of TCP connectors.

To create a server for the `SimpleTCPConnector`, you should instantiate a new
[`SimulatedServer`](https://javadoc.io/doc/dev.qixils.crowdcontrol/crowd-control-sender/latest/dev/qixils/crowdcontrol/socket/SimulatedServer.html).
For creating a client (or several) for the `SimpleTCPClientConnector`, you should instantiate a new
[`SimulatedClient`](https://javadoc.io/doc/dev.qixils.crowdcontrol/crowd-control-sender/latest/dev/qixils/crowdcontrol/socket/SimulatedClient.html).
Both of these classes implement
[`StartableService`](https://javadoc.io/doc/dev.qixils.crowdcontrol/crowd-control-sender/latest/dev/qixils/crowdcontrol/StartableService.html)
which holds all the important methods for working with these simulated services.

To start the service, you must call the
[`#start`](https://javadoc.io/doc/dev.qixils.crowdcontrol/crowd-control-sender/latest/dev/qixils/crowdcontrol/StartableService.html#start())
method on your service. For clients, you may call the
[`#autoStart`](https://javadoc.io/doc/dev.qixils.crowdcontrol/crowd-control-sender/latest/dev/qixils/crowdcontrol/AutomatableService.html#autoStart())
method to automatically reconnect to the server when the connection is lost.

Requests may be dispatched to connected services using the
[`#sendRequest`](https://javadoc.io/doc/dev.qixils.crowdcontrol/crowd-control-sender/latest/dev/qixils/crowdcontrol/SimulatedService.html#sendRequest(dev.qixils.crowdcontrol.socket.Request.Builder))
method. For the server, this will return a Flux of Fluxes of Responses which correspond to the
Responses received from the connected clients. The client will return a single Flux of Responses.

The Response Fluxes will emit a Response each time one corresponding to the Request is received from
the connected service, and it will complete upon a Response indicating that the effect has finished
executing. It may also error if the effect times out or the connected service disconnects before
completion.

Before sending Requests, you should ensure that the effect being requested is known to be available
by checking the result of
[#isEffectAvailable](https://javadoc.io/doc/dev.qixils.crowdcontrol/crowd-control-sender/latest/dev/qixils/crowdcontrol/SimulatedService.html#isEffectAvailable(java.lang.String)).
If the server states that an effect does not exist, then it will be marked as unavailable and the
`#sendRequest` method will throw an
[EffectUnavailableException](https://javadoc.io/doc/dev.qixils.crowdcontrol/crowd-control-sender/latest/dev/qixils/crowdcontrol/exceptions/EffectUnavailableException.html)
if you try to use it.

### Further Reading

The documentation for all classes and methods may be found
[here](https://javadoc.io/doc/dev.qixils.crowdcontrol/crowd-control-sender/latest/index.html).
