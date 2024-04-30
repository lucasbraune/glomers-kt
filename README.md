# Gossip Glomers - Distributed systems challenges

Install Maelstrom's [system dependencies](https://github.com/jepsen-io/maelstrom/blob/main/doc/01-getting-ready/index.md):

```sh
brew install openjdk graphviz gnuplot
```

Download maelstrom:

```bash
curl -L https://github.com/jepsen-io/maelstrom/releases/download/v0.2.3/maelstrom.tar.bz2 | tar xf - -C build
```

Build project:

```bash
./gradlew installDist
```

Run project against "echo" workload:

```bash
./app/build/maelstrom/maelstrom test -w echo --bin ./app/build/install/app/bin/app --node-count 1 --time-limit 10 
```

