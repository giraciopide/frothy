(ns undercover.util)

(defn map-map [m fk fv]
  "Transforms a map applying fk to each key, and fv to each value. In case the transformed keys clash, the results are undefined"
  (into {} (for [[k v] m] [(fk k) (fv v)])))

(defn map-map-vals [m f]
  "Transforms a map applying fv to each value"
  (map-map m identity f))

(defn map-map-keys [m f]
  "Transforms a map applying fk to each key. In case the transformed keys clash, the results are undefined"
  (map-map m f identity))