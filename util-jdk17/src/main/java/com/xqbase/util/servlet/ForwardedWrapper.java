package com.xqbase.util.servlet;

import java.security.GeneralSecurityException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashSet;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import com.xqbase.util.Base64;
import com.xqbase.util.ByteArrayQueue;

public class ForwardedWrapper implements WrapperFactory {
	private static final X509Certificate[] EMPTY_X509CERTS = {};

	private HashSet<String> trustedIPs = new HashSet<>();

	public ForwardedWrapper(ServletContext sc) {
		for (String ip : sc.getInitParameter(ForwardedWrapper.
				class.getName() + ".trustedIPs").split(",")) {
			trustedIPs.add(ip);
		}
	}

	@Override
	public HttpServletRequestWrapper getWrapper(HttpServletRequest req,
			HttpServletResponse resp) {
		if (!trustedIPs.contains(req.getRemoteAddr())) {
			return null;
		}

		String forwardedFor = req.getHeader("X-Forwarded-For");
		if (forwardedFor == null) {
			forwardedFor = req.getRemoteAddr();
		}

		String proto = req.getHeader("X-Forwarded-Proto");
		if (proto == null) {
			proto = req.getScheme();
		}

		String sslSessionId = req.getHeader("X-Forwarded-SSL-Session-ID");
		if (sslSessionId != null) {
			req.setAttribute("javax.servlet.request.ssl_session_id", sslSessionId);
		}

		String sslCipher = req.getHeader("X-Forwarded-SSL-Cipher");
		if (sslCipher != null) {
			req.setAttribute("javax.servlet.request.cipher_suite", sslCipher);
		}

		String certificates = req.getHeader("X-Forwarded-Certificates");
		if (certificates != null) {
			ByteArrayQueue baq = new ByteArrayQueue();
			baq.add(Base64.decode(certificates));
			try {
				Collection<? extends Certificate> certs = CertificateFactory.
						getInstance("X509").generateCertificates(baq.getInputStream());
				req.setAttribute("javax.servlet.request.X509Certificate",
						certs.toArray(EMPTY_X509CERTS));
			} catch (GeneralSecurityException e) {/**/}
		}

		final String scheme = proto;
		final String remoteAddr = forwardedFor;
		return new HttpServletRequestWrapper(req) {
			@Override
			public String getScheme() {
				return scheme;
			}

			@Override
			public boolean isSecure() {
				return super.isSecure() || "https".equalsIgnoreCase(scheme);
			}

			@Override
			public String getRemoteAddr() {
				return remoteAddr;
			}

			@Override
			public String getRemoteHost() {
				return remoteAddr;
			}
		};
	}
}