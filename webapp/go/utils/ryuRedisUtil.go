package utils

import (
	"github.com/go-redis/redis"
	sessionRedis "github.com/go-session/redis"
	"github.com/go-session/session"
	"log"
	"os"
	"time"
)

type REDIS struct {
	redisHost string
	redisPort string
}

var (
	Rdb                 *redis.Client
	sessionManagerStore session.ManagerStore
)

func (slf *REDIS) WaitOK() {
	if Rdb != nil {
		return
	} else {
		Rdb = slf.getRedisClient(1)
	}

	if sessionManagerStore != nil {
		return
	} else {
		sessionManagerStore = slf.getSessionManagerStore(15)
	}
}

func (slf *REDIS) getRedisClient(db int, timeout ...time.Duration) *redis.Client {
	slf.redisHost = os.Getenv("REDIS_HOST")
	if slf.redisHost == "" {
		slf.redisHost = "127.0.0.1"
	}

	slf.redisPort = os.Getenv("REDIS_PORT")
	if slf.redisPort == "" {
		slf.redisPort = "6379"
	}

	spentTime := 0 * time.Second
	waitTime := WaitTimeMax
	if len(timeout) > 0 {
		waitTime = timeout[0]
	}

	for {
		newClient := redis.NewClient(&redis.Options{
			Addr:     slf.redisHost + ":" + slf.redisPort,
			Password: "",
			DB:       db,
		})

		_, err := newClient.Ping().Result()
		if err == nil {
			log.Println("ZSJ - succeeded to connect to redis.")
			return newClient
		} else {
			log.Printf("ZSJ - failed to connect to redis: %s\n", err.Error())

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

func (slf *REDIS) getSessionManagerStore(db int) session.ManagerStore {
	store := sessionRedis.NewRedisStore(&sessionRedis.Options{
		Addr: slf.redisHost + ":" + slf.redisPort,
		DB:   db,
	})

	return store
}
