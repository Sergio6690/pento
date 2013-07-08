(ns pento-clojure.feature-extraction-test
  (:require [clojure.test :refer :all]
            [pento-clojure.feature-extraction :refer :all]))


(deftest test-cleaning-id 
  (testing "Stuff after + is removed in an email address"
    (is (= "karthik" (clean-id "karthik+test")))))


(deftest test-splitting-camelcase
  (testing "CamelCase should be changed to underscores"
    (is (= "karthik_kumara" (split-camel-case "karthikKumara")))
    (is (= "k_kumara" (split-camel-case "kKumara")))))

(deftest test-id-to-words 
  (testing "given id with multiple words with common delimiters, the words are cleanly seperated"
    (is (= ["karthik" "kumara"] (id_words "karthik_kumara")))
    (is (= ["karthik" "kumara"] (id_words "karthikKumara")))
    (is (= ["karthik" "kumara"] (id_words "karthik-kumara")))
    (is (= ["karthik" "kumara"] (id_words "karthik.kumara")))))

(deftest test-index-lookup
  (testing "to see if the index lookup works"
    (is (= true (is-in-index {"a" "b"} "a")))
    (is (= false (is-in-index {"a" "b"} "c")))))

(deftest test-segmenting-words
  (testing "Segmentation works"
    (is (= ["karthik" "kumara"] (segment "karthikkumara" {"karthik" 1 "kumara" 2})))
    (is (= ["karthik" "kumar" "a"] (segment "karthikkumara" {"karthik" 1 "kumar" 2 "a" 3})))
    (is (= [] (segment "karthikkumara" {})))
    (is (= ["karthik" "kumara"] (segment "karthikkumara" (set ["karthik"  "kumara" "a" "kumar"]))))))


(deftest test-soudex-equivalency
  (testing "soundex works as expected"
    (is (= (soundex "john") (soundex "jhon")))
    (is (= (soundex "mary") (soundex "marie")))
    (is (= (soundex "stefan") (soundex "steven")))
    (is (not= (soundex "steve") (soundex "stacy")))))


(deftest test-complete-splitting
  (testing "ids get split as expected"
    (is (= ["amit" "rathore"]  (split-id "amit.rathore")))
    (is (= ["siva"  "jag"]  (split-id "SivaJag")))
    (is (= ["karthik" "kumara"]  (split-id "karthik_kumara")))
    (is (= ["order" "update"]  (split-id "order-update")))
    (is (= ["amit" "rathore"]  (split-id "amitrathore")))
    (is (= ["siva"  "jag"]  (split-id "sivajag")))
    (is (= ["karthik" "kumara"]  (split-id "karthikkumara")))
    (is (= ["order" "update"]  (split-id "orderupdate")))
    ))
