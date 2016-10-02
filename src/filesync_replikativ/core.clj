(ns filesync-replikativ.core
  (:gen-class)
  (:require [replikativ.crdt.cdvcs.stage :as cs]
            [filesync-replikativ.filesystem :refer :all]
            [clojure.java.io :as io]
            [clojure.data :refer [diff]]
            [konserve.core :as k]
            [konserve.filestore :refer [new-fs-store]]
            [replikativ.peer :refer [client-peer]]
            [replikativ.crdt.cdvcs.realize :as r]
            [replikativ.stage :refer [connect! create-stage!]]
            [hasch.core :refer [uuid]]
            [full.async :refer [<?? go-try go-loop-try <?]]
            [clojure.core.async :refer [go <!! >!! chan go-loop timeout] :as async])
  (:import [java.util Date]))

;; 1. list all files
;; how to deal with special files? folder/symlinks etc.?
;; 2. calculate reasonable delta
;; 3. store delta
;; 4. restore delta(s) into separate folder


(defn -main [config-path]
  (let [{:keys [sync-path store-path user cdvcs-id remote] :as config}
        (read-string (slurp config-path))
        _ (prn "Syncing folder with config:" config)
        _ (def store (<?? (new-fs-store store-path)))
        _ (def peer (<?? (client-peer store)))
        _ (def stage (<?? (create-stage! user peer)))
        _ (<?? (cs/create-cdvcs! stage :id cdvcs-id))
        c (chan)]
    (def sync-in-loop (r/stream-into-identity! stage [user cdvcs-id]
                                               eval-fs-fns
                                               sync-path
                                               ;; do not re-sync on startup
                                               ;; :applied-log [sync-path :in-loop]
                                               ))
    (connect! stage remote)
    (def sync-out-loop
      (go-loop-try [before (or (<? (k/get-in store [[sync-path :stored]]))
                               {})]
                   (<? (timeout 10000))
                   (let [after (list-dir sync-path)]
                     (let [txs (->> (delta before after)
                                    add-blobs-to-deltas
                                    (relative-paths sync-path))]
                       (when-not (empty? txs)
                         (prn "New txs:" txs)
                         (<? (cs/transact! stage [user cdvcs-id] txs))
                         (<? (k/assoc-in store [[sync-path :stored]] after))))
                     (recur after))))
    ;; HACK block main thread
    (<?? c)))


(comment
  (-main "resources/example-config.edn")


  (get-in @stage ["mail:whilo@topiq.es" #uuid "34db9ec4-82bf-4c61-8e2a-a86294f0e6d4" :state])

  (<?? (r/head-value store eval-fs-fns (get-in @stage ["mail:whilo@topiq.es" #uuid "34db9ec4-82bf-4c61-8e2a-a86294f0e6d4" :state])))

  )

