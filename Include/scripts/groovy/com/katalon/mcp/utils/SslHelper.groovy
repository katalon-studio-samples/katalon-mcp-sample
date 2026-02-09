package com.katalon.mcp.utils

import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate
import java.net.http.HttpClient

class SslHelper {

	private static SSLContext trustAllSslContext

	static SSLContext getTrustAllSslContext() {
		if (trustAllSslContext == null) {
			def trustAllCerts = [
				new X509TrustManager() {
					X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0] }
					void checkClientTrusted(X509Certificate[] certs, String authType) {}
					void checkServerTrusted(X509Certificate[] certs, String authType) {}
				}
			] as TrustManager[]

			trustAllSslContext = SSLContext.getInstance("TLS")
			trustAllSslContext.init(null, trustAllCerts, new java.security.SecureRandom())
		}
		return trustAllSslContext
	}

	/**
	 * Creates an HttpClient.Builder that trusts all SSL certificates and
	 * skips hostname verification. Use with MCP SDK transports that accept
	 * a clientBuilder parameter.
	 */
	static HttpClient.Builder createTrustAllClientBuilder() {
		// java.net.http.HttpClient ignores SSLParameters for hostname verification.
		// This JDK-internal property is the only way to disable it.
		System.setProperty("jdk.internal.httpclient.disableHostnameVerification", "true")
		return HttpClient.newBuilder()
				.sslContext(getTrustAllSslContext())
	}
}
