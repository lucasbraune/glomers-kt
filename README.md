# Gossip Glomers - Distributed systems challenges

Solutions to Fly.io's [distributed systems challenges](https://fly.io/dist-sys/).

## Getting started

Install Maelstrom and its [system dependencies](https://github.com/jepsen-io/maelstrom/blob/main/doc/01-getting-ready/index.md):

```bash
brew install openjdk graphviz gnuplot
curl -L https://github.com/jepsen-io/maelstrom/releases/download/v0.2.3/maelstrom.tar.bz2 | tar xf -
```

Build and run:

```bash
./gradlew :broadcast:runMaelstromTest
```
