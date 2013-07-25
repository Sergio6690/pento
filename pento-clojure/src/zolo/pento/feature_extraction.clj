(ns zolo.pento.feature-extraction
  (:require [cheshire.core :as json])
  (:require [clojure.string :as str]))


(defn read-db-file [data-fl]
   (set (clojure.string/split-lines (slurp data-fl))))

(def all-words (read-db-file "data/allwords"))

(def all-names (read-db-file "data/allnames"))

(def group-emails (read-db-file "data/group_email_domains"))

(def common-email-domains (read-db-file "data/common_email_domains"))

(def soundex-encoder (new org.apache.commons.codec.language.Soundex))

(defn soundex [str]
  (.encode soundex-encoder str))

(def all-names-soundex (set (map soundex all-names)))

(defn is-in-index 
  ([indx, test]
     (is-in-index indx, test, false))
  ([indx, test, use-soundex]
     (let [test-str (if use-soundex (soundex test) test)]
       (not (= nil (get indx test-str))))))

(def segment)

(defn do-split [word dictionary index]
  (if (is-in-index dictionary (.substring word 0 index))
      (let [w1 (.substring word 0 index)
            w2 (.substring word index)
            w2splits (segment w2 dictionary)]
        (if (not-empty w2splits)
          (cons w1 w2splits)
          []))))

(defn segment [combined-word dictionary]
  (if (get dictionary combined-word) 
   [combined-word] 
    (let [splits (sort-by count
     (filter not-empty 
             (map (partial do-split combined-word dictionary) 
                  (range 1 (.length combined-word)))))]
      (if (> (count splits) 0) (first splits) []))))

(defn split-camel-case [name]
  (str/lower-case 
   (str/replace 
    (str/replace name #"(.)([A-Z][a-z]+)" "$1_$2") 
   #"([a-z0-9])([A-Z])" "$1_$2")))


(defn clean-id [id] 
  (first (str/split id #"\+")))

(defn split-on-char [char x] (flatten (map #(clojure.string/split % char) x)))

(def split-on-dash (partial split-on-char #"-"))

(def split-on-dot (partial split-on-char #"\."))

(def split-on-underscore (partial split-on-char #"_"))

(defn id-words [id]
  (let [src-str (split-camel-case id)] 
    (-> [src-str]
        split-on-underscore
        split-on-dot
        split-on-dash)))


(defn split-id [id]
  (cond (>  (count (id-words id)) 1) (id-words id)
        (>  (count (segment id all-words)) 0) (segment id all-words)
        (>  (count (segment id all-names)) 0) (segment id all-names)
        true [id]))


(defn -firstchar [s]
  (.substring s 1 ))

(defn -lastchar [s]
  (.substring s 0 (dec (.length s))))

(defn singular-version [s]
  (if (= \s (last s)) (-lastchar s) s))

; FEATURES


(defn has-name [{id :id}]
  (let [id (soundex id)](or (is-in-index all-names-soundex id)         
      (is-in-index all-names-soundex (.substring id 1))
      (is-in-index all-names-soundex (.substring id 0 (dec (.length id)))))))

(defn has-word [{id :id}]
  (let [singular (singular-version id)]
    (boolean (some (partial is-in-index all-words) 
          [id (-firstchar id) (-lastchar id) (-firstchar singular)]))))

(defn has-any-name [{words :words}]
  (boolean (some (partial is-in-index all-names) words)))

(defn are-all-names [{words :words}] 
  (every? (partial is-in-index all-names) words))

(defn has-any-word [{words :words}] 
  (boolean (some (partial is-in-index all-words) words)))

(defn are-all-words [{words :words}]
  (every? (partial is-in-index all-words) words))

(defn is-group-email [{domain :domain}]
  (is-in-index group-emails domain))

(defn is-common-email-host [{domain :domain}]
  (is-in-index common-email-domains domain))

(defn is-org-edu-tld [{tld :tld}]
  (or (= "org" tld) 
      (= "edu" tld)))

(defn is-info-me-tld [{tld :tld}]
  (or (= "info" tld)
      (= "me" tld)))

(defn domain-in-id-or-id-in-domain [{id :id domain :domain  words :words}]
  (boolean (or
   (some #{domain} (set words))
   (> (.indexOf domain id) -1)
   (> (.indexOf id domain) -1)
   (some #(> (.indexOf domain %) -1) words))))

(defn has-number-in-id [{id :id}]
  (boolean (re-find #"[0-9]" id)))

(defn has-subdomins [{email :email}]
  (> (count (str/split (last (str/split email #"@")) #"\.")) 2))

(defn clean-id [id]
  (first (str/split id #"\+")))

(defn parse-email [id]
  (let [splits (str/split id #"@")
        id (first splits)
        domain-splits (str/split (second splits) #"\.")
        tld (last domain-splits)
        domain (last (butlast domain-splits))
        ]
    {:id (clean-id id)
     :domain domain
     :tld tld}))

(defn log1+ [x]
  (java.lang.Math/log (+ 1 x)))

(defn get-feature-input [email name]
  (let [parts (parse-email email)
        words (if name (str/split (str/lower-case name) #" ") 
                  (id-words (:id parts)))]
    (merge parts {:words words :email email})))

(defn to-int [bool]
  (if bool 1 0))

#_(def weights {:has-name 1.085958,:has-word 1.472036e-04,:has-any-name 3.376846e-01,:are-all-names 4.140926e-01,:has-any-word -3.370337e-01,:are-all-words -5.085958e-01,:is-group-email -3.266195e+00,:is-common-email-host 1.662579e+00,:is-org-edu-tld 0.000000e+00,:domain-in-id-or-id-in-domain -3.379769e-01,:has-number-in-id -1.032386e-03,:has-subdomins 5.353531e-05,:sent 6.135196e-01,:recvd -2.246330e-04,:has-name-given 5.085958e-01, :intercept -1.016358})

(def weights { :intercept 0.736386460501176 :has-name 0.12012338097327 :has-word -0.372218346057325 :has-any-name 0.84251022530397 :are-all-names 0.58032225004523 :has-any-word -0.794262281276281 :are-all-words -2.11336613050202 :is-group-email -1.98665165879241 :is-common-email-host 1.15571782717454 :is-org-edu-tld -0.422957730208069 :domain-in-id-or-id-in-domain -0.765263404972649 :has-number-in-id -1.15565369304306 :has-subdomins -0.0981245126605303 :sent 0.674154081035754 :recvd -0.154445790801934 :has-name-given -0.0137261598988324})

(defn dot [v1 v2]
  (apply + (map * v1 v2)))

(defn get-features [email sent recd name]
  (if (= email name) (get-features email sent recd nil)
      (concat (map #(to-int (% (get-feature-input email name))) [has-name, has-word has-any-name are-all-names has-any-word are-all-words is-group-email is-common-email-host is-org-edu-tld domain-in-id-or-id-in-domain has-number-in-id has-subdomins]) [(log1+ sent) (log1+ recd) (if name 1 0) 1])))

(def feature-names [:has-name :has-word :has-any-name :are-all-names :has-any-word :are-all-words :is-group-email :is-common-email-host :is-org-edu-tld :domain-in-id-or-id-in-domain :has-number-in-id :has-subdomins :sent :recvd :has-name-given :intercept ])

(defn classify [email sent recd name] 
  (let [features (get-features email sent recd name)
        weights (map  weights feature-names)] 
    (dot features weights)))
        
; Feature extraction from a big json file

(defn features-from-json [{email "email"  name "name"  sent "sent_count" recd "received_count"}]
  (get-features email sent recd name))

(defn features-to-header []
  (str (str/join "\t" (concat (map name feature-names) ["y"])) "\n"))

(defn flip [f] (fn [a b] (f b a)))

(defn extract-features [in-file out-file y]
  (->> 
   in-file
   slurp
   str/split-lines
   (map json/parse-string)
   (map features-from-json)
   (map (partial (flip concat) [y]))
   (map (partial str/join "\t"))
   (str/join "\n")
   (spit out-file)))

(defn -main [in out y]
  (if (= in "header") (spit out (features-to-header))
  (extract-features in out y)))
