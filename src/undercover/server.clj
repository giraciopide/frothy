(ns undercover.server)

(defn make-uuid [] (str (java.util.UUID/randomUUID)))

;; the fucking gargantuan server state
(defonce state (atom {
                      :rooms {} ;; name -> set of id
                      :chans #{} 
                      :chan-by-id {}  ;; id -> channel
                      :nick-by-id {}     ;; name -> id
                      :id-by-nick {}     ;; id -> name 
                    }))

(defn add-user-channel
	[state uuid nick channel]
	(assoc state
		:chan-by-id (assoc (:chan-by-id state) uuid channel)
		:nick-by-id (assoc (:nick-by-id state) nick uuid)
		:id-by-nick (assoc (:id-by-nick state) uuid nick)))

(defn remove-user-channel)

(defn add-user-channel! 
	[nick channel]
	(let [uiid (make-uuid)]
		(swap! state )))