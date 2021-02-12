package utils

import (
	"log"
	"time"
)

const (
	InitCheckInterval = 500 * time.Millisecond
	WaitTimeMax       = 120 * time.Minute
)

func IsCached(field string) bool {
	pipe := Rdb.Pipeline()
	result := pipe.HSetNX("init:status", field, "1")
	pipe.Exec()

	ret, err := result.Result()
	if err == nil {
		return !ret
	} else {
		log.Printf("ZSJ - failed to connect to DB: %s.\n", err.Error())
		return false
	}
}
