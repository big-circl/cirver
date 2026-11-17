# Cirver

A lightweight modular monolith for semi-independent systems in one JVM. 

## Usage

```sh
mvn clean verify
./run.sh                               # Config: ./config; data: /data
./run.sh --config-dir ./config --data-dir ./local-data
./run.sh --help
```

| Module | What do it do? |
| --- | --- |
| [cirver-core](cirver-core/README.md) | The framework for making it all work. With frames. I'm sorry. |
| [cirver-app](cirver-app/README.md) | The actual app, along with registration |
| [system-wilp2p](system-wilp2p/README.md) | wilp2p |
