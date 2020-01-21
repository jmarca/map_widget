(ns calvad.map.events
  (:require
    [clojure.string :as str]
            [clojure.set :as set]
    [calvad.map.db    :refer [initial-state]]
    [re-frame.core :refer [reg-event-db reg-event-fx inject-cofx path trim-v
                           after debug dispatch]]
    [cljs.spec     :as s]
    [cljsjs.topojson]
    [cljsjs.d3]
    ))

(enable-console-print!)

;; -- Interceptors --------------------------------------------------------------
;;

(defn check-and-throw
  "throw an exception if db doesn't match the spec."
  [a-spec db]
  (when-not (s/valid? a-spec db)
    (throw (ex-info (str "spec check failed: " (s/explain-str a-spec db)) {}))))


;; Event handlers change state, that's their job. But what happens if there's
;; a bug which corrupts app state in some subtle way? This interceptor is run after
;; each event handler has finished, and it checks app-db against a spec.  This
;; helps us detect event handler bugs early.
(def check-spec-interceptor (after (partial check-and-throw :calvad.map.db/db)))


;; the chain of interceptors we use for all handlers that manipulate letters
(def grid-interceptors [;;check-spec-interceptor               ;; ensure the spec is still valid
                          (path :grid)                        ;; 1st param to handler will be the value from this path
                          ;;debug                                  ;; look in your browser console for debug logs
                          trim-v])                               ;; removes first (event id) element from the event vec

;; the chain of interceptors we use for all handlers that manipulate the alphabet
(def alphabet-interceptors [check-spec-interceptor             ;; ensure the spec is still valid
                          (path :alphabet)                       ;; 1st param to handler will be the value from this path
                          ;;->local-store                        ;; write todos to localstore
                          debug                                  ;; look in your browser console for debug logs
                          trim-v])                               ;; removes first (event id) element from the event vec



;; -- Helpers -----------------------------------------------------------------


;; -- Event Handlers ----------------------------------------------------------

(reg-event-db                 ;; setup initial state
  :initialize-db                     ;; usage:  (dispatch [:initialize-db])
  ;;[check-spec-interceptor]
  (fn
    [db _]
    (merge db initial-state)))    ;; what it returns becomes the new state


;; update (or create) an existing (or non-existant) letter hash in db
;; for example, change position x or y, set class, etc
;; usage:  (dispatch [:letter-update  {:text "d" :hash "of" :properties true}])
;;
;; not yet used, so not yet uncommented
;;
;; (reg-event-db
;;   :cell-update
;;   grid-interceptors
;;   (fn [grid [value]]
;;     (let [id (:cellid value)]
;;       (assoc-in grid [id :data] value))))



(reg-event-db
 :update-path
 (fn
   [db [_ dims]]
   ;; get the land, get the path, update the path
   (let [land (:land db)
         geoPath (js/d3.geoPath.)
         gtm (js/d3.geoTransverseMercator.)
         margin (:margin dims)
         width (:width dims)
         height (:height dims)
         path (.projection geoPath
                           (.rotate gtm (clj->js [124 -32.5])) ;; center of calif
                           (.fitExtent gtm (clj->js [[(nth margin 3)
                                                      (nth margin 0)]
                                                     [(- width (nth margin 1))
                                                      (- height (nth margin 2))]
                                                     ]) land))
         ]
     (assoc-in db [:path] path)
     )
   ))



;; (reg-event-fx
;;  :grid-json
;;  [(path :grid) trim-v ];;check-spec-interceptor]
;;  (fn [olddata [newdata]]
;;    ;; 1st argument is coeffects, instead of db
;;    ;; try to break up and stash the incoming data.  I think
;;    ;; (println "handling " newdataalpha " removing " oldalpha)
;;    (let [incoming newdata
;;          outgoing (:db olddata)
;;          enters  (set/difference (set incoming) (set outgoing))
;;          exits   (set/difference (set outgoing) (set incoming))
;;          updates (set/difference (set incoming) (set enters))
;;          ;; create objects for building statements
;;          update-group  (map
;;                         (fn [cell]
;;                           (let [
;;                                 elem {:class (if (set/subset? cell enters) "enter" "update")
;;                                       ;; incoming data element
;;                                       }]
;;                             elem))
;;                        incoming )
;;          disp (if (> (count exits) 0 )
;;                 {:dispatch-n (concat (vector [:databet  newdata])
;;                                      (concat (mapv (fn [d] (vector :letter-update d )) update-group)
;;                                              (mapv (fn [d] (vector :letter-exit d )) exits)
;;                                      ))}
;;                 {:dispatch-n (concat (vector [:databet  newdata])
;;                                      (mapv (fn [d] (vector :letter-update d )) update-group)
;;                                      ;;(map (fn [d] (vector :letter-exit d )) exits)
;;                                      )}
;;                 )

;;          ]
;;      (println "updates" update-group)
;;      (println "exits" exits)
;;      (println disp)
;;      disp)))

;; a handler to accept the topojson doc from the server

(reg-event-fx
 :process-topojson
;; [debug]
  (fn
    [cofx [_ response dims]]   ;; destructure the response from the event vector
    (println "handling topojson")
    (let [
          db (:db cofx)
          ;;allgeoms (.geometries (.grids (.objects response)))
          allgeoms (.-geometries (.-grids (.-objects response)))
          land (.. js/topojson
                   (feature response
                            (clj->js {:type "GeometryCollection"
                                      :geometries allgeoms})
                            ))
          geoPath (js/d3.geoPath.)
          gtm (js/d3.geoTransverseMercator.)
          margin (:margin dims)
          width (:width dims)
          height (:height dims)
          path (.projection geoPath
                            (.rotate gtm (clj->js [124 -32.5])) ;; center of calif
                            (.fitExtent gtm (clj->js [[(nth margin 3)
                                                       (nth margin 0)]
                                                      [(- width (nth margin 1))
                                                       (- height (nth margin 2))]
                                                      ]) land))

          land-features (map  (fn [d]
                                (let [p (.-properties d)
                                      id (str/replace
                                          (str (.-i_cell p) "_" (.-j_cell p))
                                          #"\.0*"
                                          "")
                                      ]
                                  {:cellid id
                                   :svgpath (path d)
                                   :data d}))
                               (.-features land)
                              )
          ]
      ;;

      {:db (merge db {:land land
                 :path path
                 :grid (into (sorted-map)
                             ;; map each of grid cells into the right slot
                             (map (fn [feat]
                                    (let [id (:cellid feat)
                                          ]
                                      [id feat]))
                                  land-features ))})
       :dispatch [:not-loading]}

      )))


(defn colorit
  [v]
  (let [c (.. js/d3
             (scalePow.)
             (exponent 0.3)
             (domain (clj->js [0 1744422]))
             (range (clj->js [0 1])))]
    (.interpolateViridis js/d3 (c v))))


(reg-event-db
 :process-hpmsjson
 ;; [debug]
 (fn
   [db [_ json]]   ;; destructure the response from the event vector
   (println "handling hpmsjson")

   (let [griddb (:grid db)
         colorkey (:colorkey db)
         incoming (into
                   (sorted-map)
                   (map (fn [record]
                          (let [id (str (.-cell_i  record)
                                        "_"
                                        (.-cell_j record ))
                                data-value (aget record colorkey)
                                color (colorit data-value)
                                ]

                            [id {:hpms record
                                 :color color} ]))
                        json))
         ;; Need to handle the case in which there is an old entry, but no new one.
         ;; One issue I'm not handling right now is if there is a grid
         ;; entry in old but not in new, I want old to go away.
         current (set
                  (keys
                   (filter
                    (fn [[idx d]] (nil? (find d :hpms)))
                    griddb)))
         newk (set (keys incoming))
         exit (set/difference current newk) ;; current less newk is exit set
         ;; make sure "exit" set is switched off

         ;; but trying to do the merge in just one pass over the data
         ;; because the sets are big and expensive
         ;; last one wins, so if there is old HPMS data, the incoming one will win
         ;; updates (merge-with merge (:grid db) incoming)

         updates (into
                  (sorted-map)
                  (map (fn [idx]
                         (let [oldr (get griddb idx)
                               newr (get incoming idx)]
                           (if (and (not (nil? (:hpms oldr)))
                                    (= (:hpms oldr) (:hpms newr)))
                             [idx oldr]
                             [idx (merge oldr newr)]
                             )))
                       newk ;; iterate over incoming keys
                         ))
         exits (into
                (sorted-map)
                (map (fn [idx]
                       [idx (dissoc (get griddb idx) [:hpms :color]) ])
                       exit ;; iterate over exiting keys
                       ))

         updatedgrid (merge griddb exits updates)

         ]
     ;;(println (first updates))
     ;;(println (first exits))
     ;;(println (first updatedgrid))
     ;;(println "current:" (get griddb (first newk)))
     ;;(println "revised:" (get updatedgrid (first newk)))
     (merge db {:grid updatedgrid})
     ;;db
     )))


(reg-event-db
 :set-colorkey
 (fn
   [db [_ key-to-use]]
   (assoc-in db [:colorkey] key-to-use)))


(reg-event-db
 :not-loading
 (fn
   [db _]
   (dissoc db :loading?)))


(reg-event-db
 :loading
 (fn
   [db _]
   (assoc db :loading? true)))

(reg-event-fx
 :get-hpmsdata
 (fn
   [{db :db} [_ json-file]]
   (.json js/d3 json-file
          (fn [e j]
            (if (not (nil? e))
              (println "json fetch error " e)
              (dispatch [:process-hpmsjson j]))
            (dispatch [:not-loading])))
   {:dispatch [:loading]}))
