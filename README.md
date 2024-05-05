# Gossip Glomers - Distributed systems challenges

Install Maelstrom and its [system dependencies](https://github.com/jepsen-io/maelstrom/blob/main/doc/01-getting-ready/index.md):

```bash
brew install openjdk graphviz gnuplot
curl -L https://github.com/jepsen-io/maelstrom/releases/download/v0.2.3/maelstrom.tar.bz2 | tar xf - -C build
```

Build and run project:

```bash
./gradlew installDist
```

Run project against "broadcast A" workload:

```bash
./gradlew installDist && ./maelstrom/maelstrom test -w broadcast --bin ./app/build/install/app/bin/app --node-count 5 --time-limit 20 --rate 10
```
