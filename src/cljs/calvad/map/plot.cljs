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

;; mostly copying from my old work and from the brush and zoom example

(defn d3-inner-plot [data active]
      (reagent/create-class
       {:reagent-render (fn [] [:div.plot
                                [:svg {:width 500 :height 500}
                                 ]])
        :component-did-mount (fn [this] ())
        :component-did-update (fn [this] ())
        }))
