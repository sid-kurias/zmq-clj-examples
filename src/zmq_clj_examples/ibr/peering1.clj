(ns zmq-clj-examples.ibr.peering1
  (import [ java.io.Console ])
  (require [com.keminglabs.zmq-async.core :refer [register-socket!]]
           [clojure.core.async :refer [>! <!  <!! >!! timeout alts!!
                                       go thread chan sliding-buffer close!]]))


(def  ^:private pub-in (chan (sliding-buffer 64)))
(def  ^:private pub-out (chan (sliding-buffer 64)))
(def  ^:private sub-in (chan (sliding-buffer 64)))
(def  ^:private sub-out (chan (sliding-buffer 64)))

(defn bind-publish
  "Registers to publish on port"
  [name]
  (register-socket! {:in pub-in :out pub-out :socket-type :pub
                     :configurator (fn [socket]
                                     (.bind socket (str "ipc://" name "-state.ipc")))}))

(defn subscribe
  "connects statefe to all peers"
  [peers]
  (register-socket! {:in sub-in :out sub-out :socket-type :sub
                     :configurator (fn [socket]
                                     (.subscribe socket (into-array Byte/TYPE ""))
                                     (doall (map  #(.connect socket (str "ipc://" % "-state.ipc"))
                                                  peers)))}))

(defn process-worker
  "Processes incoming state messages from peers"
  [val]
  (println (str (first val) " - " (second val) " workers free"))
  )

(defn- publish-status
  "Sends status to every peer"
  [my-name]
  (>!! pub-in [my-name (str (int (rand 10)))]))

(defn process-events
  "If there are incoming events will process them, else will send status"
  [my-name]
  (thread (while true
            (let [ [val source] (alts!! [sub-out  (timeout 1000)])]
              (if (identical? sub-out source)
                (println (str (String. (first val)) " - " (String. (second val)) " workers free"))
                (publish-status my-name))))))

(defn keepalive []
  (.start (Thread. (fn [] (.join (Thread/currentThread))) "keepalive thread")))

(defn -main
  "First argument is this brokers name. Other arguments are names of peers"
  [& names]
  {:pre [(> (count names) 1)]}
  (println "I: preparing broker at " (first names) " ...")
  (bind-publish (first names))
  (subscribe (rest names))
  (process-events (first names))
  (keepalive))
