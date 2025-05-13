import asyncio
import aiohttp
import time
from tqdm import tqdm
import random

URL = "http://localhost:8080/games/game123/scores"
TOTAL_REQUESTS = 10000
CONCURRENCY = 280  # number of concurrent clients

async def send_score(session, sem, user_id):
    payload = {
        "userId": f"user_{user_id}",
        "score": random.randint(1, 1000)
        # timestamp will be auto-set by server
    }

    async with sem:
        async with session.post(URL, json=payload) as response:
            if response.status != 200:
                print(f"Failed for user {user_id}: {response.status}")
            return await response.text()

async def run():
    sem = asyncio.Semaphore(CONCURRENCY)
    connector = aiohttp.TCPConnector(limit=0)
    async with aiohttp.ClientSession(connector=connector) as session:
        tasks = [send_score(session, sem, i) for i in range(TOTAL_REQUESTS)]
        for f in tqdm(asyncio.as_completed(tasks), total=TOTAL_REQUESTS):
            await f

if __name__ == "__main__":
    start = time.perf_counter()
    asyncio.run(run())
    duration = time.perf_counter() - start
    print(f"\nSent {TOTAL_REQUESTS} requests in {duration:.2f} seconds")
    print(f"Throughput: {TOTAL_REQUESTS / duration:.2f} req/sec")