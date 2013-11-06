(ns zmq_clj_examples.wu-client
  (require [com.keminglabs.zmq-async.core :refer [register-socket!]]
           [cheshire.core  :as cc]
           [clojure.core.async :refer [>! <! >!! <!!
                                       go thread chan sliding-buffer close!]]))

(def  ^:private c-in (chan (sliding-buffer 64)))
(def  ^:private c-out (chan (sliding-buffer 64)))


(defn wu-client [zip-codes]
  ;; (if (nil? zip-codes) (conj zip-codes "") zip-codes)
  (register-socket! {:in c-in :out c-out :socket-type :sub
                     :configurator (fn [socket]
                                     ;;filter for events with given zip-codes
                                     (doall (map #(.subscribe socket (into-array Byte/TYPE (str %)))
                                                 zip-codes))
                                     (.connect socket "tcp://127.0.0.1:5544"))})
  (go (dotimes [count 100]
        (println count)
        (let [ msg (<! c-out)]
          (println  (str " Received: " (cc/parse-string
                                        (String. (second msg)) true)))))     
      (close! c-in)
      (close! c-out)
      ))

(defn keepalive []
    (.start (Thread. (fn [] (.join (Thread/currentThread))) "keepalive thread")))

(defn -main
  "Run the client and pass in filter arguments"
  [& args]
  (wu-client args)
  (keepalive))
