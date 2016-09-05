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



;; letters using d3 controllers
(defn d3-inner-map [data]
    (reagent/create-class
     {:reagent-render (fn [] [:div
                              [:svg {:width 500 :height 500}
                               ;; translate to 0, height/3.  Not sure why
                               ;; [:g ;;{:transform  "translate(0,167)"}
                               ;;  ]
                               ]])
      :component-did-mount (fn []
                             (let [d3data (clj->js data)
                                   land (.. js/topojson
                                            (feature d3data
                                                     {:type "GeometryCollection"
                                                      :geometries
                                                      (:geometries
                                                        (:grids
                                                         (:objects d3data )))
                                                      }))
                                   path (.. js/d3
                                            (d3.geoPath
                                             (projection (d3.geoTransverseMercator))
                                             (rotate [120 -35])
                                             (fitExtent [[20 20] [480 480]] land)))
                                   svg (.. js/d3
                                           (select "svg"))
                                   ]
                               (.. svg
                                   (selectAll "path")
                                   (data d3data.geo_json)
                                   (attr "class" "update")
                                   enter
                                   (append "path")
                                   (attr "class" "grid")
                                   (attr "d" path)
                                   (append "title")
                                   (text (fn [d] (.-id d)))
                                   )
                               (.. svg
                                   (append "path")
                                   (datum (.. js/topojson
                                              (mesh d3data
                                                    (.-grids (.-objects d3data))
                                                    (fn [a b] (!= a b))
                                                    )))
                                   (attr "class" "grid-border")
                                   (attr "d" path)
                                   )
                               ))

      :component-did-update (fn [this] ())
      }))
