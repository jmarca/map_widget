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

(defn colorit
  [v]
  (let [c (.. js/d3
             (scalePow.)
             (exponent 0.3)
             (domain (clj->js [0 1744422]))
             (range (clj->js [0 1])))]
    (.interpolateViridis js/d3 (c v))))


(defn hpmshandler [err json]
  (let [jj (js->clj json)
        groupdfeat (group-by #(str (get  %  "cell_i") "_"(get % "cell_j"))  jj)
        grpkeys (clj->js  (keys groupdfeat))
        g (.. js/d3 (selectAll ".map svg g"))
        cells (.. g
                  (selectAll "path")
                  (data grpkeys (fn [d] d)))
        ]
    (.. cells
        exit
        (classed "nohpms" true)
        (classed "havedata" false))
    (.. cells
        (classed "nohpms" false)
        (classed "havedata" true)
        (style "fill"
              (fn [d i]
                    (let [mydata (get (first (get groupdfeat d)) "sum_vmt")
                          vmtclr (colorit mydata)
                          ]
                    ;; (println (str id " " vmtclr))
                    (str vmtclr)))))))
    ;;       (attr "class"
    ;;             (fn [d]
    ;;               (let [id (str (.-cell_i d) "_" (.-cell_j d))
    ;;                     vmtclr (colorit (.-sum_vmt d))]
    ;;                 (println (str id " " vmtclr))
    ;;                 (str "grid " id vmtclr)
    ;;                 ))))
    ;;   )

(defn date-clicker
  []
  (let [jf "hpms2009.json"]
    (.json js/d3 jf hpmshandler)
    ))




(defn map-data-clickr [param idx value]
  [:input {:type "button"
           :on-click #(date-clicker)}])

;; map

(defn d3-inner-map [data active click-handler]
  (defn clicked
    [e idx grids]
    ;;(let [newactive (.-target e)]
    (println (clj->js active))
    (if (nil? (:element @active))
      (println "nothing to do")
      (.classed (:element @active) "active" false))
    (this-as this
      (let [e (.select js/d3 this)]
        (if (.classed e "active")
          (.classed e "active" false)
          (.classed e "active" true))
        ;;(attr "x1" #(.. % -source -x))
        (dispatch [:active {:element e}])
        ;; (click-handler {:element e
        ;;                  ;;:data d
        ;;                  })
        )))
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

                                            (scaleExtent (clj->js [1 512]))
                                            (on "zoom" zoomed))
                                   land-features (group-by :id (map  (fn [d]
                                                    (let [p (get d "properties")
                                                          id (str/replace
                                                              (str (get p "i_cell") "_" (get p "j_cell"))
                                                              #"\.0*"
                                                              "")
                                                          ]
                                                      {:id id :data (assoc d :id id)}))
                                                       (js->clj (.-features land))
                                                  ))
                                   ;; lif (first land-features)
                                   ;;merged-feat-id (map (fn [a b] {:id b :data (assoc a :id b)}) land-features land-ids)
                                   ;;mff (first merged-feat-id)
                                   ;;groupdfeat (group-by :id merged-feat-id)
                                                        ;; #(str/replace
                                                        ;;   (get (get % "properties") "i_cell")
                                                        ;;   #"\.0*"
                                                        ;;   "")
                                                        ;; land-features)
                                   grpkeys (clj->js (keys land-features))
                                   ]
                               (.. svg
                                   (call zoom))

                               (.. g
                                   (selectAll "path")
                                   (data grpkeys (fn [d] d))
                                   enter
                                   (append "path")
                                   (attr "class"
                                         (fn [d]
                                           (str "grid " d)
                                             ))
                                   (attr "d" (fn [d]
                                               (let [myentry (first (get land-features d))
                                                     mydata (get myentry :data)
                                                     mypath (path (clj->js mydata))
                                                     ]
                                                 mypath
                                                 )))
                                   (on "click" clicked)
                                   )
                               ))

      :component-did-update (fn [this] ())
      }))
