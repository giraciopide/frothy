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
        (println "channel " channel-uuid "received data" (str data))
        (srv/handle-msg! channel-uuid channel (srv/make-msg data)))))))
        ;; (hk/send! channel data)

    
;;
;; Routes for the application
;; 
(compojure.core/defroutes all-routes
  (compojure.core/GET "/ws" [] chat-handler)      ;; websocket
  (compojure.route/resources "/") ;; static file url prefix /, in `public` folder
  (compojure.route/not-found "<p>Whoopsie! The page you are looking for is not here.</p>"))

;;
;; Main
;; 
(defn -main
  "Starts the undercover chatserver"
  [& args]
  (hk/run-server (compojure.handler/site #'all-routes) {:port 8080}))
