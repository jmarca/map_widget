(ns calvad.map.letters
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as reagent ]
            [reagent.dom :as rdom]
            [re-frame.core :refer [register-handler
                                   path
                                   register-sub
                                   dispatch
                                   dispatch-sync
                                   subscribe]]
            [clojure.string :as str]
            [clojure.set :as set]
            [cljsjs.d3]
            [cljsjs.remarkable]
            [calvad.map.map]))


(enable-console-print!)
(println "Edits to this text should show up in your developer console.")

(def alphabet (rest (str/split "abcdefghijklmnopqrstuvwxyz" #"")))

(def app-state {:circles [{:name "circle 1"
                      :x 10
                      :y 10
                      :r 10
                      :color "black"}
                     {:name "circle 2"
                      :x 35
                      :y 35
                      :r 15
                      :color "red"}
                     {:name "circle 3"
                      :x 100
                      :y 500
                      :r 30
                      :color "blue"}]
                :letts {}
                :alphabet ""
                :datablob {} ;; for plotting widget
                :griddata {} ;; for hpms data coloring map
                :active {:element nil} ;; for which grid cell clicked
                })

(defn d3-inner-l [d]
  (reagent/create-class
   {:reagent-render (fn [d]
                      (println "rendering a letter with " d)
                      [:text d (:d d) ])
    :display-name  "my-letter-component"  ;; for more helpful warnings & errors
    :component-will-update
    (fn [this new-argv] ;; fn[this new-argv]
      (let [[_ newdata] (reagent/argv this)
            d3data (clj->js newdata)
            ;; moving position, have something to do
            node (.select js/d3 (rdom/dom-node this))
            x (:x newdata)
            y (:y newdata)
            i (:i newdata)
            class (:class newdata)
            fill-opacity (:fill-opacity newdata)
            t (.. (js/d3.transition.)
                  (duration 750)
                  (ease js/d3.easeCubicInOut))
            ]
        (println "update a letter with " newdata )

        (.. node
                (attr "class" class)
                (attr "y" y)
                (text (:d newdata))
                ;;(transition t)
                (attr "x" x)
                ))
        )
    :component-did-update (fn [this]
                            ;;(println "letter component updated")
                            )
    :component-did-mount (fn [this]
                           (let [d3data (clj->js d)
                             ;; verify position and text
                             node (.select js/d3 (rdom/dom-node this))
                             x (.-x d3data)
                             y (.-y d3data)
                             d (.-d d3data)
                             class (.-class d3data)
                             fill-opacity (.-fill-opacity d3data)
                             t (.. (js/d3.transition.)
                                   (duration 750)
                                   (ease js/d3.easeCubicInOut))
                                 ]
                             (println "update a letter with " d3data )

                             (.. node
                                 (attr "class" class)
                                 (attr "y" y)
                                 (text d)
                                 ;;(transition t)
                                 (attr "x" x)
                                 ))
                           )

    }))


;; define your app data so that it doesn't get over-written on reload
;;---- Event handlers-----------
(register-handler
  :initialize-db
  (fn
    [_ _]
    app-state))

(register-handler
  :update
  (fn
    [db [_ idx param val]]
    ;;(println "idx " idx "param " param "val " val)
    (assoc-in db [:circles idx param ] val)))

(register-handler
  :enter-letter
  (fn
    [db [_ key valhash]]
    (println "try to load in db:  key " key  "val " valhash)
    (assoc-in db [:letts key] valhash)
    ))

(register-handler
  :exit-letter
  (fn
    [db [_ idx]]
    (println "exit letter " idx )
    (dissoc db [:letts idx ])))



(register-handler
  :shuffle
  (fn
    [db [_ ]]
    ;;(println "shuffle and cut")
    (let [lettres (clj->js (random-sample
                   0.5
                   ;;(.shuffle js/d3 (clj->js
                   alphabet
                   ;;))
                   ))]
      (println db)
      (dispatch [:alphabet lettres])))
  )

(register-handler
  :alphabet
  (fn
    [db [_ vals]]
    (println (str ":alphabet dispatcher handling " vals))
    (let [incoming (sort (set vals))
          current  (set (sort (keys (:letts db))))
          enters (doall (set/difference incoming current))
          exits  (doall (set/difference current incoming))
          updates (doall (set/difference (set incoming) (doall enters)))
          ]
      (println "previous" (sort current) "new" incoming)
      (println "enters " enters "exits " exits )
      (println db)
      ;; ;; also modify individual letters
      (let [;;delete-group (doall (map #(dispatch [:exit-letter (keyword %)]) exits))
            enter-group  (doall (map-indexed
                          (fn [idx ch]
                            (let [
                                  elem {:class "enter"
                                        :y 0
                                        :fill-opacity 1
                                        :x (* idx 15)
                                        :d ch
                                        :i idx}]
                                     elem))
                                 enters ))
            update-group (doall (map-indexed
                          (fn [idx ch]
                            (let [
                                  elem {:class "update"
                                        :y 0
                                        :fill-opacity 1
                                        :x (* idx 15)
                                        :d ch
                                        :i idx}]
                              ))
                          updates ))

            ]
        (for [e enter-group]
          (dispatch [:enter-letter (keyword (:d e)) e]))


        ;;(concat enter-group update-group)))

        )
      )))

;;(dispatch [:shuffle])

(register-handler
  :active
  (fn
    [db [_  val]]
    (assoc-in db [:active] val)))

(register-handler
  :datablob
  (fn
    [db [_  val]]
    (assoc-in db [:datablob] val)))



;;---- Subscription handlers-----------
(register-sub
  :circles
  (fn
    [db _]
    (reaction (:circles @db))))


(register-sub
  :alphabet
  (fn
    [db _]
    (reaction (:alphabet @db))))

(register-sub
  :active
  (fn
    [db _]
    (reaction (:active @db))))

(register-sub
  :datablob
  (fn
    [db _]
    (reaction (:datablob @db))))


(register-sub
  :letts
  (fn
    [db [_ d]]
    (reaction (get-in @db [:letts d]))))

;; circles using d3 controllers
(defn d3-inner [data]
 (reagent/create-class
    {:reagent-render (fn [] [:div [:svg {:width 400 :height 800}]])

     :component-did-mount (fn []
                            (let [d3data (clj->js data)]
                              (.. js/d3
                                  (select "svg")
                                  (selectAll "circle")
                                  (data d3data)
                                  enter
                                  (append "svg:circle")
                                  (attr "cx" (fn [d] (.-x d)))
                                  (attr "cy" (fn [d] (.-y d)))
                                  (attr "r" (fn [d] (.-r d)))
                                  (attr "fill" (fn [d] (.-color d))))))

     :component-did-update (fn [this]
                             (let [[_ data] (reagent/argv this)
                                   d3data (clj->js data)]
                               (.. js/d3
                                   (selectAll "circle")
                                   (data d3data)
                                   (attr "cx" (fn [d] (.-x d)))
                                   (attr "cy" (fn [d] (.-y d)))
                                   (attr "r" (fn [d] (.-r d))))))}))

;; letters using d3 controllers

;; -- Helpers -----------------------------------------------------------------

(defn allocate-next-id
  "Returns the next letter's id.
  Just increments forever.  All new letters get a unique id."
  []
  (let [my-atom (atom 0)]
       (swap! my-atom inc)
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


(defn d3-inner-letters
  [data]
  (reagent/create-class
   {:component-will-mount
    (fn []
      (println "will mount has been called")
      ;; (js/d3.interval
      ;;  (fn [elapsed]
      ;;    (println elapsed)
      ;;    (let [new-alpha
      ;;          (random-sample
      ;;           0.5
      ;;           (.shuffle js/d3 (clj->js alphabet)))
      ;;          ]
      ;;      (println  "new alphabet being dispatched")
      ;;      (dispatch [:alphabet (js->clj (sort new-alpha))]))
      ;;    )
      ;;  1500)


      ;; (dispatch [:alphabet (random-sample
      ;;                       0.5
      ;;                       alphabet)])
      )

    :display-name "letters"

    :reagent-render (fn
                      [data]
                      (let [d3data (clj->js data)
                            t (.. (js/d3.transition.)
                                  (duration 750))
                            texts (doall
                                   (map
                                    (fn [ch]
                                      (let [sub (subscribe [:letts ch])]
                                        ^{:key ch} [d3-inner-l @sub]))
                                    data))
                            ]
                        [:div.letters
                         [:svg {:class "letters" :width 500 :height 124}
                          [:g {:transform  "translate(15,62)"}
                           texts
                           ]]]
                        ))


    :component-did-update (fn [this]
                            (let [[_ data] (reagent/argv this)
                                  d3data (clj->js data)
                                  t (.. (js/d3.transition.)
                                        (duration 750))
                                  g (.select js/d3 "svg.letters g")
                                  ]
                              nil))

    :component-did-mount (fn []
                           ;; (println (str "did mount called for letters, setting alphabet to" alphabet))
                           ;; (dispatch [:alphabet alphabet])
                           )
                           }))

;; the slider widgets for user actions
(defn slider [param idx value]
  [:input {:type "range"
           :value value
           :min 0
           :max 500
           :style {:width "100%"}
           :on-change #(dispatch [:update idx param (-> % .-target .-value)])}])

(defn clickr [param idx value]
  [:input {:type "button"
           :on-click #(dispatch [:shuffle])}])


;; (for [todo  @visible-todos]
;;   ^{:key (:id todo)} [todo-item todo])

(defn sliders [data]
    [:div (for [[idx d] (map-indexed vector data)]
            ^{:key (str "slider-" idx)}
            [:div
             [:h3 (:name d)]
             "x " (:x d) (slider :x idx (:x d))
             "y " (:y d) (slider :y idx (:y d))
             "r " (:r d) (slider :r idx (:r d))])])


;; ;; letters using d3 controllers
;; (defn my-component []
;;   (let [necessary-state (r/cursor global-state ["foo" "bar"])]
;;     [d3-gauge @necessary-state]))

(defn app [gridtopo]
    (let [data (subscribe [:circles])
          datal (subscribe [:alphabet])
          active (subscribe [:active])
          datablob (subscribe [:datablob])]
      (fn []
        [:div {:class "container"}
        ;;  [:div {:class "row"}
        ;;   [:div {:class "col firstapp"}
        ;;    [d3-inner @data]]
        ;;   [:div {:class "col firstcontrol"}
        ;;    [sliders @data]]]

         [:div {:class "row"}
          [:div {:class "col letrdapp"}
           [d3-inner-letters @datal]]
          [:div {:class "col letrcontrol"}
           [clickr]]
          ]

;;          ;; map testing
;;          [:div {:class "row"}
;;           [:div {:class "col mapapp"}
;;            [calvad.map.map/d3-inner-map
;;             gridtopo
;;             active
;; ;;            griddata
;;               ]]
;;            [:div {:class "col mapcontrol"}
;;             [calvad.map.map/map-data-clickr]]
;;           ]
;;         ;; plotting pair
;;          [:div {:class "row"}
;;           [:div {:class "col plotapp"}
;;            [calvad.map.plot/d3-inner-plot
;;             datablob
;;             active
;;               ]]
;;           ]
         ]
        )))

(defn ^:export main [json-file]
  (let []
  (dispatch-sync [:initialize-db])
  (.json js/d3 json-file
         (fn [error json]
           (reagent/render-component [app json]
                                     (. js/document (getElementById "app")))
           ))))


(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
