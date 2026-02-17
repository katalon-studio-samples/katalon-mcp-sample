/**
 * SSL Certificate Test
 * 
 * Tests SSL certificate validation behavior with and without SslHelper.
 * This test suite verifies:
 * - Default HttpClient correctly rejects invalid certificates
 * - SslHelper.createTrustAllClientBuilder() bypasses certificate validation
 * 
 * Uses badssl.com test endpoints to verify:
 * - Valid certificates (should pass)
 * - Self-signed certificates (should fail by default, pass with trust-all)
 * - Expired certificates (should fail by default, pass with trust-all)
 * - Wrong host certificates (should fail by default, pass with trust-all)
 * - Untrusted root certificates (should fail by default, pass with trust-all)
 * 
 * Test Endpoints: badssl.com
 * Purpose: Validate SslHelper utility for transparent proxy environments
 */

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

import javax.net.ssl.SSLHandshakeException

import com.katalon.mcp.utils.SslHelper

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import org.slf4j.LoggerFactory

// Suppress Katalon's line-by-line Groovy statement logging
((Logger) LoggerFactory.getLogger("testcase")).setLevel(Level.WARN)

// --- badssl.com test endpoints ---
def VALID_URL       = "https://badssl.com/"
def SELF_SIGNED_URL = "https://self-signed.badssl.com/"
def EXPIRED_URL     = "https://expired.badssl.com/"
def WRONG_HOST_URL  = "https://wrong.host.badssl.com/"
def UNTRUSTED_URL   = "https://untrusted-root.badssl.com/"

int passed = 0
int failed = 0

println "=========================================="
println "SSL Certificate Test (badssl.com)"
println "=========================================="

// --- Helpers ---

def httpClientGet(String url, HttpClient client) {
    def req = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(10))
            .GET()
            .build()
    return client.send(req, HttpResponse.BodyHandlers.ofString())
}

def isSslError(Exception e) {
    def msg = (e.message ?: "") + (e.cause?.message ?: "")
    def lower = msg.toLowerCase()
    return lower.contains("ssl") || lower.contains("certificate") ||
           lower.contains("pkix") || lower.contains("trust") ||
           lower.contains("handshake") || e instanceof SSLHandshakeException ||
           e.cause instanceof SSLHandshakeException
}

def runTest(String name, boolean expectFailure, Closure test) {
    print "  ${name} ... "
    try {
        test()
        if (expectFailure) {
            println "UNEXPECTED PASS (expected failure)"
            return false
        }
        println "PASS"
        return true
    } catch (Exception e) {
        if (expectFailure && isSslError(e)) {
            println "PASS (SSL rejected: ${e.cause?.class?.simpleName ?: e.class.simpleName})"
            return true
        }
        if (expectFailure) {
            println "PASS (failed: ${e.class.simpleName} - ${e.message?.take(80)})"
            return true
        }
        println "FAIL - ${e.class.simpleName}: ${e.message?.take(100)}"
        return false
    }
}

// =========================================================================
// 1. Default SSL — bad certs rejected
// =========================================================================
println "\n  1. Default HttpClient (no SslHelper):"
def defaultClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

if (runTest("valid cert", false) { httpClientGet(VALID_URL, defaultClient) }) passed++ else failed++
if (runTest("self-signed", true) { httpClientGet(SELF_SIGNED_URL, defaultClient) }) passed++ else failed++
if (runTest("expired", true) { httpClientGet(EXPIRED_URL, defaultClient) }) passed++ else failed++
if (runTest("wrong host", true) { httpClientGet(WRONG_HOST_URL, defaultClient) }) passed++ else failed++
if (runTest("untrusted root", true) { httpClientGet(UNTRUSTED_URL, defaultClient) }) passed++ else failed++

// =========================================================================
// 2. SslHelper.createTrustAllClientBuilder() — all certs accepted
// =========================================================================
println "\n  2. SslHelper.createTrustAllClientBuilder():"
def trustAllClient = SslHelper.createTrustAllClientBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

if (runTest("valid cert", false) { httpClientGet(VALID_URL, trustAllClient) }) passed++ else failed++
if (runTest("self-signed", false) { httpClientGet(SELF_SIGNED_URL, trustAllClient) }) passed++ else failed++
if (runTest("expired", false) { httpClientGet(EXPIRED_URL, trustAllClient) }) passed++ else failed++
if (runTest("wrong host", false) { httpClientGet(WRONG_HOST_URL, trustAllClient) }) passed++ else failed++
if (runTest("untrusted root", false) { httpClientGet(UNTRUSTED_URL, trustAllClient) }) passed++ else failed++

// =========================================================================
// Summary
// =========================================================================
println "\n=========================================="
println "Results: ${passed} passed, ${failed} failed out of ${passed + failed} tests"
println ""
if (failed > 0) {
    println "Some tests did not behave as expected."
} else {
    println "All tests passed."
    println "  1. Default SSL correctly rejects bad certs"
    println "  2. SslHelper.createTrustAllClientBuilder() bypasses all cert checks"
}
println "=========================================="
