package dev.qixils.crowdcontrol;

import dev.qixils.crowdcontrol.exceptions.ExceptionUtil;
import dev.qixils.crowdcontrol.socket.SocketManager;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.math.BigInteger;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Manager for Crowd Control-related clients or servers.
 * It holds variables that may be used for a {@link SocketManager}.
 * @since 3.3.0
 */
@ApiStatus.AvailableSince("3.3.0")
public interface ServiceManager {
	/**
	 * Encrypts a password using the specified algorithm.
	 *
	 * @param password  password to encrypt
	 * @param algorithm algorithm to use
	 * @return encrypted password
	 * @throws NoSuchAlgorithmException if the specified algorithm is not found
	 * @since 3.3.0
	 */
	@ApiStatus.AvailableSince("3.3.0")
	@ApiStatus.Internal
	@NotNull
	@CheckReturnValue
	static String encryptPassword(@NotNull String password, @NotNull String algorithm) throws NoSuchAlgorithmException {
		ExceptionUtil.validateNotNull(password, "password");
		ExceptionUtil.validateNotNull(algorithm, "algorithm");
		MessageDigest md = MessageDigest.getInstance(algorithm);
		byte[] digest = md.digest(password.getBytes(StandardCharsets.UTF_8));
		return new BigInteger(1, digest).toString(16);
	}

	/**
	 * Encrypts a password using the SHA-512 algorithm.
	 * This is the default algorithm used by the receiver library.
	 *
	 * @param password password to encrypt
	 * @return hexadecimal representation of the encrypted password
	 * @since 3.3.0
	 */
	@ApiStatus.AvailableSince("3.3.0")
	@ApiStatus.Internal
	@NotNull
	@CheckReturnValue
	static String encryptPassword(@NotNull String password) {
		try {
			return encryptPassword(password, "SHA-512");
		} catch (NoSuchAlgorithmException e) {
			// this should never happen since SHA-512 is a valid algorithm
			// but alas, we must catch the exception and chuck it into unsuspecting code
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns the IP that the {@link SocketManager} will listen on or bind to.
	 * May be null for servers to bind to all local IPs.
	 *
	 * @return IP if available
	 * @since 3.3.0
	 */
	@ApiStatus.AvailableSince("3.3.0")
	@Nullable
	@CheckReturnValue
	InetAddress getIP();

	/**
	 * Returns the port that the {@link SocketManager} will listen on.
	 *
	 * @return IP port
	 * @since 3.3.0
	 */
	@ApiStatus.AvailableSince("3.3.0")
	@CheckReturnValue
	int getPort();

	/**
	 * Returns the password required for clients to connect to this server as a SHA-512 encrypted,
	 * Base64-encoded string. If running in client mode, this will be null.
	 *
	 * @return password required to connect
	 * @since 3.3.0
	 */
	@ApiStatus.AvailableSince("3.3.0")
	@Nullable
	@CheckReturnValue
	String getPassword();
}
