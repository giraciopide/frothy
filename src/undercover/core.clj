(ns undercover.core
  (:require [clojure.string :as str]
            [compojure.route]
            [compojure.handler]
            [compojure.core]
            [undercover.server :as srv]
            [org.httpkit.server :as hk])
  (:gen-class))

(defn make-uuid [] (str (java.util.UUID/randomUUID)))

(defn chat-handler [request]
  (hk/with-channel request channel
    (let [channel-uuid (make-uuid)]
      (println "channel [" channel "] has been given uuid [" channel-uuid "]")
      (hk/on-close channel (fn [status] 
        (srv/handle-chan-closed! channel-uuid channel status)
        (println "channel " channel-uuid " closed: " status)))
      (hk/on-receive channel (fn [data] 
        (srv/handle-msg! channel-uuid channel (srv/make-msg data)))))))

    
;;
;; Routes for the application
;; 
(compojure.core/defroutes all-routes
  (compojure.core/GET "/chat" [] chat-handler)      ;; websocket
  (compojure.route/resources "/") ;; static file url prefix /, in `public` folder
  (compojure.route/not-found "<p>Whoopsie! The page you are looking for is not here.</p>"))

;;
;; Main
;; TODO make port and ip binding configurable
(defn -main
  "Starts the undercover chatserver"
  [& args]
  (hk/run-server (compojure.handler/site #'all-routes) {:port 8349 :ip "0.0.0.0"}))
