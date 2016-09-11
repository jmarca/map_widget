(ns calvad.map.letters
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as reagent ]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [calvad.map.events]
            [calvad.map.subs]
            [calvad.map.views]
            ))


(enable-console-print!)
(println "Edits to this text should show up in your developer console.")

;; (def alphabet (rest (str/split "abcdefghijklmnopqrstuvwxyz" #"")))



(defn ^:export main []
  (let []
  (dispatch-sync [:initialize-db])
  (reagent/render-component [calvad.map.views/app]
                            (. js/document (getElementById "app")))
           ))


(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
