(ns filesync-replikativ.core-test
  (:require [clojure.test :refer :all]
            [filesync-replikativ.filesystem :refer :all]))


(deftest filesystem-routines
  (testing "Test filesystem routines."
    (let [before {"./test_folder" {:type :dir}}
          after {"./test_folder" {:type :dir},
                 "./test_folder/foo" {:type :dir},
                 "./test_folder/foo/bar" {:type :dir},
                 "./test_folder/foo/bar/baz"
                 {:type :file,
                  :last-modified-time #inst "2016-10-02T10:18:56.934-00:00"}}
          ds (delta before after)]
      (is (= (list-dir "./test_folder/") after))
      (is (= ds
             '([add-dir {:path "./test_folder/foo"}]
               [add-dir {:path "./test_folder/foo/bar"}]
               [add-file {:path "./test_folder/foo/bar/baz"}])))
      (is (= (-> (add-blobs-to-deltas ds) last second :content String.)
             "bazzz\n"))
    )))
