(set-env!
 :source-paths #{"src/clj" "src/cljs" "src/cljc"}
 :resource-paths #{"resources/html"}

 :dependencies '[
                 [org.clojure/clojure "1.8.0"]         ;; add CLJ
                 [org.clojure/clojurescript "1.9.89"] ;; .89 works.  trying 216.  add CLJS, but not 1.9.93
                 [adzerk/boot-cljs "1.7.228-1"]
                 [pandeiro/boot-http "0.7.3"]
                 [adzerk/boot-reload "0.4.12"]
                 [adzerk/boot-cljs-repl "0.3.3"]       ;; add bREPL
                 [com.cemerick/piggieback "0.2.1"]
                 [weasel "0.7.0"]
                 [org.clojure/tools.nrepl "0.2.12"]
                 [adzerk/boot-test "1.1.2"]
                 [crisptrutski/boot-cljs-test "0.2.1"]
                 [compojure "1.5.1"]             ;; for routing
                 ;; probably unnecessary, but debugging a java crash
                 [javax.servlet/servlet-api "3.0-alpha-1"]
                 [org.clojars.magomimmo/shoreleave-remote-ring "0.3.1"]
                 [org.clojars.magomimmo/shoreleave-remote "0.3.1"]
                 [org.clojars.magomimmo/valip "0.4.0-SNAPSHOT"]
                 [enlive "1.1.6"]
                 ;; clojurescript stuff
                 ;; using re-frame and reagent
                 [re-frame "0.8.0"]
                 [reagent "0.6.0-rc"]
                 [re-com "0.8.3"]
                 ;; leftovers from modern-clojurescript tutorial
                 [org.clojars.magomimmo/domina "2.0.0-SNAPSHOT"]
                 [hiccups "0.3.0"]
                 [cljsjs/remarkable "1.6.2-0"]
                 ;;[cljsjs/leaflet "0.7.7-4"] ;; not sure I want this
                 [cljsjs/d3 "4.2.0-0"]
                 [cljsjs/topojson "1.6.18-0"]
                 ])

(require '[adzerk.boot-cljs :refer [cljs]]
         '[pandeiro.boot-http :refer [serve]]
         '[adzerk.boot-reload :refer [reload]]
         '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
         '[adzerk.boot-test :refer [test]]
         '[crisptrutski.boot-cljs-test :refer [test-cljs]]
         )

(deftask dev
  "Launch immediate feedback dev environment"
  []
  (comp
   (serve :handler 'calvad.core/app           ;; ring handler
          :resource-root "target"                      ;; root classpath
          :reload true)                                ;; reload ns
   (watch)
   (reload)
   (cljs-repl) ;; before cljs
   (cljs)
   (target :dir #{"target"})))
