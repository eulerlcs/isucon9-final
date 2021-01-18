# ISUCON9 本戦問題  with java

covert from go by eulerlcs at 2021/01/01

## run at local windows

```bat
cd isucon9-final
mvn -f webapp\java clean package -Dmaven.test.skip=true & docker-compose -f webapp\java\docker\docker-compose.yml build

docker-compose -f webapp\java\docker\docker-compose.yml up -d
mysql -u isutrain -p isutrain -h 127.0.0.1 -P 13306
```

## payment api

### /card

事前にAWS上やlocalでpayment servieを立ち上げること。

```bash
curl -i -H "Content-Type: application/json" -d "{\"card_information\":{\"card_number\":\"11111111\",\"cvv\":\"111\",\"expiry_date\":\"11/22\"}}" http://${aws_host_ip}:5000/card

{"card_token":"b26ee197-67bf-4f8a-6c47-9ed79838bcb9","is_ok":true}
```



## test api

### /initialize

```
curl -X POST http://127.0.0.1:8000/initialize
{"available_days":10,"language":"golang"}
```

### /api/settings

```
curl http://127.0.0.1:8000/api/settings
{"payment_api":"http://localhost:5000"}
```

### /api/stations

```
curl http://127.0.0.1:8000/api/stations
```

### /api/train/search

```
######## /api/train/search?use_at=<ISO8601形式の時刻>&from=東京&to=大阪&adult=3&child=1

curl "http://127.0.0.1:8000/api/train/search?use_at=2020-01-01T21:10:00.000Z&from=%E6%9D%B1%E4%BA%AC&to=%E5%A4%A7%E9%98%AA&train_class=%E6%9C%80%E9%80%9F&adult=3&child=1"
```

### api/train/seats

```
######## /api/train/seats?date=<ISO8601形式の時刻>&from=東京&to=大阪&train_class=最速&train_name=1&car_number=4

curl "http://127.0.0.1:8000/api/train/seats?date=2019-12-31T15:00:00.000Z&train_class=%E6%9C%80%E9%80%9F&train_name=1&car_number=4&from=%E6%9D%B1%E4%BA%AC&to=%E5%A4%A7%E9%98%AA"
```

### api/train/reserve

```bash
curl -i -H "Content-Type: application/json" -b "session_isutrain=F92FA80C1BFD01D659065D93E7F4DAAC" -d @data-reserve-全条件指定.json http://127.0.0.1:8000/api/train/reserve

curl -i -H "Content-Type: application/json" -b "session_isutrain=F92FA80C1BFD01D659065D93E7F4DAAC" -d @data-reserve-全条件非指定.json  http://127.0.0.1:8000/api/train/reserve

{"is_error":true,"message":"no session"}
```

#### data-reserve-全条件指定.json     utf-8

```json
{"date":"2019-12-31T15:00:00.000Z","train_class":"最速","train_name":"1","car_number":4,"seat_class":"reserved","departure":"東京","arrival":"大阪","child":0,"adult":1,"column":"","seats":[{"row":1,"column":"E","class":"reserved","is_smoking_seat":false,"is_occupied":false,"text":"○","disabled":false,"selected":true}]}
```

### api/train/reservation/commit

```bash
curl -i -H "Content-Type: application/json" -b "session_isutrain=F92FA80C1BFD01D659065D93E7F4DAAC" -d "{\"card_token\":\"161b2f8f-791b-4798-42a5-ca95339b852b\",\"reservation_id\":\"1\"}" http://127.0.0.1:8000/api/train/reservation/commit
```

### api/auth/signup

```bash
curl -i -H "Content-Type: application/json" -d "{\"email\":\"aaa@abc.com\",\"password\":\"123456\"}" http://127.0.0.1:8000/api/auth/signup
```

### api/auth/login

```
curl -i -H "Content-Type: application/json" -d "{\"email\":\"aaa@abc.com\",\"password\":\"123456\"}"  http://127.0.0.1:8000/api/auth/login
```

### api/auth

```bash
curl -i -b "session_isutrain=F92FA80C1BFD01D659065D93E7F4DAAC" http://127.0.0.1:8000/api/auth
```
### api/auth/logout

```
curl -X POST -i -b "session_isutrain=F92FA80C1BFD01D659065D93E7F4DAAC" http://127.0.0.1:8000/api/auth/logout
```
### api/user/reservations

```bash
curl -i -b "session_isutrain=F92FA80C1BFD01D659065D93E7F4DAAC" http://127.0.0.1:8000/api/user/reservations
```
### api/user/reservations/:item_id

```
curl -i -b "session_isutrain=F92FA80C1BFD01D659065D93E7F4DAAC" http://127.0.0.1:8000/api/user/reservations/2
```
### api/user/reservations/:item_id/cancel

```
curl -i -X POST -b "session_isutrain=F92FA80C1BFD01D659065D93E7F4DAAC" http://127.0.0.1:8000/api/user/reservations/2/cancel
```


