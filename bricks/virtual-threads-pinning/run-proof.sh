#!/usr/bin/env bash
# Compiles this brick and runs ONE proof's main class, unchanged, on JDK 21 and JDK 26, with the
# carrier count fixed by the two -D flags. Called by the mise tasks in the root mise.toml; not
# meant to be run directly from anywhere but this brick's own directory tree (paths below are
# relative to $BRICK, which mise sets via $MISE_PROJECT_ROOT — see the comment at the top of
# mise.toml for why every path here is absolute).
set -e

PROOF="$1"
[ -n "$PROOF" ] || { echo "usage: run-proof.sh <ProofClassName>"; exit 2; }

BRICK="$(cd "$(dirname "$0")" && pwd)"
CLASSES="$BRICK/target/classes"
MAIN="dev.lukasgrigis.foundations.virtualthreadspinning.proof.$PROOF"

cd "$BRICK"
# Skip recompiling when the caller (the "virtual-threads-pinning" umbrella task) already built once;
# a standalone `mise run virtual-threads-pinning:<proof>` still compiles for itself.
[ -n "${VTP_SKIP_BUILD:-}" ] || mvn -q compile

for jdk in openjdk-21.0.2 26.0.1; do
  JAVA="$(mise where "java@$jdk")/bin/java"
  [ -x "$JAVA" ] || { echo "missing JDK: mise install java@$jdk"; exit 2; }
  echo "================= $PROOF on java@$jdk ================="
  "$JAVA" \
    -Djdk.virtualThreadScheduler.parallelism=2 \
    -Djdk.virtualThreadScheduler.maxPoolSize=2 \
    -cp "$CLASSES" "$MAIN"
done
