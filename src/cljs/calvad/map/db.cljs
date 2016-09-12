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

(s/def ::i int?) ;; i of cell
(s/def ::j int?) ;; j of cell
(s/def ::cellid string?) ;; cell id, equal to the i and j
(s/def ::class string?) ;; not sure if necessary, but the class of the shape
(s/def ::d string?) ;; the shape string
(s/def ::cell (s/keys :req-un [::i ::j ::cellid ::d]))
(s/def ::grid (s/and                               ;; should use the :kind kw to s/map-of (not supported yet)
                 (s/map-of ::cellid ::cell)             ;; in this map, each todo is keyed by its :text
                 #(instance? PersistentTreeMap %)   ;; is a sorted-map (not just a map)
                 ))
(s/def ::db (s/keys :req-un [::grid]))

;; -- Default app-db Value  ---------------------------------------------------
;;
;; When the application first starts, this will be the value put in app-db

(def initial-state {:grid ;; the map to draw
                    })
