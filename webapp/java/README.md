# ISUCON9 本戦問題  with java

covert from go

## run at local

```bat
cd isucon9-final\webapp\java
mvn clean compile jib:dockerBuild
cd docker
docker-compose up -d

mysql -u isutrain -p isutrain -h 127.0.0.1 -P 13306
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
[
    {
        "id": 1,
        "name": "東京",
        "is_stop_express": true,
        "is_stop_semi_express": true,
        "is_stop_local": true
    },
    {
        "id": 2,
        "name": "古岡",
        "is_stop_express": false,
        "is_stop_semi_express": true,
        "is_stop_local": true
    }
]
```

### /api/train/search

```
######## /api/train/search?use_at=<ISO8601形式の時刻>&from=東京&to=大阪&adult=3&child=1
curl "http://127.0.0.1:8000/api/train/search?use_at=2020-01-01T21:10:00.000Z&from=%E6%9D%B1%E4%BA%AC&to=%E5%A4%A7%E9%98%AA&train_class=%E6%9C%80%E9%80%9F&adult=3&child=1"
[
    {
        "train_class": "最速",
        "train_name": "43",
        "start": "東京",
        "last": "大阪",
        "departure": "東京",
        "arrival": "大阪",
        "departure_time": "09:35",
        "arrival_time": "11:44:59",
        "seat_availability": {
            "reserved_smoke": "○",
            "non_reserved": "○",
            "premium_smoke": "×",
            "premium": "○",
            "reserved": "○"
        },
        "fare": {
            "reserved_smoke": 34996,
            "non_reserved": 34996,
            "premium_smoke": 34996,
            "premium": 34996,
            "reserved": 34996
        }
    },
    ### 他の９件省略
]
```

### api/train/seats

```
######## /api/train/seats?date=<ISO8601形式の時刻>&from=東京&to=大阪&train_class=最速&train_name=1&car_number=4

curl "http://127.0.0.1:8000/api/train/seats?date=2019-12-31T15:00:00.000Z&train_class=%E6%9C%80%E9%80%9F&train_name=1&car_number=4&from=%E6%9D%B1%E4%BA%AC&to=%E5%A4%A7%E9%98%AA"
```

### api/train/reserve

```
curl -i -H "Content-Type: application/json" -b "session_isutrain=23560778A6D69DED6A54AA2F298C9A64" -d @data-reserve-全条件指定.json http://127.0.0.1:8000/api/train/reserve

curl -i -H "Content-Type: application/json" -b "session_isutrain=23560778A6D69DED6A54AA2F298C9A64" -d @data-reserve-全条件非指定.json  http://127.0.0.1:8000/api/train/reserve

{"is_error":true,"message":"no session"}
```

#### data-reserve-全条件指定.json     utf-8

```json
{"date":"2019-12-31T15:00:00.000Z","train_class":"最速","train_name":"1","car_number":4,"seat_class":"reserved","departure":"東京","arrival":"大阪","child":0,"adult":1,"column":"","seats":[{"row":1,"column":"E","class":"reserved","is_smoking_seat":false,"is_occupied":false,"text":"○","disabled":false,"selected":true}]}
```

#### all in one --  git bash OK,  cmd OK

```
curl -X POST -H "Content-Type: application/json" -d "{\"date\":\"2019-12-31T15:00:00.000Z\",\"train_class\":\"\u6700\u901f\",\"train_name\":\"1\",\"car_number\":4,\"seat_class\":\"reserved\",\"departure\":\"\u6771\u4eac\",\"arrival\":\"\u5927\u962a\",\"child\":0,\"adult\":1,\"column\":\"\",\"seats\":[{\"row\":1,\"column\":\"E\",\"class\":\"reserved\",\"is_smoking_seat\":false,\"is_occupied\":false,\"text\":\"\u25cb\",\"disabled\":false,\"selected\":true}]}" http://127.0.0.1:8000/api/train/reserve
```

#### all in one   -- git bash OK,  cmd NG

```bash
curl -H "Content-Type: application/json" -d '{"date":"2019-12-31T15:00:00.000Z","train_class":"\u6700\u901f","train_name":"1","car_number":4,"seat_class":"reserved","departure":"\u6771\u4eac","arrival":"\u5927\u962a","child":0,"adult":1,"column":"","seats":[{"row":1,"column":"E","class":"reserved","is_smoking_seat":false,"is_occupied":false,"text":"\u25cb","disabled":false,"selected":true}]}' http://127.0.0.1:8000/api/train/reserve
```





### api/train/reservation/commit

```

```

### api/auth/login

```
curl -i -H "Content-Type: application/json" -d "{\"email\":\"ryu@abc.com\",\"password\":\"123456\"}"  http://127.0.0.1:8000/api/auth/login
```

```
HTTP/1.1 403
Content-Type: application/json
Transfer-Encoding: chunked
Date: Sun, 10 Jan 2021 04:35:28 GMT

{"is_error":true,"message":"authentication failed"}
```

```
HTTP/1.1 200
Set-Cookie: session_isutrain=A4ADB9DC5512DE3829AD8FE403DE981B; Path=/; HttpOnly
Content-Type: application/json
Transfer-Encoding: chunked
Date: Sun, 10 Jan 2021 04:36:01 GMT

{"is_error":false,"message":"autheticated"}
```



### api/auth

```bash
curl -i -b "session_isutrain=23560778A6D69DED6A54AA2F298C9A64" http://127.0.0.1:8000/api/auth
```
```
HTTP/1.1 200
Content-Type: application/json
Transfer-Encoding: chunked
Date: Sun, 10 Jan 2021 04:19:09 GMT

{"email":"ryu@abc.com"}
```



### api/auth/signup

```

```
### api/auth/logout

```

```
### api/user/reservations

```

```
### api/user/reservations/:item_id

```

```
### api/user/reservations/:item_id/cancel

```

```





## Author

- ryu - 2021/01/01