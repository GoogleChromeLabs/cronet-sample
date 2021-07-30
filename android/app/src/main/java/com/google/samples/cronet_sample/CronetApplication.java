package com.google.samples.cronet_sample;

import android.app.Application;
import android.content.Context;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.net.CronetProviderInstaller;
import com.google.android.gms.tasks.Task;

import org.chromium.net.CronetEngine;
import org.chromium.net.CronetProvider;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class CronetApplication extends Application {

    // We recommend that each application uses a single, global CronetEngine. This allows Cronet
    // to maximize performance. This can either be achieved using a global static . In this example,
    // we initialize it in an Application class to manage lifecycle of the network log.
    private CronetEngine cronetEngine;

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

    public CronetEngine getCronetEngine() {
        return cronetEngine;
    }

    public ExecutorService getCronetCallbackExecutorService() {
        return cronetCallbackExecutorService;
    }

    private static CronetEngine createDefaultCronetEngine(Context context) {
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
        return new CronetEngine.Builder(context)
                // The storage path must be set first when using a disk cache.
                .setStoragePath(context.getFilesDir().getAbsolutePath())

                // Enable on-disk cache, this enables automatic QUIC usage for subsequent requests
                // to the same domain across application restarts. If you also want to cache HTTP
                // responses, use HTTP_CACHE_DISK instead. Typically you will want to enable caching
                // in full, we turn it off for this demo to better demonstrate Cronet's behavior
                // using net protocols.
                .enableHttpCache(CronetEngine.Builder.HTTP_CACHE_DISK_NO_HTTP, 100 * 1024)

                // HTTP2 and QUIC support is enabled by default. When both are enabled (and no hints
                // are provided), Cronet tries to use both protocols and it's nondeterministic which
                // one will be used for the first few requests. As soon as Cronet is aware that
                // a server supports QUIC, it will always attempt to use it first. Try disabling
                // and enabling HTTP2 support and see how the negotiated protocol changes! Also try
                // forcing a new connection by enabling and disabling flight mode after the first
                // request to ensure QUIC usage.
                .enableHttp2(true)
                .enableQuic(true)

                // Brotli support is NOT enabled by default.
                .enableBrotli(true)

                // One can provide a custom user agent if desired.
                .setUserAgent("CronetSampleApp")

                // As noted above, QUIC hints speed up initial requests to a domain. Multiple hints
                // can be added. We don't enable them in this demo to demonstrate how QUIC
                // is being used if no hints are provided.

                // .addQuicHint("storage.googleapis.com", 443, 443)
                // .addQuicHint("www.googleapis.com", 443, 443)
                .build();
    }

    private static void createCustomCronetEngine(Context context) {
        // For most users of Cronet on modern devices it should be sufficient to just create
        // a CronetEngine.Builder directly, as demonstrated in createDefaultCronetEngine().
        // The implementation selects the "best" (the most recent) implementation of Cronet
        // available on the device. However, if the application requires more control over which
        // Cronet engine is selected, we allow that too.

        // To guarantee that a Google Play Services Cronet implementation is available,
        // one can explicitly install the Play Services Cronet provider. The returned task can
        // then be used to either proceed with creating a CronetEngine, launching an intent to
        // upgrade Google Play Services, or handling Play Services absence gracefully.
        Task<?> installTask = CronetProviderInstaller.installProvider(context);
        installTask.addOnCompleteListener(
                task -> {
                    if (task.isSuccessful()) {
                        // create a Cronet engine
                        return;
                    }
                    if (task.getException() != null) {
                        Exception cause = task.getException();
                        if (cause instanceof GooglePlayServicesNotAvailableException) {
                            Toast.makeText(context, "Google Play services not available.",
                                    Toast.LENGTH_SHORT).show();
                        } else if (cause instanceof GooglePlayServicesRepairableException) {
                            Toast.makeText(context, "Google Play services update is required.",
                                    Toast.LENGTH_SHORT).show();
                            context.startActivity(((GooglePlayServicesRepairableException) cause)
                                    .getIntent());
                        } else {
                            Toast.makeText(context, "Unexpected error: " + cause,
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(context, "Unable to load Google Play services.",
                                Toast.LENGTH_SHORT).show();
                    }
                });

        // If the user wants to pick a Cronet implementation (from those available on the device)
        // manually, they can retrieve the list of all providers on the device and use them to
        // create a CronetEngine directly. Make sure to check if the providers are enabled,
        // otherwise you might run into compatibility issues further down.
        List<CronetProvider> enabledProviders =
                CronetProvider.getAllProviders(context)
                        .stream()
                        .filter(CronetProvider::isEnabled)
                        .collect(Collectors.toList());

        if (enabledProviders.isEmpty()) {
            Toast.makeText(context, "No enabled Cronet providers!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Typically, the application would decide based on the name and version. We just pick
        // a random one.
        Collections.shuffle(enabledProviders);
        CronetProvider winner = enabledProviders.get(0);
        Toast.makeText(context, "And the winning Cronet implementation is " + winner.getName() +
                        ", version " + winner.getVersion(),
                Toast.LENGTH_SHORT).show();

        // Then, one can use the provider to create a builder, and set it up as demonstrated
        // in createDefaultCronetEngine.
        winner.createBuilder().enableBrotli(true).build();
    }
}
