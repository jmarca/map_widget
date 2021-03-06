(ns calvad.map.views
  (:require [reagent.core  :as reagent]
            [reagent.dom :as rdom]
            [re-frame.core :refer [subscribe dispatch]]
            [clojure.string :as str]
            [cljsjs.topojson]
            [cljsjs.d3]
            ))


;; reagent/create-class docs:
;; Create a component, React style. Should be called with a map,
;; looking like this:
;; {:get-initial-state (fn [this])
;; :component-will-receive-props (fn [this new-argv])
;; :should-component-update (fn [this old-argv new-argv])
;; :component-will-mount (fn [this])
;; :component-did-mount (fn [this])
;; :component-will-update (fn [this new-argv])
;; :component-did-update (fn [this old-argv])
;; :component-will-unmount (fn [this])
;; :reagent-render (fn [args....])   ;; or :render (fn [this])
;; }

;; Individual grid cell rendering

;; helper function
;; will need to partial this with path at the least

(defn- cell-render
  ([path comp]
   (let [
         record (-> comp reagent/props )  ; use props to get data
         data (:data record)
         gridpath (path (clj->js data))
         node (.select js/d3 (rdom/dom-node comp))
         ]
     ;;(println "processing " (.-cellid d3data))
     (.. node
         (attr "d" gridpath)
         )
     )))


;; okay, first real issue with this.  How to handle active.  Is it a
;; db thing, with one entry per grid cell, or is it a single value,
;; and all grid cells subscribe to it.  If the latter (which is what
;; I'm going to try first) then will clicking on a single cell cause
;; all grid cells to re-render and/or go through the update cycle?

(defn grid-cell [data active]
  (let [
        ;;path      (subscribe [:path])
        ]

    (reagent/create-class
     {:reagent-render (fn [data active]
;;                        (println "rendering " data ", active " active)
                        (let [active (if (= active (:cellid data)) " active" "")
                              d (:svgpath data)
                              class (str "grid" active)
                              [_ color] (find data :color)
                              c (if (nil? color)
                                  {:class class :d d}
                                  ;; else, have vmt, so color it so
                                  {:class class :d d
                                   :style {:fill color}
                                   })
                              ]
                          [:path c ]
                          )
                      )
    :display-name  "one-grid-cell"  ;; for more helpful warnings & errors
      ;;:component-did-mount  (partial cell-render @path)
      ;;:component-did-update (partial cell-render @path)
    })))


;; whole map rendering

;; helper functions
;; will need to partial this function with the "g"
;; d3 element once I grab that in the grids renderer
(defn- zoomed
  "handle a d3 zoom event"
  [g]
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


(defn map-of-grids
  "plot out a map made up of individual grid cells"
  [data  ;; call this with a subscription to grid cell ids
   land ;; and a subscr. to the land object
   width
   height
   ]

  (let [activesub (subscribe [:active])]
    (reagent/create-class
     {
      :display-name "themap"
      ;; render the map with nested, individual components
      :reagent-render (fn [data] ;; should be a list of grid cell ids
                        (let [cells (doall(map
                                           (fn [id]
                                             (let [grid (subscribe [:grid id])]
                                               ^{:key id} [grid-cell
                                                           @grid
                                                           @activesub
                                                           ]))
                                           data))
                              ]
                          [:div.map
                           [:svg {:class "map" :width width :height height}
                            [:g
                             cells
                             ]]]))
      ;; did mount tweaks the svg, projecting, scaling, zooming
      :component-did-mount
      (fn
        [data land]
        (let [
              ;; need to grab the geometry info from the db
              svg (.. js/d3
                      (select ".map svg"))
              g (.. js/d3
                    (select ".map svg g"))

              zoom-handler (partial zoomed g)
              zoom (.. (.. js/d3
                           zoom.)
                       (scaleExtent (clj->js [1 512])) ;; hand tweaked this
                       (on "zoom" zoom-handler))
              ]
          ;; everything is now handled in sub component grid-cell
          ;; except for the initial zoom call to scale properly
          (.. svg
              (call zoom))
          ))
      })))

(defn clickr [loading]
  (let [params (if loading
                 {:type "button"
                  :disabled true
                  :on-click #(dispatch [:get-hpmsdata "hpms2009.json"])
                  }
                 ;; else, not loading, so enable the clicker
                 {:type "button"
                  :on-click #(dispatch [:get-hpmsdata "hpms2009.json"])
                  })]
    [:button params "get grid data"]))


;; ;; letters using d3 controllers

(defn app
  [width height]
  (let [datal (subscribe [:grid-ids])
        datab (subscribe [:land])
        loading (subscribe [:loading])]
    (fn []
      [:div {:class "container"}
       [:div {:class "row"}
        [:div {:class "col mapapp"}
         [map-of-grids @datal @datab width height]]
        [:div {:class "col mapcontrol"}
         [clickr @loading]]
        ]
       ]
      )))
