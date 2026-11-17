# Cirver launcher

## Build and run

See the root readme for this.

## Arguments and directories

| Option | Default | Purpose |
| --- | --- | --- |
| `--config-dir <directory>`, `-c <directory>` | `./config` | Configuration root |
| `--data-dir <directory>`, `-d <directory>` | `/data` | Data root (absolute default) |
| `--help`, `-h` | | Show help without starting systems or loading configuration |

Each project gets a child of both roots, using the name supplied at registration:

```text
<config root>/
  project1/...
  project2/...
<data root>/
  project1/...
  project2/...
```

`SystemContext.configDirectory()` and `SystemContext.dataDirectory()` already point
to the project's own directories, though configuration needs to be handled per-system for now.

## Adding a project

Add its Maven/JPMS module, and then add the corresponding Maven dependency and JPMS `requires` to `cirver-app`.
Register its system with a **stable** project name in `Initializer.registerSystems`:

```java
private static void registerSystems(SystemHolder holder) {
    holder.register("wilp2p", new Wilp2pSystem());
    // holder.register("another-project", new AnotherSystem());
}
```