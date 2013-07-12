(ns zolo.pento.app
  (:gen-class)
  (:use zolo.utils.debug
        compojure.core
        ring.adapter.jetty
        ring.middleware.json-params)
  (:require [zolo.pento.server :as server]
            [ring.middleware.params :as params-mw]
            [ring.middleware.keyword-params :as kw-params-mw]            
            [ring.middleware.json-params :as json-params]
            [ring.middleware.nested-params :as nested-params-mw]
            [zolo.utils.web :as web]
            [zolo.utils.calendar :as zolo-cal]))

(def RANDOM-PROCESS-ID (java.util.UUID/randomUUID))

(def PROCESS-COUNTER (atom 0))

(defroutes APP-ROUTES
  (POST "/classify" [emails :as {params :params}] (server/classify emails)))

(defn trace-id [request]
  (str ".env-producton" 
       ".h-" (:host request)
       ".rh-" RANDOM-PROCESS-ID
       ".c-" @PROCESS-COUNTER
       ".ts-" (zolo-cal/now)  
       ".v-" (or (.get (System/getenv) "GIT_HEAD_SHA") "GIT-SHA-NOT-SET")))

(defn logging-context [request]
  (swap! PROCESS-COUNTER inc)
  (merge
   (select-keys request [:request-method :query-string :uri :server-name])
   {:trace-id (trace-id request)
    :env "production"
    :facility "pento"
    :ip-address (get-in request [:headers "x-real-ip"])}))

(def app
  (params-mw/wrap-params
   (nested-params-mw/wrap-nested-params
    (json-params/wrap-json-params
     (kw-params-mw/wrap-keyword-params
      (web/wrap-request-logging (constantly true) logging-context
       (web/wrap-error-handling
        (web/wrap-jsonify
         APP-ROUTES))))))))

(defn start-pento
  ([]
     (start-pento 8000))
  ([port]
     (run-jetty (var app) {:port port :join? false})))