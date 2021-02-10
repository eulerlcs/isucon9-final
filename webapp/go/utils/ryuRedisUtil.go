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

var RedisClient1 *redis.Client

var (
	redisHost string
	redisPort string
)

func (myRedis *REDIS) WaitOK() {
	if RedisClient1 != nil {
		return
	} else {
		RedisClient1 = myRedis.getRedisClient(1)
	}
}

func (myRedis *REDIS) GetRedisStore(db int) session.ManagerStore {
	store := sessionRedis.NewRedisStore(&sessionRedis.Options{
		Addr: redisHost + ":" + redisPort,
		DB:   db,
	})

	return store
}

func (myRedis *REDIS) getRedisClient(db int, timeout ...time.Duration) *redis.Client {
	redisHost = os.Getenv("REDIS_HOST")
	if redisHost == "" {
		redisHost = "127.0.0.1"
	}

	redisPort = os.Getenv("REDIS_PORT")
	if redisPort == "" {
		redisPort = "6379"
	}

	spentTime := 0 * time.Second
	waitTime := WaitTimeMax
	if len(timeout) > 0 {
		waitTime = timeout[0]
	}

	for {
		newClient := redis.NewClient(&redis.Options{
			Addr:     redisHost + ":" + redisPort,
			Password: "",
			DB:       db,
		})

		_, err := newClient.Ping().Result()
		if err == nil {
			log.Println("ZSJ - succeeded to connect to redis.")
			return newClient
		} else {
			log.Printf("failed to connect to redis: %s\n", err.Error())

			if newClient != nil {
				newClient.Close()
			}

			if spentTime > waitTime {
				return nil
			}

			time.Sleep(InitCheckInterval)
			spentTime += InitCheckInterval
		}
	}

	return nil
}
