package circle.big.cirver.core;

import circle.big.cirver.core.events.EventBus;
import circle.big.cirver.core.events.SimpleEventBus;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.function.BiConsumer;

/** Handles system holding and setup. */
public final class SystemHolder implements AutoCloseable {
    public enum State { NEW, STARTING, RUNNING, STOPPING, STOPPED, FAILED }

    private final Registry<Capability> capabilities = new Registry<>(Capability.class);
    private final Map<Class<? extends Capability>, Registration> providers = new HashMap<>();
    private final List<Registration> registrations = new ArrayList<>();
    private final SimpleEventBus events;
    private final Path configDirectory;
    private final Path dataDirectory;
    private final CountDownLatch terminated = new CountDownLatch(1);
    private final List<Registration> initialized = new ArrayList<>();
    private volatile State state = State.NEW;

    public SystemHolder() {
        this(Path.of("config"), Path.of("/data"));
    }

    public SystemHolder(Path configDirectory, Path dataDirectory) {
        this.configDirectory = Objects.requireNonNull(configDirectory, "configDirectory").toAbsolutePath().normalize();
        this.dataDirectory = Objects.requireNonNull(dataDirectory, "dataDirectory").toAbsolutePath().normalize();
        events = new SimpleEventBus();
    }

    public SystemHolder(BiConsumer<Object, RuntimeException> eventExceptionHandler) {
        this(Path.of("config"), Path.of("/data"), eventExceptionHandler);
    }

    public SystemHolder(Path configDirectory, Path dataDirectory,
                        BiConsumer<Object, RuntimeException> eventExceptionHandler) {
        this.configDirectory = Objects.requireNonNull(configDirectory, "configDirectory").toAbsolutePath().normalize();
        this.dataDirectory = Objects.requireNonNull(dataDirectory, "dataDirectory").toAbsolutePath().normalize();
        events = new SimpleEventBus(eventExceptionHandler);
    }

    /** Systems registered to the same project share its configuration and data directories. */
    public Registration register(String project, System system) {
        requireState(State.NEW);
        Objects.requireNonNull(project, "project");
        if (!project.matches("[A-Za-z0-9][A-Za-z0-9._-]*")) {
            throw new IllegalArgumentException("Invalid project name: " + project);
        }
        Objects.requireNonNull(system, "system");
        if (registrations.stream().anyMatch(registration -> registration.system == system)) {
            throw new IllegalArgumentException("System instance already registered: " + system.getClass().getName());
        }
        var registration = new Registration(project, system);
        registrations.add(registration);
        return registration;
    }

    public <T extends Capability> T require(Class<T> capability) {
        return capabilities.require(capability);
    }

    /** Application-level subscriptions must be closed manually (or by closing this holder).
     *
     * If you're looking here, you probably want {@link SystemContext#events()}. */
    public EventBus events() {
        return events;
    }

    public State state() {
        return state;
    }

    public void run() {
        var shutdownHook = new Thread(this::close, "cirver-shutdown");
        try {
            synchronized (this) {
                requireState(State.NEW);
                Runtime.getRuntime().addShutdownHook(shutdownHook);
                startAll();
            }
            await(terminated);
        } finally {
            try {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            } catch (IllegalStateException shutdownInProgress) {
                // The JVM is already executing its shutdown hooks.
            }
        }
    }

    private void startAll() {
        state = State.STARTING;
        try {
            var ordered = resolveOrder();
            for (var registration : ordered) {
                initialized.add(registration);
                registration.system.initialize(new SystemContext(capabilities,
                        Set.copyOf(registration.dependencies), registration.events,
                        configDirectory.resolve(registration.project), dataDirectory.resolve(registration.project)));
            }
            for (var registration : ordered) {
                registration.system.start();
            }
            state = State.RUNNING;
        } catch (Exception | Error failure) {
            var cleanupFailure = cleanup();
            state = State.FAILED;
            terminated.countDown();
            if (failure instanceof Error error) {
                if (cleanupFailure != null) {
                    error.addSuppressed(cleanupFailure);
                }
                throw error;
            }
            var exception = new SystemLifecycleException("System startup failed", failure);
            if (cleanupFailure != null) {
                exception.addSuppressed(cleanupFailure);
            }
            throw exception;
        }
    }

    @Override
    public synchronized void close() {
        if (state == State.STARTING) {
            throw new IllegalStateException("Cannot close holder from a startup callback");
        }
        if (state == State.STOPPING || state == State.STOPPED || state == State.FAILED) {
            return;
        }
        state = State.STOPPING;
        var failure = cleanup();
        state = failure == null ? State.STOPPED : State.FAILED;
        terminated.countDown();
        if (failure != null) {
            throw failure;
        }
    }

    private static void await(CountDownLatch latch) {
        boolean interrupted = false;
        while (true) {
            try {
                latch.await();
                break;
            } catch (InterruptedException interruption) {
                interrupted = true;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private List<Registration> resolveOrder() {
        for (var registration : registrations) {
            registration.dependencies = List.copyOf(registration.system.dependencies());
            for (var dependency : registration.dependencies) {
                if (!providers.containsKey(dependency)) {
                    throw new NoSuchElementException("Missing capability " + dependency.getName()
                            + " required by " + registration.system.getClass().getName());
                }
            }
        }
        var ordered = new ArrayList<Registration>();
        var visited = new IdentityHashMap<Registration, Boolean>();
        var path = new ArrayList<Registration>();
        for (var registration : registrations) {
            visit(registration, visited, path, ordered);
        }
        return ordered;
    }

    private void visit(Registration registration, Map<Registration, Boolean> visited,
                       List<Registration> path, List<Registration> ordered) {
        Boolean complete = visited.get(registration);
        if (Boolean.TRUE.equals(complete)) {
            return;
        }
        if (Boolean.FALSE.equals(complete)) {
            var cycle = new ArrayList<String>();
            for (var member : path.subList(path.indexOf(registration), path.size())) {
                cycle.add(member.system.getClass().getName());
            }
            cycle.add(registration.system.getClass().getName());
            throw new IllegalStateException("Circular system dependency: " + String.join(" -> ", cycle));
        }
        visited.put(registration, false);
        path.add(registration);
        for (var dependency : registration.dependencies) {
            visit(providers.get(dependency), visited, path, ordered);
        }
        path.removeLast();
        visited.put(registration, true);
        ordered.add(registration);
    }

    private SystemLifecycleException cleanup() {
        SystemLifecycleException failure = null;
        for (int i = initialized.size() - 1; i >= 0; i--) {
            var registration = initialized.get(i);
            try {
                registration.system.stop();
            } catch (Exception | Error stopFailure) {
                if (failure == null) {
                    failure = new SystemLifecycleException("System shutdown failed: "
                            + registration.system.getClass().getName(), stopFailure);
                } else {
                    failure.addSuppressed(stopFailure);
                }
            } finally {
                registration.events.close();
            }
        }
        // Also seals scopes belonging to systems that were never initialized.
        registrations.forEach(registration -> registration.events.close());
        events.close();
        return failure;
    }

    private void requireState(State expected) {
        if (state != expected) {
            throw new IllegalStateException("Expected holder state " + expected + ", was " + state);
        }
    }

    /** Associates any number of interface implementations with one system lifecycle. */
    public final class Registration {
        private final String project;
        private final System system;
        private final ScopedEventBus events = new ScopedEventBus(SystemHolder.this.events);
        private List<Class<? extends Capability>> dependencies = List.of();

        private Registration(String project, System system) {
            this.project = project;
            this.system = system;
        }

        public <T extends Capability> Registration provides(Class<T> capability, T implementation) {
            requireState(State.NEW);
            capabilities.register(capability, implementation);
            providers.put(capability, this);
            return this;
        }
    }
}
