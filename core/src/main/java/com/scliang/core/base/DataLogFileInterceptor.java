package com.scliang.core.base;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;

import java.io.EOFException;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
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
import okio.Buffer;
import okio.BufferedSource;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2018/8/27.
 */
public class DataLogFileInterceptor implements Interceptor {
    private static final Charset UTF8 = StandardCharsets.UTF_8;
    private SimpleDateFormat mSDF;

    public interface Logger {
        void log(String message);
    }

    DataLogFileInterceptor(DataLogFileInterceptor.Logger logger) {
        this.logger = logger;
        mSDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.CHINESE);
    }

    private final DataLogFileInterceptor.Logger logger;

    @NotNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request();
        {
            Response response = DataMemoryCacheInterceptor.getCacheResponse(request);
            if (response != null) {
                return chain.proceed(request);
            }
        }

        StringBuilder sb = new StringBuilder();

        sb.append("<section style='word-break: break-all;'>");
        sb.append("<br/><br/><br/><b>").append(mSDF.format(new Date())).append("</b><br/>");
        sb.append("=====================================").append("<br/>");

        RequestBody requestBody = request.body();
        boolean hasRequestBody = requestBody != null;

        Connection connection = chain.connection();
        Protocol protocol = connection != null ? connection.protocol() : Protocol.HTTP_1_1;
        String requestStartMessage = "<font color='#33b5e5'><b>" + request.method() + "</b></font> [" + protocol + "]";
        requestStartMessage += "<br/><font color='#33b5e5'><b>" + request.url() + "</b></font><br/>";
//        if (requestBody != null) {
//            requestStartMessage += requestBody.contentLength() + "-byte body";
//        }
        sb.append(requestStartMessage);

        if (hasRequestBody) {
            if (requestBody.contentType() != null) {
                sb.append("Content-Type: ").append(requestBody.contentType()).append(" ");
            }
            if (requestBody.contentLength() != -1) {
                sb.append("Content-Length: ").append(requestBody.contentLength());
            }
        }

        Headers headers = request.headers();
        for (int i = 0, count = headers.size(); i < count; i++) {
            String name = headers.name(i);
            if (!"Content-Type".equalsIgnoreCase(name) && !"Content-Length".equalsIgnoreCase(name)) {
                sb.append(name).append(": ").append(headers.value(i)).append(" ");
            }
        }

        sb.append("<br/>");

        Buffer buffer = new Buffer();
        if (requestBody != null) {
            requestBody.writeTo(buffer);
        }

        Charset charset = UTF8;
        if (requestBody != null) {
            MediaType contentType = requestBody.contentType();
            if (contentType != null) {
                charset = contentType.charset(UTF8);
            }
        }

        if (charset != null) {
            if (isPlaintext(buffer)) {
                sb.append("<font color='#33b5e5'><b>");
                try {
                    String requestStr = URLDecoder.decode(buffer.readString(charset), "UTF-8");
                    String[] strs = requestStr.split("&");
                    for (String str : strs) {
                        sb.append(str);
                    }
                } catch (Exception e) {
                    sb.append(buffer.readString(charset));
                }
//            if (requestBody != null) {
//                sb.append(request.method()).append(" (").append(requestBody.contentLength()).append("-byte body)");
//            }
                sb.append("</b></font>");
            } else {
                if (requestBody != null) {
                    sb.append(request.method()).append(" (binary ")
                      .append(requestBody.contentLength()).append("-byte body omitted)");
                }
            }
        }

        long startNs = System.nanoTime();
        Response response;
        try {
            response = chain.proceed(request);
        } catch (Exception e) {
            sb.append("<br/><font color='#ff4444'>HTTP FAILED: ").append(e).append("</font><br/>");
            throw e;
        }

        int code = response.code();
        long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);
        sb.append("<br/>");
        ResponseBody responseBody = response.body();
        long contentLength = 0;
        if (responseBody != null) {
            contentLength = responseBody.contentLength();
//            String bodySize = contentLength != -1 ? contentLength + "-byte" : "unknown-length";
            sb.append(code < 400 ? "<font color='#2b7c04'><b>" : "<font color='#ff4444'><b>")
                    .append(code)
                    .append(" ")
                    .append(response.message())
                    .append("</b></font>")
                    .append("<br/>")
                    .append(response.request().url())
                    .append(" ")
                    .append(tookMs)
                    .append("ms")
//                    .append(bodySize)
//                    .append(" body)")
                    .append("<br/>");
        }

        // response
        headers = response.headers();
        for (int i = 0, count = headers.size(); i < count; i++) {
            sb.append(headers.name(i)).append(": ").append(headers.value(i)).append(" ");
        }

        if (responseBody != null) {
            BufferedSource source = responseBody.source();
            if (source.isOpen()) {
                source.request(Long.MAX_VALUE); // Buffer the entire body.
                buffer = source.buffer();
            }
        }

        charset = UTF8;
        if (responseBody != null) {
            MediaType contentType = responseBody.contentType();
            if (contentType != null) {
                charset = contentType.charset(UTF8);
            }
        }

        if (!isPlaintext(buffer)) {
            sb.append("(binary ").append(buffer.size()).append("-byte body omitted)");
            return response;
        }

        if (contentLength != 0 && charset != null) {
            sb.append(" [").append(buffer.size()).append("-byte body]");
            sb.append("<br/>");
            sb.append(code < 400 ? "<font color='#2b7c04'><b>" : "<font color='#ff4444'><b>");
            sb.append(buffer.clone().readString(charset));
            sb.append("</b></font>");
        }

        sb.append("<br/><br/><br/>");
        sb.append("</section>");

        logger.log(sb.toString());

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
}
