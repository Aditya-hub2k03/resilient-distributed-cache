Phase - I

1. Method : POST

   "http://localhost:8080/api/v1/cache"

   raw/json:

   {

     "key": "user:101",

     "value": "Aditya",

     "ttlSeconds": 60

   }

   O/P:

   {

     "success": true,

     "message": "Cache entry persisted successfully",

     "data": null,

     "timestamp": "2026-11-01T23:43:01.6314252"

   }

   Status : 200 OK

2. Method : GET

   "http://localhost:8080/api/v1/cache/stats"

   O/P:

   {

     "success": true,

     "message": "Telemetry stats fetched",

     "data": {

   ​    "hits": 0,

   ​    "misses": 4,

   ​    "evictions": 0,

   ​    "currentSize": 0,

   ​    "capacity": 500,

   ​    "hitRate": 0.0

     },

     "timestamp": "2026-11-01T23:53:13.6995739"

   }

   Status : 200 OK