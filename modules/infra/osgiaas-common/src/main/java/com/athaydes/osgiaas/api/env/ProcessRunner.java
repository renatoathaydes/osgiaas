package com.athaydes.osgiaas.api.env;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * A Simple wrapper around {@link ProcessBuilder} to make it easy to run native commands,
 * redirecting the output to provided {@link PrintStream}s.
 */
public class ProcessRunner {

    private final ExecutorService executorService = Executors.newFixedThreadPool( 2 );

    /**
     * Run the given command, redirecting its process output to the provided streams.
     *
     * @param commands         command + its arguments.
     * @param workingDirectory process working directory
     * @param out              regular output stream
     * @param err              error output stream
     * @return the status code returned by the process
     * @throws IOException          if some problem occurs when running the process.
     * @throws InterruptedException if the process gets interrupted.
     */
    public int run( List<String> commands,
                    File workingDirectory,
                    PrintStream out,
                    PrintStream err ) throws IOException, InterruptedException {
        Process process = new ProcessBuilder( commands )
                .directory( workingDirectory )
                .redirectInput( ProcessBuilder.Redirect.INHERIT )
                .start();

        CountDownLatch latch = new CountDownLatch( 2 );

        consume( process.getInputStream(), out, err, latch );
        consume( process.getErrorStream(), err, err, latch );

        int exitValue = process.waitFor();

        boolean noTimeout = latch.await( 5, TimeUnit.SECONDS );

        if ( !noTimeout ) {
            err.println( "Process timeout! Killing it forcefully" );
            process.destroyForcibly();
        }

        return exitValue;
    }

    /**
     * Shutdown the executor used internally. After a call to this method, this instance must
     * NOT be used again.
     */
    public void shutdown() {
        executorService.shutdownNow();
    }

    private void consume( InputStream stream,
                          PrintStream writer,
                          PrintStream err,
                          CountDownLatch latch ) {
        executorService.submit( () -> {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader( stream, StandardCharsets.UTF_8 ), 1024 );

            String nextLine;
            try {
                while ( ( nextLine = reader.readLine() ) != null ) {
                    writer.println( nextLine );
                }
            } catch ( Throwable e ) {
                e.printStackTrace( err );
            } finally {
                // done!
                latch.countDown();
            }
        } );
    }

}
