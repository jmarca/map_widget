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
            [cljsjs.remarkable]))


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
                :alphabet (str/split "abcdefghijklmnopqrstuvwxyz" #"")})


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
    (println "idx " idx "param " param "val " val)
    (assoc-in db [:circles idx param ] val)))

(register-handler
  :shuffle
  (fn
    [db [_ ]]
    (println "shuffle and cut")
    (let [lettres (random-sample
                   0.5
                   (str/split "abcdefghijklmnopqrstuvwxyz" #"")
                   )]
      (assoc-in db [:alphabet] val)))
  )


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
  (let [updater (fn [this]
                           (let [[_ data] (reagent/argv this)
                                 d3data (clj->js data)]
                             (println d3data)
                             (def text (.. js/d3
                                          (select "g")
                                          (selectAll "text")
                                          (data d3data)))
                            (.attr text "class" "update")
                            (.. text
                                enter
                                (append "text")
                                (attr "class" "enter")
                                (attr "x" (fn [d i] (* i 15) ))
                                (attr "dy" ".35em")
                                (merge text)
                                (text (fn [d] d))
                                )))]
    (reagent/create-class
     {:reagent-render (fn [] [:div
                              [:svg {:width 500 :height 80}
                               [:g {:transform  "translate(15,40)"}]]])

      :component-did-update updater

   :component-did-mount updater
                            })))

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


(defn app []
  (let [data (subscribe [:circles])
        datal (subscribe [:alphabet])]
    (fn []
      [:div {:class "container"}
       [:div {:class "row"}
        [:div {:class "col firstapp"}
         [d3-inner @data]]
        [:div {:class "col firstcontrol"}
         [sliders @data]]]
       [:div {:class "row"}
        [:div {:class "col secondapp"}
         [d3-inner-letters @datal]]
        [:div {:class "col secondcontrol"}
         [clickr]]
         ]
        ]
      )))

(let []
  (dispatch-sync [:initialize-db])
  (reagent/render-component [app]
                            (. js/document (getElementById "app"))))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)



     ;; (defn update-letters
;;   "update the d3 thing, in this case letters"
;;   [dom-node data ...]
;;   ;; draw here.  not super sure how to
;;   (.. js/d3
;;       (let [g (select dom-node)
;;             ]
;;         (. g (selectAll "text"))))
;;   )


;; (defn build-letters-svg
;;   "build the letters node to manipulate"
;;   [dom-node]
;;   (.. js/d3
;;       (let [svg (select dom-node)
;;             width (. svg attr "width")
;;             height (. svg attr "height")
;;             ]
;;         (append "g")
;;         (. g attr "transform" (str/join
;;                                "translate(32,"
;;                                (/ height 2)
;;                                ")"
;;                                ))
;;         )))


;; (defn d3-gauge [data ...]
;;   (let [dom-node (r/atom nil)]
;;     (r/create-class
;;       {:component-did-update
;;        (fn [this old-argv]
;;          (let [[_ data ...] (r/argv this)]
;;            ;; This is where we get to actually draw the D3 gauge.
;;            (update-letters @dom-node data ...)))

;;        :component-did-mount
;;        (fn [this]
;;          (let [node (r/dom-node this)]
;;            ;; This will trigger a re-render of the component.

;;            (build-letters-svg node )
;;            d3Chart.create(el, {
;;                                width: '100%',
;;                                height: '300px'
;;                                }, this.getChartState());
;;            },

;;            (reset! dom-node node)))

;;        :reagent-render
;;        (fn [data ...]
;;          ;; Necessary for Reagent to see that we depend on the dom-node r/atom.
;;          ;; Note: we don't actually use any of the args here.  This is because
;;          ;; we cannot render D3 at this point.  We have to wait for the update.
;;          @dom-node
;;          [:div.app [:svg]])})))


;; (defn ^:export main [json-file]
;;   (let [width 960
;;         height 600
;;         letters-thing (build-force-layout width height)
;;         svg (build-svg width height)]
