package dev.qixils.crowdcontrol;

import dev.qixils.crowdcontrol.exceptions.ExceptionUtil;
import dev.qixils.crowdcontrol.socket.Request;
import dev.qixils.crowdcontrol.socket.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A three-dimensional integer vector.
 */
final class Vec3i {
	int x;
	int y;
	int z;
}

/**
 * A world in the game.
 */
interface World {

	/**
	 * Plants a random tree at the given location.
	 */
	boolean plantRandomTree(Vec3i position);
}

/**
 * A player connected to the game.
 */
interface Player {

	/**
	 * Gets whether the player is alive.
	 */
	boolean isAlive();

	/**
	 * Kills the player.
	 */
	void kill();

	/**
	 * Gets the world that the player is in.
	 */
	World getWorld();

	/**
	 * Gets the position of the player.
	 */
	Vec3i getPosition();

	/**
	 * Gets the ID of the player.
	 */
	UUID getId();

	/**
	 * Fetch the stream channel names that the player has indicated they wish to receive
	 * effects from (i.e. via a chat command or a config file).
	 */
	Set<String> getSubscribedChannels();
}

/**
 * An event that is fired when a player jumps.
 */
interface PlayerJumpEvent {

	/**
	 * Gets the player that jumped.
	 */
	Player getPlayer();

	/**
	 * Gets whether the jump is cancelled.
	 */
	boolean isCancelled();

	/**
	 * Sets whether the jump is cancelled.
	 */
	void setCancelled(boolean cancelled);
}

/**
 * Marks a handler of game events.
 */
@interface EventHandler {
}

/**
 * The game instance.
 */
abstract class Game {

	/**
	 * The logger instance.
	 */
	final Logger logger = LoggerFactory.getLogger("Game");

	Game() {
		initEffects();
	}

	/**
	 * Gets the Crowd Control instance.
	 */
	abstract CrowdControl getCrowdControl();

	/**
	 * Gets whether the game has loaded.
	 */
	abstract boolean gameIsLoaded();

	/**
	 * Runs a task on the main thread.
	 */
	abstract void runOnMainThread(Runnable runnable);

	/**
	 * Registers a listener for game events.
	 */
	abstract void registerGameEventListener(Object listener);

	/**
	 * Gets a collection of players in the game.
	 */
	abstract Collection<Player> getPlayers();

	/**
	 * Initializes effects and checks for Crowd Control.
	 */
	void initEffects() {

		// Gets or initializes the Crowd Control instance
		CrowdControl crowdControl = getCrowdControl();

		// Register a check to ensure the game has loaded & living players are online
		crowdControl.registerCheck(() -> gameIsLoaded() && getLivingPlayers().size() > 0 ? CheckResult.ALLOW : CheckResult.DISALLOW);

		// Register a check to ensure that requests have a target
		crowdControl.registerCheck(request -> getPlayersFromRequest(request).isEmpty() ? CheckResult.DISALLOW : CheckResult.ALLOW);

		// Register an effect that kills player(s)
		crowdControl.registerHandler("kill_player", request -> {
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
		crowdControl.registerHandler("plant_tree", request -> {
			runOnMainThread(() -> {
				// See above for reasons why tracking success is important
				boolean success = false;
				for (Player player : getPlayersFromRequest(request)) {
					boolean treeSuccess = player.getWorld().plantRandomTree(player.getPosition());
					if (treeSuccess) {
						success = true;
					}
				}

				// Determine response based on success of command
				Response.Builder response = success
						? request.buildResponse().type(Response.ResultType.SUCCESS)
						// instead of immediately refunding the purchaser, we can return RETRY to try
						//  to plant a tree again in a few seconds
						: request.buildResponse().type(Response.ResultType.RETRY).message("No available location to plant tree(s)");
				response.send();
			});
			// This lambda method is a Consumer<Request>, so no return statement is necessary
		});

		// Registers an effect that temporarily disables jumping
		DisableJumpEffect disableJumpingEffect = new DisableJumpEffect(this);
		crowdControl.registerHandlers(disableJumpingEffect);
		registerGameEventListener(disableJumpingEffect);
	}

	/**
	 * Gets a collection of all living players in the game.
	 */
	Collection<Player> getLivingPlayers() {
		return getPlayers().stream().filter(Player::isAlive).collect(Collectors.toList());
	}

	/**
	 * Converts a Crowd Control Target (a streamer) to a Player, if online.
	 * Used when running the Crowd Control library in server-mode.
	 */
	Collection<Player> getPlayersFromTarget(Request.Target target) {
		List<Player> targetedPlayers = new ArrayList<>();
		for (Player player : getLivingPlayers()) {
			Collection<String> accounts = player.getSubscribedChannels();
			if (accounts.contains(target.getId())
					|| accounts.contains(target.getName())
					|| accounts.contains(target.getLogin())
			) {
				targetedPlayers.add(player);
			}
		}
		return targetedPlayers;
	}

	/**
	 * Gets a collection of players being targeted by an effect.
	 */
	Collection<Player> getPlayersFromRequest(Request request) {
		// TODO: Use new Sources API
		// Global requests apply to every player on the server
		if (request.isGlobal()) {
			return getLivingPlayers();
		}

		// Convert array of targets to list of players
		Request.Target[] targets = request.getTargets();
		Set<Player> players = new HashSet<>();
		for (Request.Target target : targets) {
			Collection<Player> targetedPlayers = getPlayersFromTarget(target);
			if (!targetedPlayers.isEmpty()) {
				players.addAll(targetedPlayers);
			} else {
				logger.warn("Target {} is not online", target);
			}
		}

		if (players.isEmpty()) {
			logger.warn("No players were targeted by effect {}", request.getEffect());
		}

		return players;
	}
}

/**
 * An effect which prevents players from jumping for a short period of time.
 */
class DisableJumpEffect {
	private static final Duration EFFECT_DURATION = Duration.ofSeconds(5);
	private static final Set<UUID> DISABLED_PLAYERS = new HashSet<>();
	final Game game;

	DisableJumpEffect(Game game) {
		this.game = game;
	}

	private void disableFor(Request request) {
		for (Player player : game.getPlayersFromRequest(request)) {
			DISABLED_PLAYERS.add(player.getId());
		}
	}

	private void enableFor(Request request) {
		for (Player player : game.getPlayersFromRequest(request)) {
			DISABLED_PLAYERS.remove(player.getId());
		}
	}

	// Game Listener
	@EventHandler
	public void onJump(PlayerJumpEvent event) {
		if (DISABLED_PLAYERS.contains(event.getPlayer().getId())) {
			event.setCancelled(true);
		}
	}

	// Crowd Control effect handler
	@Subscribe("disable_jump")
	public void onEffect(Request request) {
		// Timed effect prevents multiple of the same effect from running at the same time
		new TimedEffect.Builder()
				.request(request)
				.duration(ExceptionUtil.validateNotNullElse(request.getDuration(), EFFECT_DURATION))
				.startCallback(effect -> {
					disableFor(request);
					return request.buildResponse(); // defaults to success
				})
				.pauseCallback(effect -> enableFor(request))
				.resumeCallback(effect -> disableFor(request))
				.completionCallback(effect -> enableFor(request))
				.build()
				.queue();
	}
}
