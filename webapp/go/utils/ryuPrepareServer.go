package utils

import (
	"github.com/go-session/session"
	"log"
	"net/http"
	_ "net/http/pprof"
)

func PrepareServers() {
	// 性能モニターを起動する
	ppf()

	// mysql、redisの順で起動を確認する
	(&MYSQL{}).WaitOK()
	(&REDIS{}).WaitOK()

	session.InitManager(
		session.SetStore(sessionManagerStore),
	)
}

// 性能モニターを起動する
func ppf() {
	go func() {
		log.Println("ZSJ - PPROF=true")
		log.Println(http.ListenAndServe(":6060", nil))
	}()
}
