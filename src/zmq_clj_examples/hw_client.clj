(ns zmq_clj_examples.zmq.hw-client
  (require [com.keminglabs.zmq-async.core :refer [register-socket!]]
           [clojure.core.async :refer [>! <! >!! <!! go thread chan sliding-buffer close!]]))


(def  ^:private c-in (chan (sliding-buffer 64)))
(def  ^:private c-out (chan (sliding-buffer 64)))


(defn zmq-client []
(register-socket! {:in c-in :out c-out :socket-type :req
                   :configurator (fn [socket]
                                   (.connect socket "tcp://127.0.0.1:5555"))})

(thread  (dotimes [count 5]
          (>!! c-in (str " To " count))
          (println  (str " Received " (String.  (<!! c-out))))
          )))
