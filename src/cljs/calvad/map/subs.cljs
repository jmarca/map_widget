(ns calvad.map.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [reg-sub subscribe]]))

;;---- Subscription handlers-----------

(reg-sub
  :alphabet
  (fn
    [db _]
    (reaction (:alphabet @db))))

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
 (fn [sorted-letters query-v _]
    (keys sorted-letters)))

;; this subscription gets individual letters.  Here query is used
(reg-sub
 :letter
 (fn [query-v _]
   (subscribe [:sorted-letters]))
 (fn [sorted-letters [_ query-v] _]
   (get sorted-letters query-v)))
