(ns calvad.map.letters
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as reagent ]
            [reagent.dom :as rdom]
            [re-frame.core :refer [path
                                   trim-v
                                   reg-sub
                                   reg-event-db
                                   reg-event-fx
                                   debug
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

(def initial-state
  {:timer (js/Date.)
   :time-color "#f88"})

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
                :letters (sorted-map)
                :alphabet ""
                :active {} ;; for which grid cell clicked
                })

;; -- Event Handlers ----------------------------------------------------------


(reg-event-db                 ;; setup initial state
  :initialize-db                     ;; usage:  (dispatch [:initialize])
  (fn
    [db _]
    (merge db app-state)))    ;; what it returns becomes the new state


;; the chain of interceptors we use for all handlers that manipulate letters
(def letter-interceptors [;;check-spec-interceptor               ;; ensure the spec is still valid
                          (path :letters)                        ;; 1st param to handler will be the value from this path
                          ;;->local-store                        ;; write todos to localstore
                          debug                                  ;; look in your browser console for debug logs
                          trim-v])                               ;; removes first (event id) element from the event vec

;; the chain of interceptors we use for all handlers that manipulate letters
(def alphabet-interceptors [;;check-spec-interceptor             ;; ensure the spec is still valid
                          (path :alphabet)                       ;; 1st param to handler will be the value from this path
                          ;;->local-store                        ;; write todos to localstore
                          debug                                  ;; look in your browser console for debug logs
                          trim-v])                               ;; removes first (event id) element from the event vec

;; (defn allocate-next-id
;;   "Returns the next letter id.
;;   Assumes letters are sorted (sorted-map).
;;   Returns one more than the current largest id."
;;   [todos]
;;   ((fnil inc 0) (last (keys todos))))

;; new letter into db
;; usage:  (dispatch [:letter-enter  {:text "d" :hash "of" :properties true}])
;; (reg-event-db                     ;; given a hash with a :text entry, create or edit new letter entry
;;   :letter-enter
;;   letter-interceptors
;;   (fn [letters [value]]              ;; the "path" interceptor in `todo-interceptors` means 1st parameter is :todos
;;     (let [id (:text value)]
;;       (assoc todos id value))))
;; for my money, assoc is the same as assoc-in


;; update (or create) an existing (or non-existant) letter hash in db
;; for example, change position x or y, set class, etc
;; usage:  (dispatch [:letter-update  {:text "d" :hash "of" :properties true}])
(reg-event-db
  :letter-update
  letter-interceptors
  (fn [letters [value]]
    (let [id (:text value)]
      (assoc-in letters [id] value))))


;; remove a letter from the db completely.  Just forget about it
;; usage:  (dispatch [:letter-exit "d"])
(reg-event-db
  :letter-exit
  letter-interceptors
  (fn [letters [id]]
    (dissoc letters id)))


;; update the alphabet, but not here does not trigger letters updates
;; usage:  (dispatch [:alphabet "asdktrhd"])
(reg-event-db
  :alphabet
  alphabet-interceptors
  (fn [alphabet [value]]
     value))

(reg-event-fx
 :alphabet-letters
 [(path :alphabet) trim-v]
 (fn [oldalpha [newalpha]]
   ;; 1st argument is coeffects, instead of db
   ;; endeavor to make a list of letter updates to dispatch

   (let [incoming (str/split newalpha #"")
         outgoing (str/split (:db oldalpha) #"")
          enters  (set/difference (set incoming) (set outgoing))
          exits   (set/difference (set outgoing) (set incoming))
          updates (set/difference (set incoming) (set enters))
     ;; create objects for building statements
         update-group  (map-indexed
                       (fn [idx ch]
                         (let [
                               elem {:class (if (set/subset? ch enters) "enter" "update")
                                     :y 0
                                     :fill-opacity 1
                                     :x (* idx 15)
                                     :text ch
                                     :i idx}]
                           elem))
                       incoming )
         dispatch-list (map (fn [d] {:dispatch [:letter-update d]}) update-group)
         exit-list (map (fn [d] {:dispatch [:letter-exit d ]}) exits)
         ]
     (println outgoing)
     (println incoming)
     (println exits)
     (println dispatch-list)
     ;;(println
     (concat {:dispatch [:alphabet  newalpha]}
             dispatch-list
             exit-list)
     ;;)

     )))
;; untested


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






(reg-event-fx
  :shuffle
  (fn
    [db [_ ]]
    ;;(println "shuffle and cut")
    (let [lettres (random-sample
                   0.5
                   ;;(.shuffle js/d3 (clj->js
                   alphabet
                   ;;))
                   )]
      (println lettres)
      (dispatch [:alphabet-letters lettres])
      ))
  )

;; experimenting and learning interceptors
;; (def trim-event
;;   (re-frame.core/->interceptor
;;     :id      :trim-event
;;     :before  (fn [context]
;;                (let [trim-fn (fn [event] (-> event rest vec))]
;;                  (update-in context [:coeffects :event] trim-fn)))))
;;
;; (defn db-handler->interceptor
;;   [db-handler-fn]
;;   (re-frame.core/->interceptor     ;; a utility function supplied by re-frame
;;     :id     :db-handler            ;; ids are decorative only
;;     :before (fn [context]
;;               (let [{:keys [db event]} (:coeffects context)    ;; extract db and event from coeffects
;;                     new-db (db-handler-fn db event)]           ;; call the handler
;;                 (assoc-in context [:effects :db] new-db))))) ;; put db back into :effects

;; (reg-event-db
;;  :alphabet-change
;;  [trim-event intercept-letters]
;;   (fn [db v]
;;     {:db (assoc db :alphabet )}
;;     [db [_ vals]]
;;     (println (str ":alphabet dispatcher handling " vals))
;;     (let [incoming (sort (set vals))
;;           current  (set (sort (keys (:letts db))))
;;           enters (doall (set/difference incoming current))
;;           exits  (doall (set/difference current incoming))
;;           updates (doall (set/difference (set incoming) (doall enters)))
;;           ]
;;       (println "previous" (sort current) "new" incoming)
;;       (println "enters " enters "exits " exits )
;;       (println db)
;;       ;; ;; also modify individual letters
;;       (let [;;delete-group (doall (map #(dispatch [:exit-letter (keyword %)]) exits))
;;             enter-group  (doall (map-indexed
;;                           (fn [idx ch]
;;                             (let [
;;                                   elem {:class "enter"
;;                                         :y 0
;;                                         :fill-opacity 1
;;                                         :x (* idx 15)
;;                                         :d ch
;;                                         :i idx}]
;;                                      elem))
;;                                  enters ))
;;             update-group (doall (map-indexed
;;                           (fn [idx ch]
;;                             (let [
;;                                   elem {:class "update"
;;                                         :y 0
;;                                         :fill-opacity 1
;;                                         :x (* idx 15)
;;                                         :d ch
;;                                         :i idx}]
;;                               ))
;;                           updates ))

;;             ]
;;         (for [e enter-group]
;;           (dispatch [:enter-letter (keyword (:d e)) e]))


;;         ;;(concat enter-group update-group)))

;;         )
;;       )))

;;(dispatch [:shuffle])

(reg-event-db
  :active
  (fn
    [db [_  val]]
    (assoc-in db [:active] val)))


;;---- Subscription handlers-----------
(reg-sub
  :circles
  (fn
    [db _]
    (reaction (:circles @db))))


(reg-sub
  :alphabet
  (fn
    [db _]
    (reaction (:alphabet @db))))

(reg-sub
  :active
  (fn
    [db _]
    (reaction (:active @db))))

;; one again working through todo-mvc and other
;; two step registration of a subscription
(defn sorted-letters
  [db _]
  (:letters db))
(reg-sub :sorted-letters sorted-letters)

(reg-sub
 :letters
  ;; Although not required in this example, it is called with two paramters
  ;; being the two values supplied in the originating `(subscribe X Y)`.
  ;; X will be the query vector and Y is an advanced feature and out of scope
  ;; for this explanation.
 (fn [query-v _]
   (subscribe [:sorted-letters]))    ;; returns a single input signal

  ;; This 2nd fn does the computation. Data values in, derived data out.
  ;; It is the same as the two simple subscription handlers up at the top.
  ;; Except they took the value in app-db as their first argument and, instead,
  ;; this function takes the value delivered by another input signal, supplied by the
  ;; function above: (subscribe [:sorted-todos])
  ;;
  ;; Subscription handlers can take 3 parameters:
  ;;  - the input signals (a single item, a vector or a map)
  ;;  - the query vector supplied to query-v  (the query vector argument
  ;; to the "subscribe") and the 3rd one is for advanced cases, out of scope for this discussion.
  (fn [sorted-letters query-v _]
    (keys sorted-letters)))

(reg-sub
 :letter
 (fn [query-v _]
   (subscribe [:sorted-letters]))
 (fn [sorted-letters query-v _]
   (get sorted-letters query-v)))

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
                            texts (doall(map
                                    (fn [ch]
                                      (let [sub (subscribe [:letter ch])]
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
          datal (subscribe [:sorted-letters])
          active (subscribe [:active])
          ;;datablob (subscribe [:datablob])
          ]
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
