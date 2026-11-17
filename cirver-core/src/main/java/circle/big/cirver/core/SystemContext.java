package circle.big.cirver.core;

import circle.big.cirver.core.events.EventBus;

import java.nio.file.Path;
import java.util.Set;

/** A context for getting declared dependencies, owned event subscriptions, and directories for this system. */
public final class SystemContext {
    private final Registry<Capability> capabilities;
    private final Set<Class<? extends Capability>> dependencies;
    private final EventBus events;
    private final Path configDirectory;
    private final Path dataDirectory;

    SystemContext(Registry<Capability> capabilities, Set<Class<? extends Capability>> dependencies,
                  EventBus events, Path configDirectory, Path dataDirectory) {
        this.capabilities = capabilities;
        this.dependencies = dependencies;
        this.events = events;
        this.configDirectory = configDirectory;
        this.dataDirectory = dataDirectory;
    }

    public <T extends Capability> T require(Class<T> capability) {
        if (!dependencies.contains(capability)) {
            throw new IllegalStateException("Undeclared dependency: " + capability.getName());
        }
        return capabilities.require(capability);
    }

    public EventBus events() {
        return events;
    }

    /** Absolute configuration directory for the project named at registration. */
    public Path configDirectory() {
        return configDirectory;
    }

    /** Absolute data path for the project named at registration. You may need to create this since the path isn't guaranteed to exist yet. */
    public Path dataDirectory() {
        return dataDirectory;
    }
}
