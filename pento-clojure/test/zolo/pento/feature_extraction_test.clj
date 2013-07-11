(ns zolo.pento.feature-extraction-test
  (:require [clojure.test :refer :all]
            [zolo.pento.feature-extraction :refer :all]))


(deftest test-cleaning-id 
  (testing "Stuff after + is removed in an email address"
    (is (= "karthik" (clean-id "karthik+test")))))


(deftest test-splitting-camelcase
  (testing "CamelCase should be changed to underscores"
    (is (= "karthik_kumara" (split-camel-case "karthikKumara")))
    (is (= "k_kumara" (split-camel-case "kKumara")))))

(deftest test-id-to-words 
  (testing "given id with multiple words with common delimiters, the words are cleanly seperated"
    (is (= ["karthik" "kumara"] (id-words "karthik_kumara")))
    (is (= ["karthik" "kumara"] (id-words "karthikKumara")))
    (is (= ["karthik" "kumara"] (id-words "karthik-kumara")))
    (is (= ["karthik" "kumara"] (id-words "karthik.kumara")))))

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

(deftest test-parsing-email
  (testing "Parsing of email works"
    (is (= {:id "karthik" :domain "gmail" :tld "com"} (parse-email "karthik+test@gmail.com")))
     (is (= {:id "karthik" :domain "gmail" :tld "com"} (parse-email "karthik@gmail.com")))))


(deftest test-features-with-name-word
  (testing "all features that concerns names or words"
    (is (= true (has-name {:id "karthikk"})))
    (is (= true (has-name {:id "kkarthik"})))
    (is (= true (has-name {:id "kkumara"})))
    (is (= true (has-word {:id "admin"})))
    (is (= true (has-word {:id "aadmin"})))
    (is (= true (has-word {:id "admins"})))
    (is (= true (are-all-names {:words ["karthik" "kumara"]})))
    (is (= false (are-all-names {:words ["karthik" "kumara" "admin"]})))
    (is (= true (has-any-name {:words ["karthik" "kumara" "admin"]})))
    (is (= false (has-any-name {:words ["special" "admin"]})))
    (is (= true (has-any-word {:words ["special" "admin"]})))
    (is (= true (are-all-words {:words ["special" "admin"]})))
    ))

(deftest features-test 
  (testing "rest of the features"
    (is (= true (is-group-email {:domain "googlegroups"})))
    (is (= true (is-common-email-host {:domain "gmail"})))
    (is (= true (is-org-edu-tld {:tld "edu" :domain "gmail"})))
    (is (= true (is-info-me-tld {:domain "googlegroups" :tld "info"})))
    (is (= true (domain-in-id-or-id-in-domain {:id "attuverse" :domain "att" :words ["attuverse" ]})))
))

(deftest test-input-to-features 
  (testing "input to features"
    (is (= {:id "karthik" :domain "mpg" :tld "de" :words ["karthik"] :email "karthik@kyb.tuebingen.mpg.de"} (get-feature-input  "karthik@kyb.tuebingen.mpg.de"  nil)))))

(deftest test-feature-extraction 
  (testing "complete feature extraction"
    (is (> (classify "karthik@kyb.tuebingen.mpg.de" 10 10 "Karthik Kumara") 0) )
    (is (< (classify "admin@amazon.com" 0 0 nil) 0) )
))
