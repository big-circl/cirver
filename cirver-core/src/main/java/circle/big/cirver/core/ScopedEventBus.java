package circle.big.cirver.core;

import circle.big.cirver.core.events.EventBus;
import circle.big.cirver.core.events.Subscription;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/** One system's subscriptions. */
final class ScopedEventBus implements EventBus, AutoCloseable {
    private final EventBus delegate;
    private final Set<OwnedSubscription> subscriptions = new LinkedHashSet<>();
    private boolean closed;

    ScopedEventBus(EventBus delegate) {
        this.delegate = delegate;
    }

    @Override
    public synchronized <E> Subscription subscribe(Class<E> eventType, Consumer<? super E> handler) {
        ensureOpen();
        var subscription = new OwnedSubscription(delegate.subscribe(eventType, handler));
        subscriptions.add(subscription);
        return subscription;
    }

    @Override
    public void publish(Object event) {
        synchronized (this) {
            ensureOpen();
        }
        delegate.publish(Objects.requireNonNull(event, "event"));
    }

    @Override
    public synchronized void close() {
        closed = true;
        subscriptions.forEach(subscription -> subscription.delegate.close());
        subscriptions.clear();
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("System event scope is closed");
        }
    }

    private final class OwnedSubscription implements Subscription {
        private final Subscription delegate;

        private OwnedSubscription(Subscription delegate) {
            this.delegate = delegate;
        }

        @Override
        public void close() {
            synchronized (ScopedEventBus.this) {
                delegate.close();
                subscriptions.remove(this);
            }
        }
    }
}
