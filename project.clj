(defproject undercover "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [http-kit "2.3.0-alpha2"]
                 [compojure "1.5.2"]
                 [javax.servlet/servlet-api "2.5"]]
  :main ^:skip-aot undercover.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
