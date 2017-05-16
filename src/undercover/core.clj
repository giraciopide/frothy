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
        (srv/handle-channel-recv! channel-uuid channel data))))))

;;
;; Routes for the application
;; 
(compojure.core/defroutes all-routes
  (compojure.core/GET "/chat" [] chat-handler)      ;; websocket
  (compojure.route/resources "/") ;; static file url prefix /, in `public` folder
  (compojure.route/not-found "<p>Whoopsie! The page you are looking for is not here.</p>"))


(defn start-server
  "Starts the server with the given option map, returns a function that can be used to stop it"
  [options]
  (hk/run-server (compojure.handler/site #'all-routes) options))

;;
;; Main
;; TODO make port and ip binding configurable
(defn -main
  "Starts the undercover chatserver"
  [& args]
  (start-server {
      :port 8349 
      :ip "0.0.0.0" }))
