package circle.big.cirver.core.events;

import java.util.function.Consumer;

public interface EventBus {
    <E> Subscription subscribe(Class<E> eventType, Consumer<? super E> handler);

    void publish(Object event);
}
