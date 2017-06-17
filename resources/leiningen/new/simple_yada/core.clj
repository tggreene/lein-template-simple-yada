(ns {{name}}.core
  (:require [schema.core :as s]
            [yada.yada :as yada]
            [taoensso.timbre :as timbre]))

(timbre/set-config! {:level :info})

(def server (atom nil))

(defn widget-resource
  [initial-value]
  "simple modifiable widget. look at yada.resources.atom-resource for ways to add
  last-modified headers and the like"
  (let [value (atom initial-value)]
    (yada/resource
      {:methods {:get  {:produces "application/json"
                        :response (fn [ctx] @value)}
                 :post {:parameters {:body {:sprockets               s/Num
                                            :reaction                s/Str
                                            (s/optional-key :extras) s/Any}}
                        :consumes   "application/json"
                        :response   (fn [ctx]
                                      (let [body (get-in ctx [:parameters :body])]
                                        (reset! value body)))}}})))

(defn routes
  []
  ["/"
   {"hello"             (yada/as-resource "Hello World!")
    "json"              (yada/as-resource {:message "yo!"})
    "modifiable-string" (yada/as-resource (atom "original value"))
    "widget"            (widget-resource {:sprockets 3
                                          :reaction  "wow"})
    "die"               (yada/as-resource (fn []
                                            (future (Thread/sleep 100) (@server))
                                            "shutting down in 100ms..."))}])
(defn run
  "Returns promise to capture lifecycle of server"
  []
  (let [listener     (yada/listener (routes) {:port 3000})
        done         (promise)]
    (reset! server (fn []
                     ((:close listener))
                     (deliver done :done)))
    done))

(defn -main
  [& args]
  (let [done (run)]
    (println "server running on port 3000... GET \"http://localhost:3000/die\" to kill")
    @done))

(comment
  "to run in a repl, eval this:"
  (def server-promise (run))
  "then either wait on the promise:"
  @server-promise
  "or with a timeout"
  (deref server-promise 1000 :timeout)
  "or close it yourself"
  (@server))


