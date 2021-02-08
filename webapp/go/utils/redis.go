package utils

import (
	"github.com/go-redis/redis"
	sessionRedis "github.com/go-session/redis"
	"github.com/go-session/session"
	"log"
	"os"
	"time"
)

type REDIS struct{}

var (
	redisHost string
	redisPort string
)

func (myRedis *REDIS) WaitOK() {
	client := myRedis.GetRedisClient(0)
	defer client.Close()
}

func (myRedis *REDIS) GetRedisClient(db int, timeout ...time.Duration) *redis.Client {
	redisHost = os.Getenv("REDIS_HOST")
	if redisHost == "" {
		redisHost = "127.0.0.1"
	}

	redisPort = os.Getenv("REDIS_PORT")
	if redisPort == "" {
		redisPort = "6379"
	}

	newClient := redis.NewClient(&redis.Options{
		Addr:     redisHost + ":" + redisPort,
		Password: "",
		DB:       db,
	})

	spentTime := 0 * time.Second
	waitTime := WaitTimeMax
	if len(timeout) > 0 {
		waitTime = timeout[0]
	}

	for {
		_, err := newClient.Ping().Result()

		if err == nil || spentTime > waitTime {
			break
		} else {
			log.Printf("failed to connect to redis: %s\n", err.Error())

			time.Sleep(InitCheckInterval)
			spentTime += InitCheckInterval
		}
	}

	return newClient
}

func (myRedis *REDIS) GetRedisStore(db int) session.ManagerStore {
	store := sessionRedis.NewRedisStore(&sessionRedis.Options{
		Addr: redisHost + ":" + redisPort,
		DB:   db,
	})

	return store
}
