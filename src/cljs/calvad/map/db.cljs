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
;;(s/def ::class string?) ;; not sure if necessary, but the class of the shape
(s/def ::data string?) ;; the shape data, from topojson feature call
(s/def ::cell (s/keys :req-un [::cellid ::data])) ;; ::data
(s/def ::grid (s/and                               ;; should use the :kind kw to s/map-of (not supported yet)
                 (s/map-of ::cellid ::cell)             ;; in this map, each todo is keyed by its :text
                 #(instance? PersistentTreeMap %)   ;; is a sorted-map (not just a map)
                 ))
(s/def ::active string?)
(s/def ::land string?) ;; not sure here what to use.  a js structure

(s/def ::db (s/keys :req-un [::grid ::active ::land ]))

;; -- Default app-db Value  ---------------------------------------------------
;;
;; When the application first starts, this will be the value put in app-db

  (def initial-state {:grid (sorted-map);; the map to draw
                      :active "1-2" ;; a fake active cell id
                      :colorkey "sum_vmt"
                      :land nil ;; the land
                      :path nil ;; curse your inevitable betrayal
                    })
