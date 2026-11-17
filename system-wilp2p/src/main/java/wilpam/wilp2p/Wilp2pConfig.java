package wilpam.wilp2p;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

/** Wilp2p owns the format and validation of its configuration directory. */
public record Wilp2pConfig(InetSocketAddress address, Path projectsFile) {
    private static final Set<String> KEYS = Set.of("host", "port", "projects");

    public Wilp2pConfig {
        Objects.requireNonNull(address, "address");
        Objects.requireNonNull(projectsFile, "projectsFile");
    }

    public static Wilp2pConfig load(Path directory) throws IOException {
        Path root = directory.toAbsolutePath().normalize();
        Path file = root.resolve("config.properties");
        var properties = new Properties();
        try (var reader = Files.newBufferedReader(file)) {
            properties.load(reader);
        } catch (IOException failure) {
            throw new IOException("wilp2p: cannot read " + file, failure);
        }
        for (String key : properties.stringPropertyNames()) {
            if (!KEYS.contains(key)) {
                throw new IllegalArgumentException("wilp2p: unknown setting '" + key + "' in " + file);
            }
        }
        String host = properties.getProperty("host", "0.0.0.0").strip();
        String portValue = properties.getProperty("port", "8080").strip();
        int port;
        try {
            port = Integer.parseInt(portValue);
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException("wilp2p: port must be an integer from 0 to 65535: " + portValue);
        }
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("wilp2p: port must be from 0 to 65535: " + portValue);
        }
        if (host.isEmpty()) {
            throw new IllegalArgumentException("wilp2p: host must not be empty");
        }
        var address = new InetSocketAddress(host, port);
        if (address.isUnresolved()) {
            throw new IllegalArgumentException("wilp2p: cannot resolve host: " + host);
        }
        String projects = properties.getProperty("projects", "projects.yaml").strip();
        if (projects.isEmpty()) {
            throw new IllegalArgumentException("wilp2p: projects must not be empty");
        }
        Path projectsFile = root.resolve(projects).normalize();
        if (!Files.isRegularFile(projectsFile)) {
            throw new IllegalArgumentException("wilp2p: projects file does not exist: " + projectsFile);
        }
        return new Wilp2pConfig(address, projectsFile);
    }
}
