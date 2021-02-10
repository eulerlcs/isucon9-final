package main

import (
	"fmt"
	goji "goji.io"
	"goji.io/pat"
	"log"
	"net/http"
	"sync"
	"zsj-isucon-09-final/dbCache"
	_ "zsj-isucon-09-final/dbCache"
	"zsj-isucon-09-final/dbCacheSiJian"
	"zsj-isucon-09-final/logic"
	"zsj-isucon-09-final/utils"
	// "sync"
)

var (
	banner = `ISUTRAIN API`
	wg     sync.WaitGroup
)

func main() {
	utils.PrepareServers()

	log.Println("ZSJ - init cache begin...")
	go dbCacheSiJian.InitCache()
	go dbCache.DoCacheAll()
	log.Println("ZSJ - init cache end.")

	routing()
}

func routing() {
	// HTTP

	mux := goji.NewMux()

	mux.HandleFunc(pat.Post("/initialize"), logic.InitializeHandler)
	mux.HandleFunc(pat.Get("/api/settings"), logic.SettingsHandler)

	// 予約関係
	mux.HandleFunc(pat.Get("/api/stations"), logic.GetStationsHandler)

	// N+1解消    劉　春生
	// mux.HandleFunc(pat.SelectByID("/api/train/search"), trainSearchHandler)
	mux.HandleFunc(pat.Get("/api/train/search"), logic.TrainSearchHandlerRyu01)

	mux.HandleFunc(pat.Get("/api/train/seats"), logic.TrainSeatsHandler)
	mux.HandleFunc(pat.Post("/api/train/reserve"), logic.TrainReservationHandler)
	mux.HandleFunc(pat.Post("/api/train/reservation/commit"), logic.ReservationPaymentHandler)

	// 認証関連
	mux.HandleFunc(pat.Get("/api/auth"), logic.GetAuthHandler)
	mux.HandleFunc(pat.Post("/api/auth/signup"), logic.SignUpHandler)
	mux.HandleFunc(pat.Post("/api/auth/login"), logic.LoginHandler)
	mux.HandleFunc(pat.Post("/api/auth/logout"), logic.LogoutHandler)
	mux.HandleFunc(pat.Get("/api/user/reservations"), logic.UserReservationsHandler)

	mux.HandleFunc(pat.Get("/api/user/reservations/:item_id"), logic.UserReservationResponseHandler)

	// 取消を非同期処理にする    司　剣
	//	mux.HandleFunc(pat.Post("/api/user/reservations/:item_id/cancel"), userReservationCancelHandler)
	// mux.HandleFunc(pat.Post("/api/user/reservations/:item_id/cancel"), userReservationCancelHandlerSiJian01)

	// 取消を非同期処理にする　　劉　春生
	//	mux.HandleFunc(pat.Post("/api/user/reservations/:item_id/cancel"), userReservationCancelHandler)
	mux.HandleFunc(pat.Post("/api/user/reservations/:item_id/cancel"), logic.UserReservationCancelHandlerRyu02)

	fmt.Println(banner)
	err := http.ListenAndServe(":8000", mux)

	log.Fatal(err)
}
