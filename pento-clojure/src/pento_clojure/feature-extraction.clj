(ns pento-clojure.feature-extraction
  (:require [clojure.string :as str]))



(defn read-db-file [data-fl]
   (set (clojure.string/split-lines (slurp data-fl))))

(def all-words (read-db-file "data/allwords"))

(def all-names (read-db-file "data/allnames"))

(def soundex-encoder (new org.apache.commons.codec.language.Soundex))

(defn soundex [str]
  (.encode soundex-encoder str))

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


; FEATURES






