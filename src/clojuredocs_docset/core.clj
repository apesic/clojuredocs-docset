(ns clojuredocs-docset.core
  (:require [clojure.java.io :refer [file resource]]
            [clojure.java.jdbc :as j]
            [clojure.java.shell :refer [sh]]
            [clojure.string :as string])
  (:import [org.apache.commons.io FileUtils]
           [org.jsoup Jsoup])
  (:gen-class))

(def user-dir (System/getProperty "user.dir"))

(def db-file-path "clojure-docs.docset/Contents/Resources/docSet.dsidx")
(def index-html-path "clojure-docs.docset/Contents/Resources/Documents/clojuredocs.org/core-library/vars.html")
(def docset-template "clojure-docs.docset/Contents/Resources/Documents")
(def mirror-path "clojuredocs.org")

(defn file-ref [file-name]
  (file user-dir file-name))

(defn resource-copy [src dest]
  (FileUtils/copyURLToFile (resource src) (file-ref dest)))

(def sqlite-db {:classname "org.sqlite.JDBC"
                :subprotocol "sqlite"
                :subname db-file-path})

(defn print-progress [percent text]
  (let [x (int (/ percent 2))
        y (- 50 x)]
    (print "\r" (apply str (repeat 100 " "))) ; Clear existing text
    (print "\r" "["
           (apply str (concat (repeat x "=") [">"] (repeat y " ")))
           (str "] " percent "%") text)
    (when (= 100 percent) (println))
    (flush)))

(defn mirror-clojuredocs []
  (print-progress 15 "Mirroring clojuredocs.org/clojure.core")
  (apply sh ["wget" "--mirror" "--convert-links" "--adjust-extension" "--page-requisites"
             "--no-parent" "https://clojuredocs.org/clojure.core"]))

(defn create-docset-template []
  (print-progress 40 "Creating docset template")
  (.mkdirs (file-ref docset-template))
  (resource-copy "icon.png" "clojure-docs.docset/icon.png")
  (resource-copy "Info.plist" "clojure-docs.docset/Contents/Info.plist"))

(defn copy-html-to-docset []
  (print-progress 50 "Copying clojuredocs.org to docset")
  (FileUtils/copyDirectoryToDirectory (file-ref mirror-path) (file-ref docset-template)))

(defn clear-search-index []
  (print-progress 60 "Clearing index")
  (j/with-connection sqlite-db
    (j/do-commands "DROP TABLE IF EXISTS searchIndex"
                   "CREATE TABLE searchIndex(id INTEGER PRIMARY KEY, name TEXT, type TEXT, path TEXT)"
                   "CREATE UNIQUE INDEX anchor ON searchIndex (name, type, path)")))

(defn populate-search-index [rows]
  (j/with-connection sqlite-db
    (apply j/insert-records :searchIndex rows)))

(defn search-index-attributes [element]
  {:name (.text element)
   :type "Function"
   :path (str "clojuredocs.org/" (string/replace (.attr element "href") #"\.\." ""))})

(defn generate-search-index []
  (print-progress 75 "Generating index")
  (let [html-content (slurp (str user-dir "/" index-html-path))
        document (Jsoup/parse html-content)
        rows (map search-index-attributes (.select document ".var-list a"))]
    (populate-search-index rows)))

(defn generate-docset []
  (mirror-clojuredocs)
  (create-docset-template)
  (copy-html-to-docset)
  (clear-search-index)
  (generate-search-index)
  (print-progress 100 "Done."))

(defn -main
  [& args]
  ;; work around dangerous default behaviour in Clojure
  (alter-var-root #'*read-eval* (constantly false))
  (generate-docset)
  (shutdown-agents))
