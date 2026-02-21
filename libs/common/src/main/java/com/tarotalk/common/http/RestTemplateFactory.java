package com.tarotalk.common.http;

import com.tarotalk.common.web.TraceContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class RestTemplateFactory {
    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 3000;
    private static final int DEFAULT_READ_TIMEOUT_MS = 5000;
    private static final int DEFAULT_MAX_ATTEMPTS = 3;
    private static final long DEFAULT_RETRY_BACKOFF_MS = 150L;

    private RestTemplateFactory() {
    }

    public static RestTemplate create() {
        return create(
                DEFAULT_CONNECT_TIMEOUT_MS,
                DEFAULT_READ_TIMEOUT_MS,
                DEFAULT_MAX_ATTEMPTS,
                DEFAULT_RETRY_BACKOFF_MS
        );
    }

    public static RestTemplate create(int connectTimeoutMs, int readTimeoutMs, int maxAttempts) {
        return create(connectTimeoutMs, readTimeoutMs, maxAttempts, DEFAULT_RETRY_BACKOFF_MS);
    }

    public static RestTemplate create(int connectTimeoutMs,
                                      int readTimeoutMs,
                                      int maxAttempts,
                                      long retryBackoffMs) {
        RestTemplate restTemplate = new RestTemplate(requestFactory(connectTimeoutMs, readTimeoutMs));
        List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
        interceptors.add(new TraceHeaderInterceptor());
        interceptors.add(new RetryInterceptor(Math.max(1, maxAttempts), Math.max(0L, retryBackoffMs)));
        restTemplate.setInterceptors(interceptors);
        return restTemplate;
    }

    private static ClientHttpRequestFactory requestFactory(int connectTimeoutMs, int readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        return factory;
    }

    private static class TraceHeaderInterceptor implements ClientHttpRequestInterceptor {
        @Override
        public ClientHttpResponse intercept(org.springframework.http.HttpRequest request,
                                            byte[] body,
                                            ClientHttpRequestExecution execution) throws IOException {
            HttpHeaders headers = request.getHeaders();
            if (!headers.containsKey("X-Trace-Id")) {
                headers.set("X-Trace-Id", TraceContext.currentTraceIdOrRandom());
            }
            String userId = TraceContext.currentUserId();
            if (userId != null && !headers.containsKey("X-User-Id")) {
                headers.set("X-User-Id", userId);
            }
            return execution.execute(request, body);
        }
    }

    private static class RetryInterceptor implements ClientHttpRequestInterceptor {
        private final int maxAttempts;
        private final long retryBackoffMs;

        private RetryInterceptor(int maxAttempts, long retryBackoffMs) {
            this.maxAttempts = maxAttempts;
            this.retryBackoffMs = retryBackoffMs;
        }

        @Override
        public ClientHttpResponse intercept(org.springframework.http.HttpRequest request,
                                            byte[] body,
                                            ClientHttpRequestExecution execution) throws IOException {
            HttpMethod method = request.getMethod();
            if (method == null || !isRetryableMethod(method)) {
                return execution.execute(request, body);
            }

            IOException lastIo = null;
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                try {
                    ClientHttpResponse response = execution.execute(request, body);
                    if (shouldRetryStatus(response) && attempt < maxAttempts) {
                        response.close();
                        sleepQuietly(attempt);
                        continue;
                    }
                    return response;
                } catch (ResourceAccessException ex) {
                    if (ex.getCause() instanceof IOException) {
                        lastIo = (IOException) ex.getCause();
                    } else {
                        lastIo = new IOException(ex.getMessage(), ex);
                    }
                } catch (IOException ex) {
                    lastIo = ex;
                }
                if (attempt < maxAttempts) {
                    sleepQuietly(attempt);
                }
            }

            if (lastIo != null) {
                throw lastIo;
            }
            throw new IOException("request failed after retries");
        }

        private boolean shouldRetryStatus(ClientHttpResponse response) {
            try {
                int rawStatus = response.getRawStatusCode();
                return rawStatus == 429 || rawStatus >= 500;
            } catch (IOException ex) {
                return false;
            }
        }

        private void sleepQuietly(int attempt) {
            if (retryBackoffMs <= 0L) {
                return;
            }
            long delay = retryBackoffMs * Math.max(1, attempt);
            try {
                Thread.sleep(delay);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }

        private boolean isRetryableMethod(HttpMethod method) {
            return HttpMethod.GET.equals(method)
                    || HttpMethod.HEAD.equals(method)
                    || HttpMethod.OPTIONS.equals(method);
        }
    }
}
