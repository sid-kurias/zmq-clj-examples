(ns zmq_clj_examples.hw-server
  (require [com.keminglabs.zmq-async.core :refer [register-socket!]]
           [clojure.core.async :refer [>! <!  <!! >!! go thread chan sliding-buffer close!]]))


(def  ^:private s-in (chan (sliding-buffer 64)))
(def  ^:private s-out (chan (sliding-buffer 64)))

(defn keepalive []
  (.start (Thread. (fn [] (.join (Thread/currentThread))) "keepalive thread")))

(defn echo-server []
(register-socket! {:in s-in :out s-out :socket-type :rep
                   :configurator (fn [socket]
                                   (.bind socket "tcp://127.0.0.1:5555"))})
(thread
 (loop [msg (String. (<!! s-out))]
   (>!! s-in (str "From Server " msg))
   (println (str "Received " msg))
   (recur (String. (<!! s-out)))))
(println "Server"))
