package utils

import (
	"context"
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
			log.Println(http.ListenAndServe("localhost:6060", nil))
		}()
	}
}

func getSessionStore(w http.ResponseWriter, r *http.Request) session.Store {
	sessionStore, err := session.Start(context.Background(), w, r)
	if err != nil {
		log.Print("[NAN]", err)
	}
	return sessionStore
}

// ■　参考
func ref1(w http.ResponseWriter, r *http.Request) {
	sessionStore := getSessionStore(w, r)

	sessionStore.Set("user_id", "user.ID")
	err := sessionStore.Save()

	if err != nil {
		log.Println("[NAN]", err)
		return
	}

	userID, ok := sessionStore.Get("user_id")
	if !ok {
		log.Println("[NAN]", err, userID)
	}
	//
}

// ■　参考
//func getUser(w http.ResponseWriter, r *http.Request) (user User, errCode int, errMsg string) {
//	var dbx = utils.Dbx
//
//	sessionstore := getSessionStore(w, r)
//
//	// userID, ok := session.Values["user_id"]
//	userID, ok := sessionstore.Get("user_id")
//	if !ok {
//		return user, http.StatusUnauthorized, "no session"
//	}
//
//	err := dbx.Get(&user, "SELECT * FROM `users` WHERE `id` = ?", userID)
//	if err == sql.ErrNoRows {
//		return user, http.StatusUnauthorized, "user not found"
//	}
//	if err != nil {
//		log.Print(err)
//		return user, http.StatusInternalServerError, "db error"
//	}
//
//	return user, http.StatusOK, ""
//}
