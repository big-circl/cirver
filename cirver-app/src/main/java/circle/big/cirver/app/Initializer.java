package circle.big.cirver.app;

import circle.big.cirver.core.SystemHolder;
import circle.big.cirver.core.SystemLifecycleException;
import wilpam.wilp2p.Wilp2pSystem;

import java.io.PrintStream;
import java.nio.file.Files;

/** Composition root for the projects hosted by this process. */
public final class Initializer {
    public static void main(String[] args) {
        int result = launch(args, System.out, System.err);
        if (result != 0) {
            System.exit(result);
        }
    }

    static int launch(String[] args, PrintStream output, PrintStream errors) {
        try {
            var options = LaunchOptions.parse(args);
            if (options.help()) {
                output.print(LaunchOptions.USAGE);
                return 0;
            }
            if (!Files.isDirectory(options.configDirectory())) {
                throw new IllegalArgumentException("Not a configuration directory: " + options.configDirectory());
            }
            var holder = new SystemHolder(options.configDirectory(), options.dataDirectory());
            registerSystems(holder);
            holder.run();
            return 0;
        } catch (IllegalArgumentException configurationError) {
            errors.println("cirver: " + configurationError.getMessage());
            errors.println("Use --help for usage.");
            return 2;
        } catch (SystemLifecycleException startupError) {
            Throwable cause = startupError;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            errors.println("cirver: could not start systems: " + cause.getMessage());
            return 1;
        }
    }

    private static void registerSystems(SystemHolder holder) {
        holder.register("wilp2p", new Wilp2pSystem());
    }
}
