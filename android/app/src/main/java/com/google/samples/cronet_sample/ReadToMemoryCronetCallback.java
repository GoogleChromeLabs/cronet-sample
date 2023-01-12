package com.google.samples.cronet_sample;

import android.net.http.HttpEngine;
import android.net.http.HttpException;
import android.net.http.UrlRequest;
import android.net.http.UrlResponseInfo;
import androidx.annotation.RequiresApi;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

@RequiresApi(api = 34)
abstract class ReadToMemoryCronetCallback extends UrlRequest.Callback {

    private static final String TAG = "ReadToMemoryCronetCallback";

    private static final int BYTE_BUFFER_CAPACITY_BYTES = 64 * 1024;

    private final ByteArrayOutputStream bytesReceived = new ByteArrayOutputStream();
    private final WritableByteChannel receiveChannel = Channels.newChannel(bytesReceived);
    private final long startTimeNanos;

    ReadToMemoryCronetCallback() {
        // This is not entirely accurate as the request doesn't start the moment the callback
        // is created, but the events are close enough for the purpose of the test application.
        startTimeNanos = System.nanoTime();
    }

    @Override
    public void onRedirectReceived(
            UrlRequest request, UrlResponseInfo info, String newLocationUrl) {
        // Invoked whenever a redirect is encountered. This will only be invoked between the call
        // to UrlRequest.start() and onResponseStarted(). The body of the redirect response, if it
        // has one, will be ignored. The redirect will not be followed until the URLRequest's
        // followRedirect method is called, either synchronously or asynchronously.

        android.util.Log.i(TAG, "****** onRedirectReceived ******");
        request.followRedirect();
    }

    @Override
    public void onResponseStarted(UrlRequest request, UrlResponseInfo info) {
        // Invoked when the final set of headers, after all redirects, is received. Will only be
        // invoked once for each request.
        //
        // With the exception of onCanceled(), no other {@link Callback} method will be invoked
        // for the request, including onSucceeded() and onFailed(), until read() is called
        // to attempt to start reading the response body.

        android.util.Log.i(TAG, "****** Response Started ******");
        android.util.Log.i(TAG, "*** Headers Are *** " + info.getAllHeaders());

        // One must use a *direct* byte buffer when calling the read method.
        request.read(ByteBuffer.allocateDirect(BYTE_BUFFER_CAPACITY_BYTES));
    }

    @Override
    public void onReadCompleted(
            UrlRequest request, UrlResponseInfo info, ByteBuffer byteBuffer) {
        // Invoked whenever part of the response body has been read. Only part of the buffer may be
        // populated, even if the entire response body has not yet been consumed.
        //
        // With the exception of onCanceled(), no other {@link Callback} method will be invoked
        // for the request, including onSucceeded() and onFailed(), until read() is called
        // to attempt to continue reading the response body.

        android.util.Log.i(TAG, "****** onReadCompleted ******" + byteBuffer);

        // The byte buffer we're getting in the callback hasn't been flipped for reading,
        // so flip it so we can read the content.
        byteBuffer.flip();

        try {
            receiveChannel.write(byteBuffer);
        } catch (IOException e) {
            android.util.Log.i(TAG, "IOException during ByteBuffer read. Details: ", e);
        }
        // Reset the buffer to prepare it for the next read
        byteBuffer.clear();

        // Continue reading the request
        request.read(byteBuffer);
    }

    @Override
    public void onSucceeded(UrlRequest request, UrlResponseInfo info) {
        // Invoked when request is completed successfully. Once invoked, no other Callback methods
        // will be invoked.

        long latencyNanos = System.nanoTime() - startTimeNanos;

        android.util.Log.i(TAG,
                "****** Cronet Request Completed, the latency is " + latencyNanos + " nanoseconds" +
                        ". " + getWasCachedMessage(info));

        android.util.Log.i(TAG,
                "****** Cronet Negotiated protocol:  " + info.getNegotiatedProtocol());

        android.util.Log.i(TAG,
                "****** Cronet Request Completed, status code is " + info.getHttpStatusCode()
                        + ", total received bytes is " + info.getReceivedByteCount());

        byte[] bodyBytes = bytesReceived.toByteArray();

        // We invoke the callback directly here for simplicity. Note that the executor running this
        // callback might be shared with other Cronet requests, or even with other parts of your
        // application. Always make sure to appropriately provision your pools, and consider
        // delegating time consuming work on another executor.
        onSucceeded(request, info, bodyBytes, latencyNanos);
    }

    private static String getWasCachedMessage(UrlResponseInfo responseInfo) {
        if (responseInfo.wasCached()) {
            return "The request was cached.";
        } else {
            return "";
        }
    }

    abstract void onSucceeded(
            UrlRequest request, UrlResponseInfo info, byte[] bodyBytes, long latencyNanos);

    @Override
    public void onFailed(UrlRequest var1, UrlResponseInfo var2, HttpException var3) {
        android.util.Log.i(TAG, "****** onFailed, error is: " + var3.getMessage());
    }
}
