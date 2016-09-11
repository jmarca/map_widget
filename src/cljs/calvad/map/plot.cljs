(ns calvad.map.plot
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
            ))

;; mostly copying from my old work and from the brush and zoom example


;; (defn brushed
;;   "handle brush events"
;;   []
;;   (let [event (.-event js/d3)
;;         source-event (.-sourceEvent event)]
;;     (if (and (not (nil? source-event)) (= (.-type source-event) "zoom"))
;;       ;; do nothing
;;       nil
;;       ;; do something
;;       (let [s (or (.-selection event) (.. x2 (range)))]
;;         (.domain x (.map s (.-invert x2) x2))
;;         )
;;         )

;;       ))


;;   x.domain(s.map(x2.invert, x2));
;;   focus.select(".area").attr("d", area);
;;   focus.select(".axis--x").call(xAxis);
;;   svg.select(".zoom").call(zoom.transform, d3.zoomIdentity
;;       .scale(width / (s[1] - s[0]))
;;       .translate(-s[0], 0));
;; }

;; function zoomed() {
;;   if (d3.event.sourceEvent && d3.event.sourceEvent.type === "brush") return; // ignore zoom-by-brush
;;   var t = d3.event.transform;
;;   x.domain(t.rescaleX(x2).domain());
;;   focus.select(".area").attr("d", area);
;;   focus.select(".axis--x").call(xAxis);
;;   context.select(".brush").call(brush.move, x.range().map(t.invertX, t));
;; }

;; function type(d) {
;;   d.date = parseDate(d.date);
;;   d.price = +d.price;
;;   return d;
;; }


(def dimensions {:width 500 :height 500})
(def margin {:top 20 :right 20 :bottom 110 :left 40})
(def margin2 {:top 430 :right 20 :bottom 30 :left 40})
(def width (- (:width dimensions) (+ (:left margin) (:right margin))))
(def height (- (:height dimensions) (+ (:top margin) (:bottom margin))))
(def height2 (- (:height dimensions) (+ (:top margin2) (:bottom margin2))))

(def parseDate (js/d3.timeParse. "%b %Y"))

(def x (.. (.scaleTime. js/d3) (range (clj->js [0 width]))))
(def x2 (.. (.scaleTime. js/d3) (range (clj->js [0 width]))))
(def y (.. (.scaleLinear. js/d3) (range (clj->js [height 0]))))
(def y2 (.. (.scaleLinear. js/d3) (range (clj->js [height2 0]))))

(def xAxis (js/d3.axisBottom x))
(def xAxis2 (js/d3.axisBottom x2))
(def yAxis (js/d3.axisLeft y))

;; (def brush (.. (js/d3.brushX.)
;;                (extent (clj->js [[0 0] [width height2]]))
;;                (on "brush end" brushed)))






(defn d3-inner-plot [data active]
      (reagent/create-class
       {:reagent-render (fn [] [:div.plot
                                [:svg {:width 500 :height 500}
                                 ;; [:defs
                                 ;;  [:clipPath {:id "clip"}
                                 ;;   [:rect {:width clj->js width
                                 ;;           :height clj->js height}]]]
                                 ;; [:g {:class "focus"
                                 ;;      :transform
                                 ;;      (str "translate(" (:left margin)
                                 ;;           ","
                                 ;;           (:top margin)
                                 ;;           ")")}]
                                 ;; [:g {:class "context"
                                 ;;      :transform
                                 ;;      (str "translate(" (:left margin2)
                                 ;;      ","
                                 ;;      (:top margin2)
                                 ;;      ")")}]
                                 ]])

        :component-did-mount (fn [this] ())

        :component-did-update (fn [this] ())

        }))
