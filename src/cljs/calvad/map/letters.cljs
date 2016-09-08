(ns calvad.map.letters
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
            [cljsjs.remarkable]
            [calvad.map.map]))


(enable-console-print!)
(println "Edits to this text should show up in your developer console.")

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
                :alphabet (str/split "abcdefghijklmnopqrstuvwxyz" #"")
                :datablob {} ;; for plotting widget
                :griddata {} ;; for hpms data coloring map
                :active {:element nil} ;; for which grid cell clicked
                })


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
  :shuffle
  (fn
    [db [_ ]]
    ;;(println "shuffle and cut")
    (let [lettres (random-sample
                   0.5
                   (str/split "abcdefghijklmnopqrstuvwxyz" #"")
                   )]
      ;;(println lettres)
      (assoc-in db [:alphabet] lettres)))
  )

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
(defn d3-inner-letters [data]

    (reagent/create-class
     {:reagent-render (fn [] [:div
                              [:svg {:width 500 :height 80}
                               [:g {:transform  "translate(15,40)"}]]])

      :component-did-update (fn [this]
                             (let [[_ data] (reagent/argv this)
                                   d3data (clj->js data)]
                               (def texta (.. js/d3
                                              (select "g")
                                              (selectAll "text")
                                              (data d3data)))
                               (.attr texta "class" "update")
                               (.. texta
                                   enter
                                   (append "text")
                                   (attr "class" "enter")
                                   (attr "x" (fn [d i] (* i 15) ))
                                   (attr "dy" ".35em")
                                   (merge texta)
                                   (text (fn [d] d))
                                   )
                               (.. texta exit (remove))))

      :component-did-mount (fn []
                             (let [d3data (clj->js data)]
                               (.. js/d3
                                   (select "g")
                                   (selectAll "text")
                                   (data d3data)
                                   (attr "class" "update")
                                   enter
                                   (append "text")
                                   (attr "class" "enter")
                                   (attr "x" (fn [d i] (* i 15) ))
                                   (attr "dy" ".35em")
                                   (text (fn [d] d))
                                   )
                            ))}))

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
        ;;  [:div {:class "row"}
        ;;   [:div {:class "col secondapp"}
        ;;    [d3-inner-letters @datal]]
        ;;   [:div {:class "col secondcontrol"}
        ;;    [clickr]]
        ;;   ]
         [:div {:class "row"}
          [:div {:class "col secondapp"}
           [calvad.map.map/d3-inner-map
            gridtopo
            active
;;            griddata
              ]]
           [:div {:class "col secondcontrol"}
            [calvad.map.map/map-data-clickr]]
          ]
         ]
        ;; plotting pair
         [:div {:class "row"}
          [:div {:class "col secondapp"}
           [calvad.map.plot/d3-inner-plot
            datablob
            active
              ]]
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
