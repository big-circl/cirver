# Cirver core

The framework manages systems in one JVM, with direct capability calls and a
shared event bus. Applications register their systems explicitly; see the
[launcher](../cirver-app/README.md).

## Registering systems and capabilities

```java
var holder = new SystemHolder();
var encyclopedia = new EncyclopediaSystem();
holder.register("encyclopedia", encyclopedia).provides(Encyclopedia.class, encyclopedia);
holder.register("encyclopedia-web", new EncyclopediaWebHost());

holder.run(); // Starts systems and waits for JVM shutdown.
```

The registration name identifies the project. Systems in the same project share
its directories.

## What is a capability?

A capability is an interface extending the empty `Capability` marker:

```java
public interface Encyclopedia extends Capability {
    boolean exists(String word);
    Optional<WordDetails> getDetails(String word);
}
```

Chain `.provides(...)` to expose multiple capabilities. Each interface has one
provider, which can be the system itself or a separate object. Consumers depend
on these interfaces rather than concrete systems.

`Registry<B>` provides type-safe `register`, `require`, and `find` operations.
The holder uses `Registry<Capability>` for registering capabilities (obviously).
Registries can also be used independently if you wish to do so.

Application code can retrieve capabilities with `holder.require(...)`.

## Implementing a system

```java
import circle.big.cirver.core.Capability;
import circle.big.cirver.core.System;
import circle.big.cirver.core.SystemContext;
import circle.big.cirver.core.events.EventBus;

import java.util.List;
import java.util.Optional;

public final class WordGameLegallyDistinctFromWordleSystem implements System {
    private Encyclopedia encyclopedia;
    private EventBus events;

    @Override
    public List<Class<? extends Capability>> dependencies() {
        return List.of(Encyclopedia.class);
    }

    @Override
    public void initialize(SystemContext context) {
        encyclopedia = context.require(Encyclopedia.class);
        events = context.events();
    }

    public Optional<WordDetails> lookup(String word) {
        var details = encyclopedia.getDetails(word);
        events.publish(new WordLookedUpEvent(word, details.isPresent()));
        return details;
    }
}
```

Declare required capabilities in `dependencies()` before retrieving them through
`context.require(...)`. Undeclared lookups are rejected. The context also supplies
the event bus and the project's directories.

## Lifecycle

Finish registration before calling `run()`. Missing capabilities and dependency
cycles are checked before lifecycle callbacks. All systems initialize first, then
start in dependency order, with providers before consumers. Shutdown calls `stop()`
in reverse order.

`initialize(context)`, `start()`, and `stop()` default to no-ops and may throw
checked exceptions. Startup failures trigger cleanup, so `stop()` **must** tolerate
partial initialization and stop any workers the system owns. Cleanup continues
if a stop fails; lifecycle failures are reported through `SystemLifecycleException`.

`run()` waits until JVM shutdown or an explicit `close()`. Closing repeatedly is
safe. Each holder runs once; restarting and late registration are rejected.

## Project directories

`context.configDirectory()` and `context.dataDirectory()` point to the registered
project's folders. With the default roots, project `encyclopedia` receives
`./config/encyclopedia` and `/data/encyclopedia` as absolute paths.

Systems own their configuration format and data files. Directories are not
created automatically. See the [launcher](../cirver-app/README.md) to change roots.

## Events

Use capabilities for requests and queries, and events for notifications. Events
can be ordinary Java objects or records:

```java
public record WordLookedUpEvent(String word, boolean found) { }

// Inside initialize(context):
var subscription = context.events().subscribe(
        WordLookedUpEvent.class,
        event -> java.lang.System.out.println("Looked up " + event.word()));

context.events().publish(new WordLookedUpEvent("java", true));
subscription.close(); // You can also unsubscribe if you want to. Otherwise, it'll get cleaned up at program exit.
```

- Delivery is synchronous on the publishing thread, in subscription order, for the
  event's exact runtime class.
- Subscriber `RuntimeException`s are logged and delivery continues.

`SimpleEventBus` can also be used independently with try-with-resources.
