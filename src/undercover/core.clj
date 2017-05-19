(ns undercover.core
  (:require [clojure.string :as str]
            [compojure.route]
            [compojure.handler]
            [compojure.core]
            [undercover.server :as srv]
            [org.httpkit.server :as hk])
  (:gen-class))


;;
;; Routes for the application
;; 
(compojure.core/defroutes all-routes
  (compojure.core/GET "/chat" [] srv/chat-handler)      ;; websocket
  (compojure.route/resources "/") ;; static file url prefix /, in `public` folder
  (compojure.route/not-found "<p>Whoopsie! The page you are looking for is not here.</p>"))


(defn start-server
  "Starts the server with the given option map, returns a function that can be used to stop it"
  [options]
  (hk/run-server (compojure.handler/site #'all-routes) options))

;;
;; Main
;; TODO make port and ip binding configurable
;; TODO find a proper way to shut it down from the outside
;; 
(defn -main
  "Starts the undercover chatserver"
  [& args]
  (start-server {
      :port 8349 
      :ip "0.0.0.0" }))
