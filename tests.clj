(ns iokov.tests
  (:require [clojure.test :refer [deftest is]]
            [iokov.view :refer [render render-flow]]
            [iokov.core :refer [iokov]]
            [iokov.handlers :refer [callback-handler promise-handler time-handler log-handler lift]]))

(deftest sync-tests
  (is (= 42 (iokov {:a 42})))
  (is (= 42 (iokov {:a 42} [:alts :a :b (constantly {:b 42})])))
  (is (= 6 (iokov {:a 42}
                  [:alts :a :b (constantly {:b 1})]
                  [:alts :a :c (constantly {:c 2})]
                  [:alts :a :d (constantly {:d 3})]
                  [:join [:b :c :d] :end (fn[m]{:end (apply + (vals m))})]))))

(deftest async-tests-callback
  (let [done (promise)]
    (iokov {:a 42}
           [:alts :a :b (fn[{:keys [a]} cb] (cb {:b a})) callback-handler]
           [:alts :b :end (fn[m](deliver done m) {:end nil})])
    (is (= {:b 42} @done))))

(deftest async-tests-promise
  (let [done (promise)]
    (iokov {:a 42}
           [:alts :a :b (fn[{:keys [a]}]
                          (let [p (promise)]
                            (deliver p {:b a})
                            p)) promise-handler]
           [:alts :b :end (fn[m](deliver done m) {:end nil})])
    (is (= {:b 42} @done))))

;;TODO: Async job + sync after = first should not block the whole chain
;;TODO: When certain keys get available all dependend rules should be executed sequenteally before making another iteration

(deftest lift-has-to-provide-all-keys
  (is (= 42
         (iokov {:b 42}
                [:alts [:a :b] :c #(or %1 %2) lift]))))

(deftest nil-values
  (is (= (iokov {:a nil}
                [:atls :a :b str lift])
         ""))
  (is (= (iokov {:a nil :b nil}
                [:join [:a :b] :c (comp count list) lift])
         2)))

(comment
  (render-flow [[:a [:b :c]]
                [:b :d]
                [:c :e]
                [[:d :e] :z]])

  nil


  (render-flow [[:request :user-id]
                [:user-id :orders-service-req]
                [:orders-service-req :orders]
                [:orders :last-order-id]
                [:last-order-id :user-and-order]
                [:user-and-order :response]])

  (render-flow [[:request [:timeout :query]]
                [:query [:user-id-missing :user-id-not-valid :user-id]]
                [:timeout :response-timeout]
                [:user-id :orders-service-req]
                [:orders-service-req [:orders-req-err :orders-req-timeout :orders]]
                [:orders [:last-order-id :no-orders-err]]
                [[:user-id :last-order-id] :user-and-order]
                [[:user-id-missing :user-id-not-valid] :response-bad-request]
                [[:orders-req-err :orders-req-timeout :no-orders-err] :response-server-error]
                [[:response-timeout :response-bad-request :response-server-error :user-and-order] :response]])

  (render [[:fork :request [:timeout :query]]
           [:alts :query [:user-id-missing :user-id-not-valid :user-id]]
           [:alts :timeout :response-timeout]
           [:alts :user-id :orders-service-req]
           [:alts :orders-service-req [:orders-req-err :orders-req-timeout :orders]]
           [:alts :orders [:last-order-id :no-orders-err]]
           [:join [:user-id :last-order-id] :user-and-order]
           [:alts [:user-id-missing :user-id-not-valid] :response-bad-request]
           [:alts [:orders-req-err :orders-req-timeout :no-orders-err] :response-server-error]
           [:alts [:response-timeout :response-bad-request :response-server-error :user-and-order] :response]])

  (render [[:alts :orders [:no-orders-err :last-order-id]]]))
