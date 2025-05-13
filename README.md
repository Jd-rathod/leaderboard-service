
# 🏆 Real-Time Leaderboard Service

A high-throughput, low-latency leaderboard service built with Java Spring Boot, Kafka, and PostgreSQL. Designed to handle **10,000 writes/sec** and **5,000 reads/sec per game**, this system supports real-time Top-K rankings, percentile lookups, and sliding-window leaderboards.

---

## 🚀 Features

- ✅ Real-time Top-K and rank retrieval
- ✅ 24h / 3d / 7d sliding window support
- ✅ Write-Ahead Logging (WAL) for crash recovery
- ✅ Asynchronous Kafka-based ingestion
- ✅ PostgreSQL for long-term persistence
- ✅ Horizontal sharding support by `gameId`

---

## 🧠 Data Structures

| Structure                          | Purpose                         |
|-----------------------------------|----------------------------------|
| `TreeMap<Integer, Set<String>>`   | Score → userIds (for ranking)   |
| `HashMap<String, ScoreEntry>`     | Fast user lookup                |
| `PriorityQueue<ScoreEntry>`       | Top-K tracking (min-heap)       |
| `Deque<ScoreEntry>`               | Sliding window expiry           |

---

## 📈 Time Complexities

| Operation               | Complexity   |
|-------------------------|--------------|
| Add/update score        | O(log n)     |
| Get rank of user        | O( r )         |
| Get percentile          | O(1)         |
| Get Top-K players       | O(k log k)   |
| Expire old scores       | O(1)         |

---

## 💾 Persistence & Recovery

- **WAL**:
  - Per-gameId `.log` file
  - Async + batched writes
  - Replayed on service startup

- **PostgreSQL**:
  - Async batch inserts using Spring JPA
  - Enables analytical querying & long-term durability

---

## 🧱 Scale-Out Strategy

- ShardRouter maps `gameId → shard` using:
  ```java
  Math.abs(gameId.hashCode()) % shardCount;
  ```

- Each shard uses a separate `LeaderboardService` instance for concurrency.

---

## ☁️ Horizontal Scaling (Multi-Node)

- Use **consistent hashing** for gameId distribution
- Service discovery via **ZooKeeper**
- Cache warm-up via WAL replay or DB preload

---

## 🔄 Consistency Model

| Layer       | Consistency         |
|-------------|----------------------|
| In-Memory   | Strong               |
| WAL         | Eventual (batched)   |
| Kafka       | At-least-once        |
| PostgreSQL  | Eventual             |

---

## 📊 Performance Goals

- ⚙️ 10K writes/sec
- 📥 5K reads/sec per game
- ⏱️ < 50ms P99 latency (single node, 4-core, 512MB heap)

---

## 🛠️ Tech Stack

- Java 17, Spring Boot
- Kafka & Zookeeper
- PostgreSQL
- Docker / Docker Compose

---

## 📂 Project Structure

- `LeaderboardService` – per-shard instance handling ingestion & queries
- `GameLeaderboard` – in-memory structure for each game + window
- `WALManager` – write-ahead log persistence
- `ScoreKafkaProducer/Consumer` – async Kafka ingestion
- `ShardRouter` – maps gameId → shard

---

## 🧪 Load Testing

```bash
pip3 install aiohttp tqdm
python3 load_test_scores.py
```

> Ensure Python 3 is installed and Kafka/Postgres are running.

---

## 🐳 Running the App

```bash
docker-compose up --build
```

Or run locally from IntelliJ if Kafka/Postgres are already running via Docker.

---

## 👤 Author

Built by **Jay Rathod** – [LinkedIn](https://www.linkedin.com/in/jay-d-rathod) | [GitHub](https://github.com/Jd-rathod)

