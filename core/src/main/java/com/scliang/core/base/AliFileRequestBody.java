package com.scliang.core.base;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;
import okio.Sink;

public class AliFileRequestBody extends RequestBody {
    private final AliFileRequestCallback mCallback;
    private final RequestBody mBody;
    private BufferedSink mSink;

    public AliFileRequestBody(@NotNull RequestBody body, AliFileRequestCallback callback) {
        mBody = body;
        mCallback = callback;
    }

    @Nullable
    @Override
    public MediaType contentType() {
        return mBody.contentType();
    }

    @Override
    public long contentLength() throws IOException {
        return mBody.contentLength();
    }

    @Override
    public void writeTo(@NotNull BufferedSink sink) throws IOException {
        if (mSink == null) {
            mSink = Okio.buffer(sink(sink));
        }
        mBody.writeTo(mSink);
        mSink.flush();
    }

    private Sink sink(Sink sink) {
        return new ForwardingSink(sink) {
            long bytesWritten = 0L;
            long contentLength = 0L;
            @Override
            public void write(@NotNull Buffer source, long byteCount) throws IOException {
                super.write(source, byteCount);
                if (contentLength == 0) {
                    contentLength = contentLength();
                }
                bytesWritten += byteCount;
                if (mCallback != null) mCallback.onProgress(contentLength, bytesWritten);
            }
        };
    }
}
