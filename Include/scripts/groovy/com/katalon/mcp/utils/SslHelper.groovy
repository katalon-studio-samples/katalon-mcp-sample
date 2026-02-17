/*
 * Copyright 2026 Katalon Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
