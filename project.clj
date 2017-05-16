(defproject undercover "0.2.0-SNAPSHOT"
  :description "A silly chat server over websockets"
  :url "http://github.com/marco-nicolini/frothy"
  :license {:name "MIT License"
            :url "http://open-source-licenses.org/MIT"}
  :dependencies [;;[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojure "1.9.0-alpha16"]
                 [dire "0.5.4"]
                 [http-kit "2.3.0-alpha2"]
                 [compojure "1.5.2"]
                 [org.clojure/data.json "0.2.6"]
                 [javax.servlet/servlet-api "2.5"]]
  :main ^:skip-aot undercover.core
  :target-path "target/%s"
  :profiles {
    :uberjar {:aot :all}
    :dev {
      :dependencies [[manifold "0.1.6"] ;; for thread safe streams/queues used in tests.
                     [stylefruits/gniazdo "1.0.0"]]} ;; for ws test clients
  })
