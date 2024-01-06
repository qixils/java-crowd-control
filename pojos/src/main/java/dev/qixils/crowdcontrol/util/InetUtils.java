package dev.qixils.crowdcontrol.util;

import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Utilities for {@link InetAddress}.
 */
public class InetUtils {

	/**
	 * Safely gets the {@code localhost} address.
	 *
	 * @return localhost
	 */
	@NotNull
	public static InetAddress getLocalHost() {
		try {
			return InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			return InetAddress.getLoopbackAddress();
		}
	}

	/**
	 * Wrapper for {@link InetAddress#getByName(String)} which throws an unchecked exception.
	 *
	 * @param ip IP address
	 * @return inet address
	 */
	@NotNull
	public static InetAddress getByName(@NotNull String ip) throws IllegalArgumentException {
		try {
			return InetAddress.getByName(ip);
		} catch (UnknownHostException e) {
			throw new IllegalArgumentException("Invalid IP address " + ip, e);
		}
	}
}
