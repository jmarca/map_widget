(ns calvad.map.events
  (:require
    [clojure.string :as str]
            [clojure.set :as set]
    [calvad.map.db    :refer [initial-state]]
    [re-frame.core :refer [reg-event-db reg-event-fx inject-cofx path trim-v
                           after debug dispatch]]
    [cljs.spec     :as s]))

;; -- Interceptors --------------------------------------------------------------
;;

(defn check-and-throw
  "throw an exception if db doesn't match the spec."
  [a-spec db]
  (when-not (s/valid? a-spec db)
    (throw (ex-info (str "spec check failed: " (s/explain-str a-spec db)) {}))))


;; Event handlers change state, that's their job. But what happens if there's
;; a bug which corrupts app state in some subtle way? This interceptor is run after
;; each event handler has finished, and it checks app-db against a spec.  This
;; helps us detect event handler bugs early.
(def check-spec-interceptor (after (partial check-and-throw :calvad.map.db/db)))


;; the chain of interceptors we use for all handlers that manipulate letters
(def letter-interceptors [;;check-spec-interceptor               ;; ensure the spec is still valid
                          (path :letters)                        ;; 1st param to handler will be the value from this path
                          ;;->local-store                        ;; write todos to localstore
                          debug                                  ;; look in your browser console for debug logs
                          trim-v])                               ;; removes first (event id) element from the event vec

;; the chain of interceptors we use for all handlers that manipulate the alphabet
(def alphabet-interceptors [;;check-spec-interceptor             ;; ensure the spec is still valid
                          (path :alphabet)                       ;; 1st param to handler will be the value from this path
                          ;;->local-store                        ;; write todos to localstore
                          debug                                  ;; look in your browser console for debug logs
                          trim-v])                               ;; removes first (event id) element from the event vec



;; -- Helpers -----------------------------------------------------------------

;; helpful definition of the alphabet I'm using
(def alphabet (rest (str/split "abcdefghijklmnopqrstuvwxyz" #"")))

;; not actually using this.  perhaps someday when letters duplicate in
;; an arbitrary string, but for now just using the letter itself as
;; the letter object's id
(defn allocate-next-id
  "Returns the next letter's id.
  Just increments forever.  All new letters get a unique id."
  []
  (let [my-atom (atom 0)]
       (swap! my-atom inc)
       ))

;; -- Event Handlers ----------------------------------------------------------

(reg-event-db                 ;; setup initial state
  :initialize-db                     ;; usage:  (dispatch [:initialize-db])
;;  [check-spec-interceptor]
  (fn
    [db _]
    (merge db initial-state)))    ;; what it returns becomes the new state

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
 [(path :alphabet) trim-v ];;check-spec-interceptor]
 (fn [oldalpha [newalpha]]
   ;; 1st argument is coeffects, instead of db
   ;; endeavor to make a list of letter updates to dispatch
   (println "handling " newalpha " removing " oldalpha)
   (let [incoming newalpha
         outgoing (:db oldalpha)
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
         disp (if (> (count exits) 0 )
                {:dispatch-n (concat (vector [:alphabet  newalpha])
                                     (concat (mapv (fn [d] (vector :letter-update d )) update-group)
                                             (mapv (fn [d] (vector :letter-exit d )) exits)
                                     ))}
                {:dispatch-n (concat (vector [:alphabet  newalpha])
                                     (mapv (fn [d] (vector :letter-update d )) update-group)
                                     ;;(map (fn [d] (vector :letter-exit d )) exits)
                                     )}
                )

         ]
     (println "updates" update-group)
     (println "exits" exits)
     (println disp)
     disp)))

;; shuffle creates a new alphabet all at once
;; I think using (clj->js ...) is needed for js/d3, and it doesn't
;; seem to affect the end result...the alphabet still works

(reg-event-fx
  :shuffle
;;  [check-spec-interceptor]
  (fn
    [db [_ ]]
    (let [lettres (random-sample
                   0.5
                   (.shuffle js/d3 (clj->js
                   alphabet
                   ))
                   )]
      (println lettres)
      (dispatch [:alphabet-letters lettres])
      ))
  )
