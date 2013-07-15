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
            [zolo.utils.calendar :as zolo-cal]
            [clojure.tools.cli :as cli]))

(def RANDOM-PROCESS-ID (java.util.UUID/randomUUID))

(def PROCESS-COUNTER (atom 0))

(defn status [request-params]
  {:status 200
   :body {:working true}})

(defn not-ignore-logging? [request]
  (nil? (#{"/server/status"} (:uri request))))

(defroutes APP-ROUTES
  (POST "/classify" [emails :as {params :params}] (server/classify emails))

  ;;GENERAL
  (GET "/server/status" {params :params} (status params)))

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
      (web/wrap-request-logging not-ignore-logging?
                                logging-context
                                #(-> %
                                     (dissoc :params)
                                     (assoc :json-params "LOTS_OF_EMAILS"))
                                #(assoc % :body "EMAILS_SCORES") 
       (web/wrap-error-handling
        (web/wrap-jsonify
         APP-ROUTES))))))))

(defn start-pento
  ([]
     (start-pento 8000))
  ([port]
     (run-jetty (var app) {:port port :join? false})))


(defn process-args [args]
  (cli/cli args
           ["-p" "--port" "Listen on this port" :default 8000  :parse-fn #(Integer. %)] 
           ["-h" "--help" "Show help" :default false :flag true]))

(defn -main [& cl-args]
  (print-vals "CL Args :" cl-args)
  (let [[options args banner] (process-args cl-args)]
    (when (:help options)
      (println banner)
      (System/exit 0))
    (start-pento (:port options))))
