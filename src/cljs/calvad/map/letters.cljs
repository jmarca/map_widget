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


(def margin [20 20 20 20]) ;; like css, start at the top
(def width 500)
(def height 500)


(defn ^:export main [json-file]
  (dispatch-sync [:initialize-db])
  (.json js/d3 json-file
         (fn [error json]
           (if (not (nil? error))
             (println "error in json" error)
             (dispatch [:process-topojson json {:margin margin
                                                :width width
                                                :height height}]))))
  (reagent/render-component [calvad.map.views/app width height]
                            (. js/document (getElementById "app")))
  )



;; then call grid with the grid data
;; but better, use the grid data to populate the db
;;            [calvad.map.map/d3-inner-map
;;             gridtopo
;;             active
;;               ]]


(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
