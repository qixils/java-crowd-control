# Usage

This software is a Java library intended for use by other Java software.
It is available in the Maven Central repositories which allows for easily
importing the library into your build automation software of choice.

### Maven

Add to the `dependencies` section of your `pom.xml` file:

```xml
<dependency>
    <groupId>dev.qixils.crowdcontrol</groupId>
    <artifactId>java-crowd-control</artifactId>
    <version>3.2.1</version>
</dependency>
```

### Gradle

Add to the `dependencies` section of your `build.gradle` file:

```gradle
compileOnly 'dev.qixils.crowdcontrol:java-crowd-control:3.2.1'
```

Or, if using Kotlin (`build.gradle.kts`):

```kts
compileOnly("dev.qixils.crowdcontrol:java-crowd-control:3.2.1")
```

## Quick Start

To start, you must create a [CrowdControl](https://javadoc.io/doc/dev.qixils.crowdcontrol/java-crowd-control/latest/dev/qixils/crowdcontrol/CrowdControl.html)
class using one of the two builder methods. This differs depending on
which Crowd Control connection type you wish to use. The two connection
types are outlined below.

You will also need to create a `.cs` file which holds a list of all of
your available effects as well as information about how the Crowd
Control desktop app should connect to your application. Example files
for each connector type are provided below.

### Client Mode

This builder corresponds with the default `SimpleTCPConnector` used by
Crowd Control. In this mode, the library runs as a client, meaning it
receives commands from a central server that it connects to. This
central server is (usually) the streamer's Crowd Control desktop
application running on a local port.

To use this connector type, use the Crowd Control [client builder](https://javadoc.io/doc/dev.qixils.crowdcontrol/java-crowd-control/latest/dev/qixils/crowdcontrol/builder/CrowdControlClientBuilder.html):

```java
import dev.qixils.crowdcontrol.CrowdControl;
// [...]
String ip = this.getIP(); // this should ideally be configurable by the streamer
CrowdControl crowdControl = CrowdControl.client()
        .ip(ip)
        .port(58429) // this should match what's in your .cs file
        .build();
```

You must also use the `SimpleTCPConnector` type inside your effect
pack's C# file that gets loaded into the Crowd Control app. Example:

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

This builder corresponds with the `SimpleTCPClientConnector`. In this
mode, the library runs as a server, meaning it can establish connections
with multiple clients (streamers) and process effects for each streamer
individually. This may be ideal for multiplayer game servers like
Minecraft.

To use this connector type, use the Crowd Control [server builder](https://javadoc.io/doc/dev.qixils.crowdcontrol/java-crowd-control/latest/dev/qixils/crowdcontrol/builder/CrowdControlServerBuilder.html):

```java
import dev.qixils.crowdcontrol.CrowdControl;
// [...]
String password = this.getPassword(); // this should ideally be configurable by the server host
CrowdControl crowdControl = CrowdControl.server()
        .password(password)
        .port(58429) // this should match what's in your .cs file
        .build();
```

You must also define several variables inside your effect
pack's C# file that gets loaded into the Crowd Control app.
Example:

```cs
using System;
using System.Collections.Generic;
using CrowdControl.Common;

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

Once you've created your CrowdControl instance, you can start registering handlers
for each of your effects.

#### Registering Individual Handlers

Two overloaded methods are provided for registering an effect handler.
You can either provide a [`Function<Request,Response>`](https://javadoc.io/doc/dev.qixils.crowdcontrol/java-crowd-control/latest/dev/qixils/crowdcontrol/CrowdControl.html#registerHandler(java.lang.String,java.util.function.Function))
which is a function that takes in a [Request](https://javadoc.io/doc/dev.qixils.crowdcontrol/java-crowd-control/latest/dev/qixils/crowdcontrol/socket/Request.html)
and returns a [Response](https://javadoc.io/doc/dev.qixils.crowdcontrol/java-crowd-control/latest/dev/qixils/crowdcontrol/socket/Response.html),
or a [`Consumer<Request>`](https://javadoc.io/doc/dev.qixils.crowdcontrol/java-crowd-control/latest/dev/qixils/crowdcontrol/CrowdControl.html#registerHandler(java.lang.String,java.util.function.Consumer))
which takes in a [Request](https://javadoc.io/doc/dev.qixils.crowdcontrol/java-crowd-control/latest/dev/qixils/crowdcontrol/socket/Request.html)
and returns nothing. As these functions are called on an asynchronous thread, the latter
is usually used for effects that require an action to be run on the main thread. It is
expected that [`Consumer<Request>`](https://javadoc.io/doc/dev.qixils.crowdcontrol/java-crowd-control/latest/dev/qixils/crowdcontrol/CrowdControl.html#registerHandler(java.lang.String,java.util.function.Consumer))
handlers will eventually call [`Response#send`](https://javadoc.io/doc/dev.qixils.crowdcontrol/java-crowd-control/latest/dev/qixils/crowdcontrol/socket/Response.html#send())
to manually issue a [Response](https://javadoc.io/doc/dev.qixils.crowdcontrol/java-crowd-control/latest/dev/qixils/crowdcontrol/socket/Response.html)
to the requesting client.

#### Registering Handler Classes

Those who prefer working with annotated methods can use [`#registerHandlers(Object)`](https://javadoc.io/doc/dev.qixils.crowdcontrol/java-crowd-control/latest/dev/qixils/crowdcontrol/CrowdControl.html#registerHandlers(java.lang.Object))
to automatically register effect handlers for every appropriately annotated method
inside a class. All methods that are annotated with [@Subscribe](https://javadoc.io/doc/dev.qixils.crowdcontrol/java-crowd-control/latest/dev/qixils/crowdcontrol/Subscribe.html);
return [Response](https://javadoc.io/doc/dev.qixils.crowdcontrol/java-crowd-control/latest/dev/qixils/crowdcontrol/socket/Response.html),
[Response.Builder](https://javadoc.io/doc/dev.qixils.crowdcontrol/java-crowd-control/latest/dev/qixils/crowdcontrol/socket/Response.Builder.html),
or Void (for synchronous effects that will manually call [`Response#send`](https://javadoc.io/doc/dev.qixils.crowdcontrol/java-crowd-control/latest/dev/qixils/crowdcontrol/socket/Response.html#send()));
have a single parameter that accepts only [Request](https://javadoc.io/doc/dev.qixils.crowdcontrol/java-crowd-control/latest/dev/qixils/crowdcontrol/socket/Request.html);
and are public will be registered as effect handlers.

For more information, please view [the method's javadocs](https://javadoc.io/doc/dev.qixils.crowdcontrol/java-crowd-control/latest/dev/qixils/crowdcontrol/CrowdControl.html#registerHandlers(java.lang.Object)).

#### Registering Checks

Checks are functions that are called every time
a [Request](https://javadoc.io/doc/dev.qixils.crowdcontrol/java-crowd-control/latest/dev/qixils/crowdcontrol/socket/Request.html)
is received which may or may not have knowledge of the Request. They are used to prevent the
execution of effects if your game has not yet loaded or if your players have not yet connected.
These can be registered
using [`#registerCheck(Supplier<CheckResult>)`](https://javadoc.io/doc/dev.qixils.crowdcontrol/java-crowd-control/latest/dev/qixils/crowdcontrol/CrowdControl.html#registerCheck(java.util.function.Supplier))
and [`#registerCheck(Function<Request,CheckResult>)`](https://javadoc.io/doc/dev.qixils.crowdcontrol/java-crowd-control/latest/dev/qixils/crowdcontrol/CrowdControl.html#registerCheck(java.util.function.Function))

#### Further Reading

The documentation for all classes and methods may be found [here](https://javadoc.io/doc/dev.qixils.crowdcontrol/java-crowd-control/latest/index.html).

### Example

The following code demonstrates what a very simple usage of this library may look like.
It does not correspond to any specific video game.
~~Any resemblance to Minecraft is purely circumstantial.~~

```java
/**
 * Initializes effects and checks for Crowd Control.
 */
void initEffects() {
  // Gets or initializes the Crowd Control instance
  CrowdControl crowdControl = getCrowdControl();

  // Register a check to ensure the game has loaded & living players are online
  crowdControl.registerCheck(() -> gameIsLoaded() && getLivingPlayers() > 0);
  
  // Register an effect that kills player(s)
		crowdControl.registerEffect("kill_player",request->{
		// Keeping track of whether something actually happens as a result of using
		//  this effect is critical for Crowd Control. If an effect does not apply,
    //  the handler should either reply with FAILURE to refund the purchaser or RETRY
    //  to attempt to apply the effect again in a few seconds if it is likely to work
    //  in the near future.
    boolean success = false;

    for (Player player : getPlayersFromRequest(request)) {
      if (player.isAlive()) {
        success = true;
        player.kill();
      }
    }

    if (success) {
      return request.buildResponse().type(Response.ResultType.SUCCESS).build();
    } else {
      return request.buildResponse().type(Response.ResultType.FAILURE).message("All targets are dead").build();
    }
  });
  
  // Register an effect that builds a complex structure in the game world which needs
  //  to be executed on the main thread
  crowdControl.registerEffect("plant_tree", request -> {
    runOnMainThread(() -> {
      // See above for reasons why tracking success is important
      boolean success = false;
      for (Player player : getPlayersFromRequest(request)) {
        boolean treeSuccess = player.getWorld().plantRandomTree(player.getLocation());
        if (treeSuccess) {
          success = true;
        }
      }

		// Determine response based on success of command
		Response.Builder response=success
		?request.buildResponse().type(Response.ResultType.SUCCESS)
		// instead of immediately refunding the purchaser, we can return RETRY to try
		//  to plant a tree again in a few seconds
		:request.buildResponse().type(Response.ResultType.RETRY).message("No available location to plant tree(s)");
		response.send();
		});
		// This lambda method is a Consumer<Request>, so no return statement is necessary
		});

		// Registers an effect that temporarily disables jumping
		DisableJumpingEffect disableJumpingEffect=new DisableJumpingEffect();
		crowdControl.registerEffects(disableJumpingEffect);
		registerGameEventListener(disableJumpingEffect);
		}

/**
 * Converts a Crowd Control Target (a streamer) to a Player, if online.
 * Used when running the Crowd Control library in server-mode.
 */
		Player getPlayerFromTarget(Request.Target target){
		for(Player player:getPlayers()){
		TwitchAccount account=player.getTwitchAccount();
		if(account!=null&&account.getId()==target.getId()){
      return player;
    }
  }
  return null;
}

/**
 * Gets a collection of players being targeted by an effect.
 */
Collection<Player> getPlayersFromRequest(Request request) {
  // Global requests apply to every player on the server
  if (request.isGlobal()) {
    return getPlayers();
  }
  
  // Convert array of targets to list of players
  Request.Target[] targets = request.getTargets();
  List<Player> players = new ArrayList(targets.length);
  for (Response.Target target : targets) {
    Player player = getPlayerFromTarget(target);
    if (player != null)  {
      players.add();
    } else {
		// insert error logging/warnings here
		}
		}

		if(players.isEmpty()){
		// insert error logging/warnings here
		}

		return players;
		}

static class DisableJumpEffect {
	private static final Duration EFFECT_DURATION = Duration.ofSeconds(5);
	private Set<Integer> disabledPlayers = new HashSet<>();

	// Game Listener
	@EventHandler
	public void onJump(PlayerJumpEvent event) {
		if (disabledPlayers.contains(event.getPlayer().getEntityId())) {
			event.setCancelled(true);
		}
	}

	// Crowd Control effect handler
	@Subscribe("disable_jump")
	public void onEffect(Request request) {
		// timed effect prevents multiple of the same effect from running at the same time
		new TimedEffect(request, EFFECT_DURATION, $ -> {
			// Get the player(s) being targeted by the effect
			for (Player player : getPlayersFromRequest(request)) {
				disabledPlayers.add(player.getEntityId());
			}
		}, $ -> {
			for (Player player : getPlayersFromRequest(request)) {
				disabledPlayers.remove(player.getEntityId());
			}
		}).queue();
	}
}
```
