package logic

import (
	"bytes"
	"database/sql"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"log"
	"net/http"
	"os"
	"strconv"
	"time"
	"zsj-isucon-09-final/domain"
	"zsj-isucon-09-final/utils"

	"goji.io/pat"
)

func UserReservationCancelHandler(w http.ResponseWriter, r *http.Request) {
	var dbx = utils.Dbx

	user, errCode, errMsg := getUser(w, r)
	if errCode != http.StatusOK {
		errorResponse(w, errCode, errMsg)
		return
	}
	itemIDStr := pat.Param(r, "item_id")
	itemID, err := strconv.ParseInt(itemIDStr, 10, 64)
	if err != nil || itemID <= 0 {
		errorResponse(w, http.StatusBadRequest, "incorrect item id")
		return
	}

	tx := dbx.MustBegin()

	reservation := domain.Reservation{}
	query := "SELECT * FROM reservations WHERE reservation_id=? AND user_id=?"
	err = tx.Get(&reservation, query, itemID, user.ID)
	fmt.Println("CANCEL", reservation, itemID, user.ID)
	if err == sql.ErrNoRows {
		tx.Rollback()
		errorResponse(w, http.StatusBadRequest, "reservations naiyo")
		return
	}
	if err != nil {
		tx.Rollback()
		errorResponse(w, http.StatusInternalServerError, "予約情報の検索に失敗しました")
	}

	switch reservation.Status {
	case "rejected":
		tx.Rollback()
		errorResponse(w, http.StatusInternalServerError, "何らかの理由により予約はRejected状態です")
		return
	case "done":
		// 支払いをキャンセルする
		payInfo := domain.CancelPaymentInformationRequest{reservation.PaymentId}
		j, err := json.Marshal(payInfo)
		if err != nil {
			tx.Rollback()
			errorResponse(w, http.StatusInternalServerError, "JSON Marshalに失敗しました")
			log.Println(err.Error())
			return
		}

		payment_api := os.Getenv("PAYMENT_API")
		if payment_api == "" {
			payment_api = "http://payment:5000"
		}

		client := &http.Client{Timeout: time.Duration(10) * time.Second}
		req, err := http.NewRequest("DELETE", payment_api+"/payment/"+reservation.PaymentId, bytes.NewBuffer(j))
		if err != nil {
			tx.Rollback()
			errorResponse(w, http.StatusInternalServerError, "HTTPリクエストの作成に失敗しました")
			log.Println(err.Error())
			return
		}
		resp, err := client.Do(req)
		if err != nil {
			tx.Rollback()
			errorResponse(w, resp.StatusCode, "HTTP DELETEに失敗しました")
			log.Println(err.Error())
			return
		}
		defer resp.Body.Close()

		// リクエスト失敗
		if resp.StatusCode != http.StatusOK {
			tx.Rollback()
			errorResponse(w, http.StatusInternalServerError, "決済のキャンセルに失敗しました")
			log.Println(resp.StatusCode)
			return
		}

		body, err := ioutil.ReadAll(resp.Body)
		if err != nil {
			tx.Rollback()
			errorResponse(w, http.StatusInternalServerError, "レスポンスの読み込みに失敗しました")
			log.Println(err.Error())
			return
		}

		// リクエスト取り出し
		output := domain.CancelPaymentInformationResponse{}
		err = json.Unmarshal(body, &output)
		if err != nil {
			tx.Rollback()
			errorResponse(w, http.StatusInternalServerError, "JSON parseに失敗しました")
			log.Println(err.Error())
			return
		}

		// zsj ryu 取消結果を判断する
		if !output.IsOk {
			tx.Rollback()
			errorResponse(w, http.StatusInternalServerError, "paymentの取消が失敗しました")
			return
		}

		fmt.Println(output)
	default:
		// pass(requesting状態のものはpayment_id無いので叩かない)
	}

	query = "DELETE FROM reservations WHERE reservation_id=? AND user_id=?"
	_, err = tx.Exec(query, itemID, user.ID)
	if err != nil {
		tx.Rollback()
		errorResponse(w, http.StatusInternalServerError, err.Error())
		return
	}

	query = "DELETE FROM seat_reservations WHERE reservation_id=?"
	_, err = tx.Exec(query, itemID)
	if err == sql.ErrNoRows {
		tx.Rollback()
		errorResponse(w, http.StatusInternalServerError, "seat naiyo")
		// errorResponse(w, http.Status, "authentication failed")
		return
	}
	if err != nil {
		tx.Rollback()
		errorResponse(w, http.StatusInternalServerError, err.Error())
		return
	}

	tx.Commit()
	messageResponse(w, "cancell complete")
}
