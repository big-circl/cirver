package circle.big.cirver.core;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

public final class Registry<B> {
    private final Class<B> baseType;
    private final Map<Class<? extends B>, B> capabilities = new HashMap<>();

    /** We need the baseType supplied for runtime validation. */
    public Registry(Class<B> baseType) {
        this.baseType = Objects.requireNonNull(baseType, "baseType");
    }

    public synchronized <T extends B> void register(Class<T> capability, T implementation) {
        Objects.requireNonNull(capability, "capability");
        Objects.requireNonNull(implementation, "implementation");
        if (!capability.isInterface() || !baseType.isAssignableFrom(capability)) {
            throw new IllegalArgumentException("Capability must be an interface extending " + baseType.getName() + ": "
                    + capability.getName());
        }
        capability.cast(implementation);
        if (capabilities.containsKey(capability)) {
            throw new IllegalArgumentException("Duplicate capability: " + capability.getName());
        }
        capabilities.put(capability, implementation);
    }

    public synchronized <T extends B> T require(Class<T> capability) {
        return find(capability).orElseThrow(() ->
                new NoSuchElementException("Missing capability: " + capability.getName()));
    }

    public synchronized <T extends B> Optional<T> find(Class<T> capability) {
        Objects.requireNonNull(capability, "capability");
        return Optional.ofNullable(capability.cast(capabilities.get(capability)));
    }
}
