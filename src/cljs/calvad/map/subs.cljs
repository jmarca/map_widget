(ns calvad.map.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [reg-sub subscribe]]))

;;---- Subscription handlers-----------

;; two step registration of a subscription
(defn sorted-grids
  [db _]
  (:grid db))

;; sorted grids subscription gets all the grids
(reg-sub :sorted-grids sorted-grids)

;; grid ids gets just the ids for the sorted grids, so you can get the
;; grid data only
(reg-sub
 :grid-ids
  ;; Although not required in this example, it is called with two paramters
  ;; being the two values supplied in the originating `(subscribe X Y)`.
  ;; X will be the query vector and Y is an advanced feature and out of scope
  ;; for this explanation.
 (fn [query-v _]
   (subscribe [:sorted-grids]))    ;; returns a single input signal

  ;; This 2nd fn does the computation. Data values in, derived data
  ;; out.  It is the same as the two simple subscription handlers up
  ;; at the top.  Except they took the value in app-db as their first
  ;; argument and, instead, this function takes the value delivered by
  ;; another input signal, supplied by the function above: (subscribe
  ;; [:sorted-letters]) Subscription handlers can take 3 parameters: -
  ;; the input signals (a single item, a vector or a map) - the query
  ;; vector supplied to query-v (the query vector argument to the
  ;; "subscribe") and the 3rd one is for advanced cases, out of scope
  ;; for this discussion.

 ;; but query isn't used here
 (fn [sorted-grids query-v _]
    (keys sorted-grids)))

;; this subscription gets individual grid cells.  Here query *is* used
(reg-sub
 :grid
 (fn [query-v _]
   (subscribe [:sorted-grids]))
 (fn [sorted-grids [_ query-v] _]
   (get sorted-grids query-v)))

;; this subscription gets topojson from db
(reg-sub
 :land
  (fn [db _]        ;; db is the value in app-db
   (:land db)))   ;; a value, not a ratom, not a reaction.

;; this subscription gets path from db
(reg-sub
 :path
  (fn [db _]        ;; db is the value in app-db
   (:path db)))   ;; a value, not a ratom, not a reaction.

;; this subscription gets which cell is active
(reg-sub
 :active
  (fn [db _]        ;; db is the value in app-db
   (:active db)))   ;; a value, not a ratom, not a reaction.

;; this subscription gets loading or not
(reg-sub
 :loading
  (fn [db _]        ;; db is the value in app-db
   (:loading? db)))   ;; a value, not a ratom, not a reaction.
