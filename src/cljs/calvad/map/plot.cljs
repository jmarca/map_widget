(ns calvad.map.plot
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as reagent ]
            [re-frame.core :refer [register-handler
                                   path
                                   register-sub
                                   dispatch
                                   dispatch-sync
                                   subscribe]]
            [clojure.string :as str]
            [cljsjs.d3]
            ))
(enable-console-print!)
(println "hello from calvad.map.plot.  Loading the code")

(def app-state {:grid_data []})
