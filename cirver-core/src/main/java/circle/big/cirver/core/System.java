package circle.big.cirver.core;

import java.util.List;

/** A system of the monolith. */
public interface System {
    /** Capability interfaces needed at any point. */
    default List<Class<? extends Capability>> dependencies() {
        return List.of();
    }

    default void initialize(SystemContext context) throws Exception {
    }

    default void start() throws Exception {
    }

    /** Be mindful of this! It may be partly initialized when stop() is called. */
    default void stop() throws Exception {
    }
}
