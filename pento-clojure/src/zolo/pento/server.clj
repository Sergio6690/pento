(ns zolo.pento.server
  (:use zolo.utils.debug
        zolo.utils.clojure)
  (:require [zolo.pento.feature-extraction :as fex]
            [clojure.data.json :as json]
            [zolo.utils.logger :as logger]))

(defn classify-each [{:keys [email name received_count sent_count] :as email-info}]  
  {email (try (fex/classify email sent_count received_count name)
              (catch Exception e
                (logger/error e (str "Pento error processing" email-info))
                (float 0.0)))})

(defn classify-batch [batch]
  (map classify-each batch))

(defn classify-emails [email-seq]
  (->> email-seq
       (partition-all 1000)
       (pmapcat classify-batch)
       (apply merge)))

(defn classify [emails]
  {:body (classify-emails emails)})
