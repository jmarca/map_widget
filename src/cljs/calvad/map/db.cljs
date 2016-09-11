(ns calvad.map.db
  (:require [cljs.reader]
            [cljs.spec :as s]
            [re-frame.core :as re-frame]))

;; -- Spec --------------------------------------------------------------------
;;
;; This is a clojure.spec specification for the value in app-db. It is like a
;; Schema. See: http://clojure.org/guides/spec
;;
;; The value in app-db should always match this spec. Only event handlers
;; can change the value in app-db so, after each event handler
;; has run, we re-check app-db for correctness (compliance with the Schema).
;;
;; How is this done? Look in events.cljs and you'll notice that all handers
;; have an "after" interceptor which does the spec re-check.
;;
;; Technique borrowed from re-frame examples.  None of this is
;; strictly necessary. It could be omitted. But we find it good
;; practice.

(s/def ::i int?)
(s/def ::x int?)
(s/def ::y int?)
(s/def ::class string?)
(s/def ::letter (s/keys :req-un [::class ::y ::x ::text ::i ::fill-opacity]))
(s/def ::letters (s/and                               ;; should use the :kind kw to s/map-of (not supported yet)
                 (s/map-of ::text ::letters)             ;; in this map, each todo is keyed by its :text
                 #(instance? PersistentTreeMap %)   ;; is a sorted-map (not just a map)
                 ))
(s/def ::alphabet (s/coll-of string? []));; :kind vector? :distinct true))     ;; keep track of whole string here


(s/def ::db (s/keys :req-un [::letters ::alphabet]))

;; -- Default app-db Value  ---------------------------------------------------
;;
;; When the application first starts, this will be the value put in app-db

(def initial-state {:letters (sorted-map) ;; the letters to display
                    :alphabet [] ;; the same as above, but easier to manage
                    })
