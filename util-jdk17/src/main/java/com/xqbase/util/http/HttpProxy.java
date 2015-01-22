package com.xqbase.util.http;

import java.io.IOException;
import java.net.Socket;

import com.xqbase.util.Base64;
import com.xqbase.util.SocketPool;
import com.xqbase.util.function.SupplierEx;

public class HttpProxy {
	private String host, username, password;
	private int port;

	public HttpProxy(String host, int port) {
		this(host, port, null, null);
	}

	public HttpProxy(String host, int port, String username, String password) {
		this.host = host;
		this.port = port;
		this.username = username;
		this.password = password;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public String getProxyAuth() {
		return username == null ? null :
				"Basic " + Base64.encode((username + ":" +
				(password == null ? "" : password)).getBytes());
	}

	public Socket createSocket(String remoteHost,
			int remotePort, boolean secure, int timeout) throws IOException {
		return HttpUtil.connect(SocketPool.
				createSocket(getHost(), getPort(), false, timeout),
				remoteHost, remotePort, getProxyAuth(), secure);
	}

	public SocketPool createSocketPool(final String remoteHost,
			final int remotePort, final boolean secure, final int timeout) {
		return new SocketPool(new SupplierEx<Socket, IOException>() {
			@Override
			public Socket get() throws IOException {
				return createSocket(remoteHost, remotePort, secure, timeout);
			}
		}, timeout);
	}
}