package logic

import (
	"encoding/json"
	"log"
	"net/http"
	"os"
	"zsj-isucon-09-final/domain"
)

func SettingsHandler(w http.ResponseWriter, r *http.Request) {
	payment_api := os.Getenv("PAYMENT_API")
	if payment_api == "" {
		payment_api = "http://localhost:5000"
	}

	log.Println("[NAN]payment_api=" + payment_api)

	settings := domain.Settings{
		PaymentAPI: payment_api,
	}

	w.Header().Set("Content-Type", "application/json;charset=utf-8")
	json.NewEncoder(w).Encode(settings)
}
