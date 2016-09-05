(ns calvad.map.map
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as reagent ]
            [re-frame.core :refer [register-handler
                                   path
                                   register-sub
                                   dispatch
                                   dispatch-sync
                                   subscribe]]
            [clojure.string :as str]
            [cljsjs.topojson]
            [cljsjs.d3]))

(def app-state {:grid_shapes {}
                :grid_data   {}})

;; grid shapes is a list of features.  Each item in the list looks like:
;; {"type":"Feature",
;;  "geometry":{"type":"MultiPolygon",
;;              "coordinates":[[[[-115.74544,32.66887],[-115.78856,32.67102],
;;                               [-115.78602,32.70733],[-115.74287,32.70518],
;;                               [-115.74544,32.66887]]]]},
;;  "properties":{"gid":166,"cell_id":"8627","i_cell":"281.00000",
;;                "j_cell":"27.00000","fid_state4":8626,"id":"281.00000_27.00000"}}
;;
;; which explains why I have to strip the .000... out of the id.
;;
;;
;; grid_data is a list of data for the grid.  Or maybe a key value
;; map.  Not sure what is more efficient, so leaving a map for now.
;;
;; in my demo app, the hpms2009.json is an array of entries like this:
;;
;; {"sum_vmt":845,"sum_lane_miles":3.45066329770239,
;;  "sum_single_unit_mt":0,"sum_combination_mt":0,
;;  "road_type":"totals",
;;  "f_system":"totals",
;;  "cell_i":"100","cell_j":"226","year":"2009"}




;; map

(defn d3-inner-map [data active]
    (reagent/create-class
     {:reagent-render (fn [] [:div.map
                              [:svg {:width 500 :height 500}
                               ;; [:rect {:class "background"
                               ;;         :width 500
                               ;;         :height 500
                               ;;         }]
                               ;; translate to 0, height/3.  Not sure why
                               ;; [:g ;;{:transform  "translate(0,167)"}
                               ;;  ]
                               ]])
      :component-did-mount (fn []
                             (let [d3data (clj->js data)
                                   allgeoms (.-geometries  (.-grids (.-objects d3data)))
                                   land (.. js/topojson
                                            (feature d3data
                                                     (clj->js{:type "GeometryCollection"
                                                      :geometries (clj->js allgeoms)
                                                      })))
                                   geoPath (js/d3.geoPath.)
                                   gtm (js/d3.geoTransverseMercator.)
                                   path (.projection geoPath
                                         (.rotate gtm (clj->js [124 -32.5]))
                                         (.fitExtent gtm (clj->js [[20 20] [480 480]]) land))
                                   svg (.. js/d3
                                           (select ".map svg"))
                                   g (.append svg "g")
                                   zoomed (fn []
                                            (let [event (.-event js/d3)
                                                  transform (.-transform event)
                                                  x (str (.-x transform))
                                                  y (str (.-y transform))
                                                  k (.-k transform)
                                                  ;; make x y k strings
                                                  tr (str x  "," y)
                                                  ]
                                              (.. g
                                                  (style "stroke-width"
                                                         (str/join
                                                          (str (/ 1.5 k))
                                                          "px") )
                                                  (attr "transform"
                                                        (str
                                                         "translate ("
                                                         tr
                                                         ")scale("
                                                         (str k)
                                                         ")") )
                                                  )))
                                   zoom (.. (.. js/d3
                                                zoom.)

                                            (scaleExtent (clj->js [1 8]))
                                            (on "zoom" zoomed))
                                   feat (.-features land)
                                   ]
                               (.. svg
                                   (call zoom))

                               (.. g
                                   (selectAll "path")
                                   (data (clj->js feat))
                                   enter
                                   (append "path")
                                   (attr "class" "grid")
                                   (attr "d" path)
                                   ;;(append "title")
                                   ;;(text (fn [d] (.-id d)))
                                   )
                               (.. svg
                                   (append "path")
                                   (datum (.. js/topojson
                                              (mesh d3data
                                                    (.-grids (.-objects d3data))
                                                    (fn [a b] (not (= a b))
                                                    ))))
                                   (attr "class" "grid-border")
                                   (attr "d" path)
                                   )
                               ))

      :component-did-update (fn [this] ())
      }))
