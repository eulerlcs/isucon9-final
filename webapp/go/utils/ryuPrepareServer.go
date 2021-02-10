package utils

import (
	"github.com/go-session/session"
	"log"
	"net/http"
	_ "net/http/pprof"
	"os"
)

func PrepareServers() {
	// 性能モニターを起動する
	ppf()

	// mysql、redisの順で起動を確認する
	(&MYSQL{}).WaitOK()
	(&REDIS{}).WaitOK()

	redisStore15 := (&REDIS{}).GetRedisStore(15)
	session.InitManager(
		session.SetStore(redisStore15),
	)
}

// 性能モニターを起動する
func ppf() {
	if os.Getenv("PPROF") == "true" {
		go func() {
			log.Println("ZSJ - PPROF=true")
			log.Println(http.ListenAndServe(":6060", nil))
		}()
	}
}
