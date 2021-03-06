/*******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.cloudifysource.dsl.utils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;

import com.googlecode.ipv6.IPv6Address;

/**
 * A utility class for IP manipulation and validation.
 * 
 * @author noak
 * @since 2.1.0
 */
public final class IPUtils {

	// hidden constructor
	private IPUtils() {
	}

	// timeout in seconds, waiting for a socket to connect
	private static final int DEFAULT_CONNECTION_TIMEOUT = 10;
	private static final int MILLISECONDS_IN_A_SECOND = 1000;
	private static final int IP_BYTE_RANGE = 256;
	private static final int IPV4_PARTS = 4;

	private static final String NETWORK_INTERFACE_SEPARATOR = "%";

	//security constants
	private static final String SPRING_ACTIVE_PROFILE_ENV_VAR = "SPRING_PROFILES_ACTIVE";
	private static final String SPRING_PROFILE_NON_SECURE = "nonsecure";

	private static final String SPRING_SECURITY_PROFILE = System.getenv(SPRING_ACTIVE_PROFILE_ENV_VAR);

	/**
	 * Converts a standard IP address to a long-format IP address.
	 * 
	 * @param ipAddress
	 *            A standard IP address
	 * @return IP address as a long value
	 * @throws IllegalArgumentException
	 *             Indicates the given IP is invalid
	 */
	public static long ip2Long(final String ipAddress) throws IllegalArgumentException {
		if (!validateIPAddress(ipAddress)) {
			throw new IllegalArgumentException("Invalid IP address: " + ipAddress);
		}

		final byte[] ipBytes = getIPv4BytesArray(ipAddress);
		return ((long) (byte2int(ipBytes[0]) * IP_BYTE_RANGE + byte2int(ipBytes[1])) 
				* IP_BYTE_RANGE + byte2int(ipBytes[2]))
				* IP_BYTE_RANGE + byte2int(ipBytes[3]);
	}

	/**
	 * Converts a long value representing an IP address to a standard IP address
	 * (dotted decimal format).
	 * 
	 * @param ip
	 *            long value representing an IP address
	 * @return A standard IP address
	 */
	public static String long2String(final long ip) {
		final long a = (ip & 0xff000000) >> 24;
		final long b = (ip & 0x00ff0000) >> 16;
		final long c = (ip & 0x0000ff00) >> 8;
		final long d = ip & 0xff;

		return a + "." + b + "." + c + "." + d;
	}

	/**
	 * Converts a standard IP address to a byte array.
	 * 
	 * @param ipAddress
	 *            IP address as a standard IP address (dotted decimal format)
	 * @return IP as a 4-element byte array
	 * @throws IllegalArgumentException
	 *             Indicates the given IP is invalid
	 */
	public static byte[] getIPv4BytesArray(final String ipAddress) throws IllegalArgumentException {
		// This implementation is commented out because it involves resolving
		// the host, which we want to avoid.
		// return InetAddress.getByName(ipAddress).getAddress();
		final byte[] addrArr = new byte[IPV4_PARTS];
		final String[] ipParts = ipAddress.split("\\.");
		if (ipParts.length == IPV4_PARTS) {
			for (int i = 0; i < IPV4_PARTS; i++) {
				addrArr[i] = (byte) Integer.parseInt(ipParts[i]);
			}
		} else {
			throw new IllegalArgumentException("Invalid IP address: " + ipAddress);
		}

		return addrArr;
	}

	/**
	 * Converts (unsigned) byte to int.
	 * 
	 * @param b
	 *            byte to convert
	 * @return int value representing the given byte
	 */
	public static int byte2int(final byte b) {
		int i = b;
		if (b < 0) {
			i = b & 0x7f + 128;
		}

		return i;
	}

	/**
	 * Converts a CIDR IP format to an IP range format (e.g. 192.168.9.60/31
	 * becomes 192.168.9.60 - 192.168.9.61)
	 * 
	 * @param ipCidr
	 *            IP addresses formatted as CIDR
	 * @return IP addresses formatted as a simple range
	 * @throws UnknownHostException
	 *             Indicates the given IP cannot be resolved
	 * @throws IllegalArgumentException
	 *             Indicates the given IP is invalid
	 */
	public static String ipCIDR2Range(final String ipCidr) throws UnknownHostException, IllegalArgumentException {

		final String[] parts = ipCidr.split("/");
		final String ipAddress = parts[0];
		int maskBits;
		if (parts.length < 2) {
			maskBits = 0;
		} else {
			maskBits = Integer.parseInt(parts[1]);
		}

		if (!validateIPAddress(ipAddress)) {
			throw new IllegalArgumentException("Invalid IP address: " + ipAddress);
		}

		// Convert IPs into ints (32 bits).
		// E.g. 157.166.224.26 becomes 10011101 10100110 11100000 00011010
		// a simple split by dots (.), escaped.
		final String[] ipParts = ipAddress.split("\\.");
		final int addr = Integer.parseInt(ipParts[0]) << 24 & 0xFF000000 | Integer.parseInt(ipParts[1]) << 16
				& 0xFF0000 | Integer.parseInt(ipParts[2]) << 8 & 0xFF00 | Integer.parseInt(ipParts[3]) & 0xFF;

		// Get CIDR mask
		final int mask = 0xffffffff << 32 - maskBits;

		// Find lowest IP address
		final int lowest = addr & mask;
		final String lowestIP = buildIPV4String(toIntArray(lowest));

		// Find highest IP address
		final int highest = lowest + ~mask;
		final String highestIP = buildIPV4String(toIntArray(highest));

		return lowestIP + "-" + highestIP;
	}

	/**
	 * Convert a packed integer IPv4 address into a 4-element array.
	 * 
	 * @param ip
	 *            IPv4 address as an int
	 * @return IP as a 4-element array
	 */
	public static int[] toIntArray(final int ip) {
		final int[] ret = new int[IPV4_PARTS];
		for (int j = 3; j >= 0; --j) {
			ret[j] |= ip >>> 8 * (3 - j) & 0xff;
		}
		return ret;
	}

	/**
	 * Converts a 4-element array into a standard IP address (dotted decimal
	 * format).
	 * 
	 * @param ipBytes
	 *            as array of IP bytes
	 * @return A standard IP address
	 */
	public static String buildIPV4String(final int[] ipBytes) {
		final StringBuilder str = new StringBuilder();
		for (int i = 0; i < ipBytes.length; ++i) {
			str.append(ipBytes[i]);
			if (i != ipBytes.length - 1) {
				str.append(".");
			}
		}
		return str.toString();
	}

	/**
	 * Gets the next IP address as a standard IP address (dotted decimal
	 * format).
	 * 
	 * @param ipAddress
	 *            IP address (dotted decimal format)
	 * @return The following IP address
	 * @throws IllegalArgumentException
	 *             Indicates the given IP is invalid
	 */
	public static String getNextIP(final String ipAddress) throws IllegalArgumentException {
		return long2String(ip2Long(ipAddress) + 1);
	}

	/**
	 * Validates a standard IP address (dotted decimal format).
	 * 
	 * @param ipAddress
	 *            IP address to validate (in a dotted decimal format)
	 * @return true if valid, false if invalid
	 */
	public static boolean validateIPAddress(final String ipAddress) {
		boolean valid = false;

		if (isIPv6Address(ipAddress)) {
			// if we're here - this is valid IPv6 address
			valid = true;
		} else {
			// a simple split by dots (.), escaped.
			final String[] ipParts = ipAddress.split("\\.");
			if (ipParts.length == IPV4_PARTS) {
				for (final String part : ipParts) {
					final int intValue = Integer.parseInt(part);
					if (intValue < 0 || intValue > IP_BYTE_RANGE - 1) {
						valid = false;
						break;
					}
					valid = true;
				}
			}
		}
		
		return valid;
	}

	/**
	 * Validates a connection can be made to the given address and port, within
	 * the given time limit.
	 * 
	 * @param ipAddress
	 *            The IP address to connect to
	 * @param port
	 *            The port number to use
	 * @throws IOException
	 *             Reports a failure to connect or resolve the given address.
	 */
	public static void validateConnection(final String ipAddress, final int port) throws IOException {
		validateConnection(ipAddress, port, DEFAULT_CONNECTION_TIMEOUT);
	}

	/**
	 * Validates a connection can be made to the given address and port, within
	 * the given time limit.
	 * 
	 * @param ipAddress
	 *            The IP address to connect to
	 * @param port
	 *            The port number to use
	 * @param timeout
	 *            The time to wait before timing out, in seconds
	 * @throws IOException
	 *             Reports a failure to connect or resolve the given address.
	 */
	public static void validateConnection(final String ipAddress, final int port, final int timeout)
			throws IOException {

		final Socket socket = new Socket();

		try {
			final InetSocketAddress endPoint = new InetSocketAddress(ipAddress, port);
			if (endPoint.isUnresolved()) {
				throw new UnknownHostException(ipAddress);
			}

			socket.connect(endPoint, safeLongToInt(TimeUnit.SECONDS.toMillis(timeout), true));
		} finally {
			try {
				socket.close();
			} catch (final IOException ioe) {
				// ignore
			}
		}
	}
	
	
	/**
	 * Safely casts long to int.
	 * 
	 * @param longValue The long to cast
	 * @param roundIfNeeded Indicating whether to change the value of the number if it exceeds int's max/min values. If
	 *        set to false and the long is too large/small, an {@link IllegalArgumentException} is thrown.
	 * @return int representing of the given long.
	 */
	public static int safeLongToInt(final long longValue, final boolean roundIfNeeded) {
		int intValue;
		if (longValue < Integer.MIN_VALUE || longValue > Integer.MAX_VALUE) {
			if (roundIfNeeded) {
				if (longValue < Integer.MIN_VALUE) {
					intValue = Integer.MIN_VALUE;
				} else {
					intValue = Integer.MAX_VALUE;
				}
			} else {
				throw new IllegalArgumentException(longValue + " cannot be cast to int without changing its value.");
			}
		} else {
			intValue = (int) longValue;
		}
		return intValue;
	}
	

	/**
	 * Resolves IP address to host name.
	 * @param ip The IP to resolve
	 * @return host name
	 * @throws UnknownHostException Indicates the IP is not a known host
	 */
	public static String resolveIpToHostName(final String ip) throws UnknownHostException {

		String hostName = "";
    	try {
    		InetAddress addr = InetAddress.getByName(ip);
    		hostName = addr.getHostName();
    	} catch (UnknownHostException e) {
    		throw new IllegalStateException("could not resolve host name of ip " + ip);
    	}
    	
    	return hostName;
	}

	/**
	 * Resolves the host name and returns its IP address.
	 * 
	 * @param hostName
	 *            The name of the host
	 * @return The IP address of the host
	 * @throws UnknownHostException
	 *             Indicates the host doesn't represent an available network
	 *             object
	 */
	public static String resolveHostNameToIp(final String hostName) throws UnknownHostException {
		final InetAddress byName = InetAddress.getByName(hostName);
		try {
			if (byName.isReachable(DEFAULT_CONNECTION_TIMEOUT * MILLISECONDS_IN_A_SECOND)) {
				return byName.getHostAddress();
			} else {
				return null;
			}
		} catch (final IOException e) {
			return null; // not reachable
		}
	}

	/**
	 * Removes the interface part of the given IP address, if found. Examples:
	 * [fe80::9da2:25f7:86ce:cddb%45] will be returned as [fe80::9da2:25f7:86ce:cddb]
	 * fe80::9da2:25f7:86ce:cddb%45 will be returned as fe80::9da2:25f7:86ce:cddb
	 * [fe80::9da2:25f7:86ce:cddb] will be returned as [fe80::9da2:25f7:86ce:cddb]
	 * fe80::9da2:25f7:86ce:cddb will be returned as fe80::9da2:25f7:86ce:cddb
	 * 
	 * @param ipAddress
	 *            The ipAddress to parse
	 * @param surroundWithBrackets
	 *            if True - The returned IP address will be surrounded by "[]".
	 *            False - the returned IP address will not be surrounded by
	 *            "[]".
	 * @return the given ipAddress, with the interface part, if found.
	 * @throws IllegalArgumentException
	 *             Indicated the given ipAddress is invalid
	 */
	/*public static String removeInterfaceFromIpAddress(final String ipAddress, final boolean surroundWithBrackets)
			throws IllegalArgumentException {

		String cleanIp = "";

		cleanIp = StringUtils.substringBefore(StringUtils.strip(ipAddress, "[]"), NETWORK_INTERFACE_SEPARATOR);

		if (surroundWithBrackets) {
			cleanIp = bracketIfNeeded(cleanIp);
		}

		return cleanIp;
	}*/

	/**
	 * Compares two IPv6 addresses. The addresses can be in the full format,
	 * short format or iPv4MappedIPv6Address.
	 * If an IPv6 address is mis-formatted, False is returned.
	 * 
	 * @param address1
	 *            The first IPv6 address.
	 * @param address2
	 *            The second IPv6 address.
	 * @return True if the two addresses represent the same address, False
	 *         otherwise.
	 */
	private static boolean isSameIpv6Address(final String address1, final String address2) {

		boolean adressesEquals = false;
		
		try {
			final IPv6Address iPv6Address1 = IPv6Address.fromString(address1);
			final IPv6Address iPv6Address2 = IPv6Address.fromString(address2);
			adressesEquals = iPv6Address1.equals(iPv6Address2);
		} catch (IllegalArgumentException e) {
			//no need to throw the exception, just return false;
		}

		return adressesEquals;
	}
	
	/**
	 * Compares two IP addresses. The addresses can be in the full IPv6 format,
	 * short format or iPv4MappedIPv6Address.
	 * If the IP address is empty or mis-formatted, False is returned.
	 * 
	 * @param address1
	 *            The first IP address.
	 * @param address2
	 *            The second IP address.
	 * @return True if the two addresses represent the same address, False otherwise.
	 */
	public static boolean isSameIpAddress(final String address1, final String address2) {
		
		boolean isSameAddress = false;
		if (StringUtils.isBlank(address1) || StringUtils.isBlank(address2)) {
			//remain false;
		} else {
			if (isIPv6Address(address1) && isIPv6Address(address2)) {
				isSameAddress = isSameIpv6Address(address1, address2);
			} else {
				isSameAddress = address1.equalsIgnoreCase(address2);
			}			
		}
		
		return isSameAddress;
	}

	
	/**
	 * Chechs if the given address is an IPv6 address.
	 * @param ipAddress IP address
	 * @return True is the address represents and IPv6 address, False otherwise
	 */
	public static boolean isIPv6Address(final String ipAddress) {
		
		boolean isIPv6 = false;
		
		String strippedIp = StringUtils.strip(ipAddress, "[]");
		if (strippedIp.indexOf(NETWORK_INTERFACE_SEPARATOR) > -1) {
			strippedIp = StringUtils.substringBefore(strippedIp, NETWORK_INTERFACE_SEPARATOR);
		}
		
		try {
			IPv6Address.fromString(strippedIp);
			isIPv6 = true;
		} catch (IllegalArgumentException e) {
			//this is not a valid IPv6 address
		}

		return isIPv6;
	}
	

	/**
	 * Returns a "safe" formatted IP address - IPv4 addresses are not changed,
	 * IPv6 addresses may change - if they include an "interface" section it is removed,
	 * and the address itself is surrounded by brackets ("[]") to allow for port concatenation. 
	 * @param ipAddress The ipAddress to handle
	 * @return an IP address, "safe" for concatenation.
	 */
	public static String getSafeIpAddress(final String ipAddress) {
		
		String safeIpAddress;
		try {
			String strippedIp = StringUtils.strip(ipAddress, "[]");
			strippedIp = StringUtils.substringBefore(strippedIp, NETWORK_INTERFACE_SEPARATOR);
			IPv6Address.fromString(strippedIp);		//verifies this is an IPv6 address
			safeIpAddress = "[" + strippedIp + "]";	
		} catch (IllegalArgumentException e) {
			//this is not a valid IPv6 address, assume this is IPv4 or host name, leave as is
			safeIpAddress = ipAddress;
		}
		
		return safeIpAddress;
	}
	
	
	/**
	 * Gets the relevant rest protocol, considering the SPRING_PROFILES_ACTIVE environment variable.
	 * @return https if the rest server is secured, http otherwise.
	 */
	public static String getRestProtocol() {
		
		String protocol = "https";
		if (StringUtils.isBlank(SPRING_SECURITY_PROFILE) 
				|| SPRING_SECURITY_PROFILE.contains(SPRING_PROFILE_NON_SECURE)) {
			//security is off
			protocol = "http";
		}
		
		return protocol;		
	}

}
