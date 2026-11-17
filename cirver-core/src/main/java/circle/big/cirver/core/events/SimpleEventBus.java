package circle.big.cirver.core.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class SimpleEventBus implements EventBus, AutoCloseable {
    private static final System.Logger LOGGER = System.getLogger(SimpleEventBus.class.getName());

    private final Map<Class<?>, List<Listener<?>>> listeners = new HashMap<>();
    private final BiConsumer<Object, RuntimeException> exceptionHandler;
    private boolean closed;

    public SimpleEventBus() {
        this((event, failure) -> LOGGER.log(System.Logger.Level.ERROR,
                "Subscriber failed for " + event.getClass().getName(), failure));
    }

    public SimpleEventBus(BiConsumer<Object, RuntimeException> exceptionHandler) {
        this.exceptionHandler = Objects.requireNonNull(exceptionHandler, "exceptionHandler");
    }

    @Override
    public synchronized <E> Subscription subscribe(Class<E> eventType, Consumer<? super E> handler) {
        ensureOpen();
        var listener = new Listener<>(Objects.requireNonNull(eventType, "eventType"),
                Objects.requireNonNull(handler, "handler"));
        listeners.computeIfAbsent(eventType, ignored -> new ArrayList<>()).add(listener);
        return () -> unsubscribe(listener);
    }

    private synchronized void unsubscribe(Listener<?> listener) {
        listener.active = false;
        var matching = listeners.get(listener.eventType);
        if (matching != null) {
            matching.remove(listener);
            if (matching.isEmpty()) {
                listeners.remove(listener.eventType);
            }
        }
    }

    @Override
    public void publish(Object event) {
        Objects.requireNonNull(event, "event");
        List<Listener<?>> snapshot;
        synchronized (this) {
            ensureOpen();
            snapshot = List.copyOf(listeners.getOrDefault(event.getClass(), List.of()));
        }
        for (var listener : snapshot) {
            if (listener.active) {
                try {
                    listener.accept(event);
                } catch (RuntimeException failure) {
                    try {
                        exceptionHandler.accept(event, failure);
                    } catch (RuntimeException reporterFailure) {
                        LOGGER.log(System.Logger.Level.ERROR, "Event exception reporter failed", reporterFailure);
                    }
                }
            }
        }
    }

    @Override
    public synchronized void close() {
        closed = true;
        listeners.values().forEach(group -> group.forEach(listener -> listener.active = false));
        listeners.clear();
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Event bus is closed");
        }
    }

    private static final class Listener<E> {
        private final Class<E> eventType;
        private final Consumer<? super E> handler;
        private volatile boolean active = true;

        private Listener(Class<E> eventType, Consumer<? super E> handler) {
            this.eventType = eventType;
            this.handler = handler;
        }

        private void accept(Object event) {
            handler.accept(eventType.cast(event));
        }
    }
}
