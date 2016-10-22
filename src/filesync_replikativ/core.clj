(ns filesync-replikativ.core
  (:gen-class)
  (:require [clojure.core.async :as async :refer [chan timeout]]
            [filesync-replikativ.filesystem :refer :all]
            [superv.async :refer [<? <?? go-loop-try S]]
            [konserve
             [core :as k]
             [filestore :refer [new-fs-store]]]
            [replikativ
             [peer :refer [client-peer]]
             [stage :refer [connect! create-stage!]]]
            [replikativ.crdt.cdvcs
             [realize :as r]
             [stage :as cs]]))

;; 1. list all files
;; how to deal with special files? folder/symlinks etc.?
;; 2. calculate reasonable delta
;; 3. store delta
;; 4. restore delta(s) into separate folder


(defn -main [config-path]
  (let [{:keys [sync-path store-path user cdvcs-id remotes] :as config}
        (read-string (slurp config-path))
        _ (prn "Syncing folder with config:" config)
        _ (def store (<?? S (new-fs-store store-path)))
        _ (def peer (<?? S (client-peer S store)))
        _ (def stage (<?? S (create-stage! user peer)))
        _ (<?? S (cs/create-cdvcs! stage :id cdvcs-id))
        c (chan)]
    (def sync-in-loop (r/stream-into-identity! stage [user cdvcs-id]
                                               eval-fs-fns
                                               sync-path
                                               ;; do not re-sync on startup
                                               ;; :applied-log [sync-path :in-loop]
                                               ))
    (doseq [r remotes] (connect! stage r))
    (def sync-out-loop
      (go-loop-try S
                   [before (or (<? S (k/get-in store [[sync-path :stored]]))
                               {})]
                   (<? S (timeout 10000))
                   (let [after (list-dir sync-path)]
                     (let [txs (->> (delta before after)
                                    add-blobs-to-deltas
                                    (relative-paths sync-path))]
                       (when (and (not (empty? txs))
                                  (> (- (.getTime (java.util.Date.))
                                        (.getTime (last-modified-time sync-path)))
                                     5000))
                         (prn "New txs:" txs)
                         (<? S (cs/transact! stage [user cdvcs-id] txs))
                         (<? S (k/assoc-in store [[sync-path :stored]] after))))
                     (recur after))))
    ;; HACK block main thread
    (<?? S c)))


(comment
  (-main "resources/example-config.edn")


  (get-in @stage ["mail:whilo@topiq.es" #uuid "34db9ec4-82bf-4c61-8e2a-a86294f0e6d4" :state])

  (let [{:keys [commit-graph heads]}
        (get-in @stage ["mail:whilo@topiq.es" #uuid "34db9ec4-82bf-4c61-8e2a-a86294f0e6d4" :state])]
    (<?? (r/commit-history-values store commit-graph (first heads))))

  )

