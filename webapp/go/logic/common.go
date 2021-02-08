package logic

import (
	"context"
	crand "crypto/rand"
	"database/sql"
	"encoding/json"
	"fmt"
	"github.com/go-session/session"
	"log"
	"net/http"
	"zsj-isucon-09-final/domain"
	"zsj-isucon-09-final/utils"
)

//func handler(w http.ResponseWriter, r *http.Request) {
//	fmt.Fprintf(w, "Hello, World")
//}

func messageResponse(w http.ResponseWriter, message string) {
	e := map[string]interface{}{
		"is_error": false,
		"message":  message,
	}
	errResp, _ := json.Marshal(e)
	w.Write(errResp)
}

func errorResponse(w http.ResponseWriter, errCode int, message string) {
	e := map[string]interface{}{
		"is_error": true,
		"message":  message,
	}
	errResp, _ := json.Marshal(e)

	w.WriteHeader(errCode)
	w.Write(errResp)
}

//func getSession(r *http.Request) *sessions.Session {
//	session, _ := store.Get(r, sessionName)
//
//	return session
//}

func getSession(w http.ResponseWriter, r *http.Request) session.Store {
	sessionstore, err := session.Start(context.Background(), w, r)
	if err != nil {
		log.Print("[NAN]", err)
	}
	return sessionstore
}

func getUser(w http.ResponseWriter, r *http.Request) (user domain.User, errCode int, errMsg string) {
	var dbx = (&utils.MYSQL{}).GetDB()
	defer dbx.Close()

	sessionstore := getSession(w, r)

	// userID, ok := session.Values["user_id"]
	userID, ok := sessionstore.Get("user_id")
	if !ok {
		return user, http.StatusUnauthorized, "no session"
	}

	err := dbx.Get(&user, "SELECT * FROM `users` WHERE `id` = ?", userID)
	if err == sql.ErrNoRows {
		return user, http.StatusUnauthorized, "user not found"
	}
	if err != nil {
		log.Print(err)
		return user, http.StatusInternalServerError, "db error"
	}

	return user, http.StatusOK, ""
}

func secureRandomStr(b int) string {
	k := make([]byte, b)
	if _, err := crand.Read(k); err != nil {
		panic(err)
	}
	return fmt.Sprintf("%x", k)
}
