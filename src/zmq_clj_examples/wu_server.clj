(ns zmq_clj_examples.zmq.wu-server
  (import [ java.io.Console ])
  (require [com.keminglabs.zmq-async.core :refer [register-socket!]]
           [cheshire.core  :as cc]
           [clojure.core.async :refer [>! <!  <!! >!! timeout
                                       go thread chan sliding-buffer close!]]))


(def  ^:private s-in (chan (sliding-buffer 64)))
(def  ^:private s-out (chan (sliding-buffer 64)))

(defn register-server
  "Registers to listen on a given port"
  []
  (register-socket! {:in s-in :out s-out :socket-type :pub
                     :configurator (fn [socket]
                                     (.bind socket "tcp://127.0.0.1:5544"))}))
(defn wu-server []
  (register-server)
  (go (while :true
        (let [rn (rand) zip (int (* rn 1000)) temp (- (* rn 215) 80)
              hum (+ 10 (* rn 50))]
          (<!! (timeout 10))
          (>!! s-in [(str zip) (cc/generate-string {:zip zip
                                                    :temp temp
                                                    :hum hum})])))))

(defn keepalive []
  (.start (Thread. (fn [] (.join (Thread/currentThread))) "keepalive thread")))

(defn -main
  "Run the server"
  []
  (wu-server)
  (keepalive)
  (println "Weather Service started"))
