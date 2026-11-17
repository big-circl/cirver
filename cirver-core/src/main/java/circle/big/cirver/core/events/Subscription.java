package circle.big.cirver.core.events;

@FunctionalInterface
public interface Subscription extends AutoCloseable {
    @Override
    void close();
}
