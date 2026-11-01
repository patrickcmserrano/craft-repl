(ns craft.ui-assets-test
  "Verification of static frontend assets, 5-tab structure, and modal dialogs."
  (:require [clojure.test :refer [deftest is testing]]
            [craft.test-helper :as th :refer [with-sandbox-server]]))

(deftest test-ui-static-assets-served
  (with-sandbox-server
    (testing "GET / serves index.html referencing modular css and js"
      (let [{:keys [status raw headers]} (th/http-get "/")]
        (is (= 200 status))
        (is (re-find #"text/html" (first (get headers "content-type" []))))
        (is (re-find #"MC Companion Pro" raw))
        (is (re-find #"/css/dashboard\.css" raw))
        (is (re-find #"/js/api\.js" raw))
        (is (re-find #"/js/radar\.js" raw))
        (is (re-find #"/js/actions\.js" raw))))

    (testing "GET /css/dashboard.css serves valid stylesheet"
      (let [{:keys [status raw headers]} (th/http-get "/css/dashboard.css")]
        (is (= 200 status))
        (is (re-find #"text/css" (first (get headers "content-type" []))))
        (is (re-find #"--surface:" raw))))

    (testing "GET /js/api.js serves javascript module"
      (let [{:keys [status raw headers]} (th/http-get "/js/api.js")]
        (is (= 200 status))
        (is (re-find #"javascript" (first (get headers "content-type" []))))
        (is (re-find #"fetchTelemetry" raw))))

    (testing "GET /js/radar.js serves radar logic"
      (let [{:keys [status raw headers]} (th/http-get "/js/radar.js")]
        (is (= 200 status))
        (is (re-find #"drawRadar" raw))))

    (testing "GET /js/actions.js serves action handlers"
      (let [{:keys [status raw headers]} (th/http-get "/js/actions.js")]
        (is (= 200 status))
        (is (re-find #"renderItemSlotContent" raw))))

    (testing "GET /textures/item/diamond_sword.png serves real Minecraft texture"
      (let [{:keys [status headers]} (th/http-get "/textures/item/diamond_sword.png")]
        (is (= 200 status))
        (is (re-find #"image/png" (first (get headers "content-type" []))))))))

(deftest test-5-tabs-and-modals-structure
  (with-sandbox-server
    (testing "DOM contains the 5 refined tabs and all modal sheets"
      (let [{:keys [raw]} (th/http-get "/")]
        ;; 5 Tabs
        (is (re-find #"id=\"tab-gps\"" raw))
        (is (re-find #"id=\"tab-inventory\"" raw))
        (is (re-find #"id=\"tab-quick\"" raw))
        (is (re-find #"id=\"tab-builds\"" raw))
        (is (re-find #"id=\"tab-vault\"" raw))

        ;; Modals
        (is (re-find #"id=\"modalCustomHouse\"" raw))
        (is (re-find #"id=\"modalItemAction\"" raw))
        (is (re-find #"id=\"modalWaypoint\"" raw))
        (is (re-find #"id=\"modalSkin\"" raw))))))

(deftest test-bluemap-proxy-and-redirects
  (with-sandbox-server
    (testing "GET /map and /bluemap redirect 301 to /map/"
      (let [r1 (th/http-get "/map")
            r2 (th/http-get "/bluemap")]
        (is (= 301 (:status r1)))
        (is (= "/map/" (first (get (:headers r1) "location" []))))
        (is (= 301 (:status r2)))
        (is (= "/map/" (first (get (:headers r2) "location" []))))))

    (testing "GET /map/ proxies BlueMap with proper Content-Type: text/html"
      (let [{:keys [status headers raw]} (th/http-get "/map/")]
        (is (= 200 status))
        (is (re-find #"text/html" (first (get headers "content-type" []))))
        (is (re-find #"BlueMap" raw))))

    (testing "GET /map/assets/ proxies JS with proper JavaScript MIME type for ES modules"
      (let [{:keys [status headers]} (th/http-get "/map/assets/index-b72fc5a8.js")]
        (is (= 200 status))
        (is (re-find #"javascript" (first (get headers "content-type" []))))))))
