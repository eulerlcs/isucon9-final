package logic

import (
	"bytes"
	crand "crypto/rand"
	"crypto/sha256"
	"database/sql"
	"encoding/json"
	"golang.org/x/crypto/pbkdf2"
	"io/ioutil"
	"log"
	"net/http"
	"zsj-isucon-09-final/domain"
	"zsj-isucon-09-final/utils"
)

func GetAuthHandler(w http.ResponseWriter, r *http.Request) {

	// userID取得
	user, errCode, errMsg := getUser(w, r)
	if errCode != http.StatusOK {
		errorResponse(w, errCode, errMsg)
		log.Printf("%s", errMsg)
		return
	}

	resp := domain.AuthResponse{user.Email}
	w.Header().Set("Content-Type", "application/json;charset=utf-8")
	json.NewEncoder(w).Encode(resp)
}

func SignUpHandler(w http.ResponseWriter, r *http.Request) {
	/*
		ユーザー登録
		POST /auth/signup
	*/
	var dbx = (&utils.MYSQL{}).GetDB()
	defer dbx.Close()

	defer r.Body.Close()
	buf, _ := ioutil.ReadAll(r.Body)

	user := domain.User{}
	json.Unmarshal(buf, &user)

	// TODO: validation

	salt := make([]byte, 1024)
	_, err := crand.Read(salt)
	if err != nil {
		errorResponse(w, http.StatusInternalServerError, "salt generator error")
		return
	}
	superSecurePassword := pbkdf2.Key([]byte(user.Password), salt, 100, 256, sha256.New)

	_, err = dbx.Exec(
		"INSERT INTO `users` (`email`, `salt`, `super_secure_password`) VALUES (?, ?, ?)",
		user.Email,
		salt,
		superSecurePassword,
	)
	if err != nil {
		errorResponse(w, http.StatusBadRequest, "user registration failed")
		return
	}

	messageResponse(w, "registration complete")
}

func LoginHandler(w http.ResponseWriter, r *http.Request) {
	/*
		ログイン
		POST /auth/login
	*/
	var dbx = (&utils.MYSQL{}).GetDB()
	defer dbx.Close()

	defer r.Body.Close()
	buf, _ := ioutil.ReadAll(r.Body)

	postUser := domain.User{}
	json.Unmarshal(buf, &postUser)

	user := domain.User{}
	query := "SELECT * FROM users WHERE email=?"
	err := dbx.Get(&user, query, postUser.Email)
	if err == sql.ErrNoRows {
		errorResponse(w, http.StatusForbidden, "authentication failed")
		return
	}
	if err != nil {
		errorResponse(w, http.StatusInternalServerError, err.Error())
		return
	}

	challengePassword := pbkdf2.Key([]byte(postUser.Password), user.Salt, 100, 256, sha256.New)

	if !bytes.Equal(user.HashedPassword, challengePassword) {
		errorResponse(w, http.StatusForbidden, "authentication failed")
		return
	}

	sessionstore := getSession(w, r)
	log.Println("[NAN]sessionstore=", sessionstore)
	log.Println("[NAN]user.ID=", user.ID)
	// session.Values["user_id"] = user.ID
	sessionstore.Set("user_id", user.ID)
	err = sessionstore.Save()
	// if err = session.Save(r, w); err != nil {
	if err != nil {
		log.Println("[NAN]", err)
		errorResponse(w, http.StatusInternalServerError, "session error")
		return
	}
	messageResponse(w, "autheticated")
}

func LogoutHandler(w http.ResponseWriter, r *http.Request) {
	/*
		ログアウト
		POST /auth/logout
	*/

	sessionstore := getSession(w, r)

	// session.Values["user_id"] = 0
	sessionstore.Set("user_id", 0)
	err := sessionstore.Save()
	//if err := session.Save(r, w); err != nil {
	if err != nil {
		log.Print(err)
		errorResponse(w, http.StatusInternalServerError, "session error")
		return
	}
	messageResponse(w, "logged out")
}

func dummyHandler(w http.ResponseWriter, r *http.Request) {
	messageResponse(w, "ok")
}
