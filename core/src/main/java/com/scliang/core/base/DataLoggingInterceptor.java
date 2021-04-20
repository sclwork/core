package com.scliang.core.base;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;

import java.io.EOFException;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import okhttp3.Connection;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.http.HttpHeaders;
import okio.Buffer;
import okio.BufferedSource;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2017/12/21.
 */
public class DataLoggingInterceptor implements Interceptor {
    private static final String sFormatLine =
      "===========================================================================================";
    public static final String sLogStartFlag = "==scliang==log==start==";
    public static final String sLogEndFlag = "==scliang==log==end==";
    private static final Charset UTF8 = StandardCharsets.UTF_8;

    public interface Logger {
        void reset();
        void log(String message);
    }

    DataLoggingInterceptor(DataLoggingInterceptor.Logger logger) {
        this.logger = logger;
    }

    private final DataLoggingInterceptor.Logger logger;

    @NotNull
    @Override public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request();
        if (DataMemoryCacheInterceptor.getCacheResponse(request) != null) {
            // 缓存的请求不打印日志
            return chain.proceed(request);
        }

        logger.reset();
        logger.log(sLogStartFlag);
        logger.log(sFormatLine);

        RequestBody requestBody = request.body();
        boolean hasRequestBody = requestBody != null;

        Connection connection = chain.connection();
        Protocol protocol = connection != null ? connection.protocol() : Protocol.HTTP_1_1;
        String requestStartMessage = request.method() + " " + request.url() + " " + protocol;
        logger.log(requestStartMessage);

        if (hasRequestBody) {
            // Request body headers are only present when installed as a network interceptor. Force
            // them to be included (when available) so there values are known.
            if (requestBody.contentType() != null) {
                logger.log("Content-Type: " + requestBody.contentType());
            }
            if (requestBody.contentLength() != -1) {
                logger.log("Content-Length: " + requestBody.contentLength());
            }
        }

        Headers rHeaders = request.headers();
        for (int i = 0, count = rHeaders.size(); i < count; i++) {
            String name = rHeaders.name(i);
            // Skip headers from the request body as they are explicitly logged above.
            if (!"Content-Type".equalsIgnoreCase(name) && !"Content-Length".equalsIgnoreCase(name)) {
                logger.log(name + ": " + rHeaders.value(i));
            }
        }

        if (!hasRequestBody) {
            logger.log("END " + request.method());
        } else if (bodyEncoded(request.headers())) {
            logger.log("END " + request.method() + " (encoded body omitted)");
        } else {
            Buffer buffer = new Buffer();
            requestBody.writeTo(buffer);

            Charset charset = UTF8;
            MediaType contentType = requestBody.contentType();
            if (contentType != null) {
                charset = contentType.charset(UTF8);
            }

            if (charset != null) {
                logger.log(sFormatLine);
                if (isPlaintext(buffer)) {
                    try {
                        String requestStr = URLDecoder.decode(buffer.readString(charset), "UTF-8");
                        String[] strs = requestStr.split("&");
                        for (String str : strs) {
                            logger.log(str);
                        }
                    } catch (Exception e) {
                        logger.log(buffer.readString(charset));
                    }
                    logger.log("END " + request.method()
                      + " (" + requestBody.contentLength() + "-byte body)");
                } else {
                    logger.log("END " + request.method() + " (binary "
                      + requestBody.contentLength() + "-byte body omitted)");
                }
            }
        }

        logger.log(sFormatLine);

        long startNs = System.nanoTime();
        Response response;
        try {
            response = chain.proceed(request);
        } catch (Exception e) {
            logger.log("HTTP FAILED: " + e);
            logger.log(sLogEndFlag);
            throw e;
        }

        long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);
        ResponseBody responseBody = response.body();
        if (responseBody != null) {
            long contentLength = responseBody.contentLength();
            String bodySize = contentLength != -1 ? contentLength + "-byte" : "unknown-length";
            logger.log(response.code() + " " + response.message() + " "
              + response.request().url() + " (" + tookMs + "ms)");

            Headers headers = response.headers();
            for (int i = 0, count = headers.size(); i < count; i++) {
                logger.log(headers.name(i) + ": " + headers.value(i));
            }

            if (!HttpHeaders.hasBody(response)) {
                logger.log("END HTTP");
            } else if (bodyEncoded(response.headers())) {
                logger.log("END HTTP (encoded body omitted)");
            } else {
                BufferedSource source = responseBody.source();
                if (source.isOpen()) {
                    source.request(Long.MAX_VALUE); // Buffer the entire body.
                    Buffer buffer = source.buffer();

                    Charset charset = UTF8;
                    MediaType contentType = responseBody.contentType();
                    if (contentType != null) {
                        charset = contentType.charset(UTF8);
                    }

                    if (!isPlaintext(buffer)) {
                        logger.log("END HTTP (binary " + buffer.size() + "-byte body omitted)");
                        logger.log(sFormatLine);
                        logger.log(sLogEndFlag);
                        return response;
                    }

                    if (contentLength != 0 && charset != null) {
                        logger.log(sFormatLine);
                        logger.log(buffer.clone().readString(charset));
                    }

                    logger.log("END HTTP (" + buffer.size() + "-byte body)");
                } else {
                    logger.log("END HTTP");
                }
            }
        }

        logger.log(sFormatLine);
        logger.log(sLogEndFlag);

        return response;
    }

    /**
     * Returns true if the body in question probably contains human readable text. Uses a small sample
     * of code points to detect unicode control characters commonly used in binary file signatures.
     */
    private static boolean isPlaintext(Buffer buffer) {
        try {
            Buffer prefix = new Buffer();
            long byteCount = buffer.size() < 64 ? buffer.size() : 64;
            buffer.copyTo(prefix, 0, byteCount);
            for (int i = 0; i < 16; i++) {
                if (prefix.exhausted()) {
                    break;
                }
                int codePoint = prefix.readUtf8CodePoint();
                if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
                    return false;
                }
            }
            return true;
        } catch (EOFException e) {
            return false; // Truncated UTF-8 sequence.
        }
    }

    private boolean bodyEncoded(Headers headers) {
        String contentEncoding = headers.get("Content-Encoding");
        return contentEncoding != null && !contentEncoding.equalsIgnoreCase("identity");
    }
}
