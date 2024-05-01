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

Run project against "unique ID" workload:

```bash
./maelstrom/maelstrom test -w unique-ids --bin ./app/build/install/app/bin/app --time-limit 30 --rate 1000 --node-count 3 --availability total --nemesis partition 
```

