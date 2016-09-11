(ns calvad.map.views
  (:require [reagent.core  :as reagent]
            [reagent.dom :as rdom]
            [re-frame.core :refer [subscribe dispatch]]))


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




(defn- letter-render
  ([comp]
   (let [
         data  (-> comp reagent/props )  ; use props
         d3data (clj->js data)
         node (.select js/d3 (rdom/dom-node comp))
         x (.-x d3data)
         y (.-y d3data)
         txt (.-text d3data)
         class (.-class d3data)
         fill-opacity (.-fill-opacity d3data)
         t (.. (js/d3.transition.)
               (duration 750)
               (ease js/d3.easeCubicInOut))
         ]
     ;;(println "will update a letter with " d3data )

        (.. node
                (attr "class" class)
                (transition t)
                (attr "x" x)
                (attr "y" y)
                (text txt)
                ))
        ))

(defn d3-inner-l [data]
  (reagent/create-class
   {:reagent-render (fn [data]
                      (println "rendering" data)
                      (let [y (if (= "enter" (:class data)) -20 0)]
                        [:text {:y y}]))
    :display-name  "my-letter-component"  ;; for more helpful warnings & errors

    ;; will update doesn't work for me...puts the component in the
    ;; wrong position, which means it is likely getting the old
    ;; params, not the new ones.
    ;; :component-will-update letter-render
    :component-did-mount letter-render
    :component-did-update letter-render
    :component-will-unmount (fn [this] (println "did unmount a letter"))
    }))


(defn d3-inner-letters
  [data]
  (reagent/create-class
   {
    :display-name "letters"
    ;; render the letters object with nested, individual letters
    :reagent-render (fn
                      [data]
                      (let [texts (doall(map
                                         (fn [ch]
                                           (let [sub (subscribe [:letter ch])]
                                             ^{:key ch} [d3-inner-l @sub
                                                         ]))
                                         data))
                            ]
                        [:div.letters
                         [:svg {:class "letters" :width 500 :height 124}
                         [:g {:transform  "translate(15,62)"}
                          texts
                          ]]]
                        ))
    }))

(defn clickr [param idx value]
  [:input {:type "button"
           :on-click #(dispatch [:shuffle])}])


;; ;; letters using d3 controllers

(defn app
  []
    (let [datal (subscribe [:letters])]
      (fn []
        [:div {:class "container"}
         [:div {:class "row"}
          [:div {:class "col letrdapp"}
           [d3-inner-letters @datal]]
          [:div {:class "col letrcontrol"}
           [clickr]]
          ]
         ]
        )))
