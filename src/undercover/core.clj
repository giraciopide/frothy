(ns undercover.core
  (:require [clojure.string :as str]
            [compojure.route]
            [compojure.handler]
            [compojure.core]
            [org.httpkit.server :as http-kit])
  (:gen-class))

(defn chat-handler [request]
  (http-kit/with-channel request channel
    (http-kit/on-close channel (fn [status] (println "channel closed: " status)))
    (http-kit/on-receive channel (fn [data] ;; echo it back
                          (http-kit/send! channel data)))))


(compojure.core/defroutes all-routes
  (compojure.core/GET "/ws" [] chat-handler)      ;; websocket
  (compojure.route/resources "/") ;; static file url prefix /, in `public` folder
  (compojure.route/not-found "<p>Whoopsie! The page you are looking for is not here.</p>"))

(defn -main
  "Starts the server"
  [& args]
  (http-kit/run-server (compojure.handler/site #'all-routes) {:port 8080}))
