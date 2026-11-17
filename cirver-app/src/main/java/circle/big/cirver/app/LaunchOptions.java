package circle.big.cirver.app;

import java.nio.file.Path;

/** The CLI selects shared roots; each hosted project owns its settings and data. */
record LaunchOptions(Path configDirectory, Path dataDirectory, boolean help) {
    static final String USAGE = """
            Usage: cirver [--config-dir <directory>] [--data-dir <directory>] [--help]

              --config-dir, -c <directory>  Configuration root (default: ./config)
              --data-dir, -d <directory>    Data root (default: /data)
              --help, -h                    Show this help without starting systems

            Each project reads its own subdirectory, for example:
              <directory>/wilp2p/config.properties
            """;

    static LaunchOptions parse(String[] args) {
        Path configDirectory = Path.of("config");
        Path dataDirectory = Path.of("/data");
        boolean help = false;
        boolean configSpecified = false;
        boolean dataSpecified = false;
        for (int i = 0; i < args.length; i++) {
            String argument = args[i];
            if (argument.equals("--help") || argument.equals("-h")) {
                help = true;
                continue;
            }
            boolean configOption = argument.equals("--config-dir") || argument.equals("-c")
                    || argument.startsWith("--config-dir=");
            boolean dataOption = argument.equals("--data-dir") || argument.equals("-d")
                    || argument.startsWith("--data-dir=");
            if (!configOption && !dataOption) {
                throw new IllegalArgumentException("Unexpected argument: " + argument);
            }
            String value;
            int equals = argument.indexOf('=');
            if (equals >= 0) {
                value = argument.substring(equals + 1);
            } else {
                if (i + 1 == args.length || args[i + 1].startsWith("-")) {
                    throw new IllegalArgumentException("Missing directory after " + argument);
                }
                value = args[++i];
            }
            String name = configOption ? "Configuration" : "Data";
            if (configOption ? configSpecified : dataSpecified) {
                throw new IllegalArgumentException(name + " directory specified more than once");
            }
            if (value.isBlank()) {
                throw new IllegalArgumentException(name + " directory must not be empty");
            }
            if (configOption) {
                configDirectory = Path.of(value);
                configSpecified = true;
            } else {
                dataDirectory = Path.of(value);
                dataSpecified = true;
            }
        }
        return new LaunchOptions(configDirectory.toAbsolutePath().normalize(),
                dataDirectory.toAbsolutePath().normalize(), help);
    }
}
