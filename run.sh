#!/usr/bin/env sh
set -eu
cirver_root=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
exec java --module-path "$cirver_root/cirver-app/target/cirver-app-1.0-SNAPSHOT.jar:$cirver_root/cirver-app/target/dependency" \
    --module circle.big.cirver.app/circle.big.cirver.app.Initializer "$@"
