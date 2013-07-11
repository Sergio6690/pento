(ns zolo.pento.app
  (:use zolo.utils.debug
        compojure.core
        ring.adapter.jetty
        ring.middleware.json-params)
  (:require [zolo.pento.server :as server]
            [ring.middleware.params :as params-mw]
            [ring.middleware.keyword-params :as kw-params-mw]            
            [ring.middleware.json-params :as json-params]
            [ring.middleware.nested-params :as nested-params-mw]
            [zolo.utils.web :as web]))

(defroutes APP-ROUTES
  (POST "/classify" [emails :as {params :params}] (server/classify emails)))

(def app
  (params-mw/wrap-params
   (nested-params-mw/wrap-nested-params
    (json-params/wrap-json-params
     (kw-params-mw/wrap-keyword-params
      (web/wrap-request-logging
       (web/wrap-error-handling
        (web/wrap-jsonify
         APP-ROUTES))))))))