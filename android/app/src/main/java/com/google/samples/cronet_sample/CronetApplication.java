package com.google.samples.cronet_sample;

import android.app.Application;
import android.content.Context;
import android.net.http.HttpEngine;

import androidx.annotation.RequiresApi;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class CronetApplication extends Application {

    // We recommend that each application uses a single, global CronetEngine. This allows Cronet
    // to maximize performance. This can either be achieved using a global static . In this example,
    // we initialize it in an Application class to manage lifecycle of the network log.
    private HttpEngine cronetEngine;

    // Executor that will invoke asynchronous Cronet callbacks. Like with the Cronet engine, we
    // recommend that it's managed centrally.
    private ExecutorService cronetCallbackExecutorService;

    // We use this variable to demonstrate how Cronet's caching behaves. Each subsequent attempt to
    // load the images fetches one more, up to the number of images specified in ImageRepository.
    // Don't do this in your production application, it's a dirty hack :).
    public final AtomicInteger imagesToLoadCeiling = new AtomicInteger();

    @Override
    public void onCreate() {
        super.onCreate();
        cronetEngine = createDefaultCronetEngine(this);
        cronetCallbackExecutorService = Executors.newFixedThreadPool(4);
    }

    public HttpEngine getCronetEngine() {
        return cronetEngine;
    }

    public ExecutorService getCronetCallbackExecutorService() {
        return cronetCallbackExecutorService;
    }

    @RequiresApi(api = 34)
    private static HttpEngine createDefaultCronetEngine(Context context) {
        // Cronet makes use of modern protocols like HTTP/2 and QUIC by default. However, to make
        // the most of servers that support QUIC, one must either specify that a particular domain
        // supports QUIC explicitly using QUIC hints, or enable the on-disk cache.
        //
        // When a QUIC hint is provided, Cronet will attempt to use QUIC from the very beginning
        // when communicating with the server and if that fails, we fall back to using HTTP. If
        // no hints are provided, Cronet uses HTTP for the first request issued to the server.
        // If the server indicates it does support QUIC, Cronet stores the information and will use
        // QUIC for subsequent request to that domain.
        //
        // We recommend that QUIC hints are provided explicitly when working with servers known
        // to support QUIC.
        return new HttpEngine.Builder(context)
                // The storage path must be set first when using a disk cache.
                .setStoragePath(context.getFilesDir().getAbsolutePath())

                // Enable on-disk cache, this enables automatic QUIC usage for subsequent requests
                // to the same domain across application restarts. If you also want to cache HTTP
                // responses, use HTTP_CACHE_DISK instead. Typically you will want to enable caching
                // in full, we turn it off for this demo to better demonstrate Cronet's behavior
                // using net protocols.
                .setEnableHttpCache(HttpEngine.Builder.HTTP_CACHE_DISK_NO_HTTP, 100 * 1024)

                // HTTP2 and QUIC support is enabled by default. When both are enabled (and no hints
                // are provided), Cronet tries to use both protocols and it's nondeterministic which
                // one will be used for the first few requests. As soon as Cronet is aware that
                // a server supports QUIC, it will always attempt to use it first. Try disabling
                // and enabling HTTP2 support and see how the negotiated protocol changes! Also try
                // forcing a new connection by enabling and disabling flight mode after the first
                // request to ensure QUIC usage.
                .setEnableHttp2(true)
                .setEnableQuic(true)

                // Brotli support is NOT enabled by default.
                .setEnableBrotli(true)

                // One can provide a custom user agent if desired.
                .setUserAgent("CronetSampleApp")

                // As noted above, QUIC hints speed up initial requests to a domain. Multiple hints
                // can be added. We don't enable them in this demo to demonstrate how QUIC
                // is being used if no hints are provided.

                // .addQuicHint("storage.googleapis.com", 443, 443)
                // .addQuicHint("www.googleapis.com", 443, 443)
                .build();
    }

}
