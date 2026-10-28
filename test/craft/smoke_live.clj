(ns craft.smoke-live
  "Safe read-only smoke tests against live Minecraft Dashboard (port 8090)."
  (:require [craft.schemas :as schemas]
            [craft.test-helper :as th]))

(defn -main [& _args]
  (println "\n==================================================")
  (println "🔎 Executando Smoke Tests Read-Only contra Porta 8090")
  (println "==================================================")
  (let [t-resp (th/http-get 8090 "/api/telemetry")
        inv-resp (th/http-get 8090 "/api/inventory?player=pace")
        wp-resp (th/http-get 8090 "/api/waypoints")
        map-resp (th/http-get 8090 "/map")
        skin-resp (th/http-get 8090 "/skins/pace.png")]
    
    (println (format "1. Telemetria: status %d | Servidor online: %s"
                     (:status t-resp) (get-in t-resp [:body :server_online])))
    (assert (= 200 (:status t-resp)) "Telemetria deve retornar 200")
    (assert (schemas/valid? schemas/TelemetryResponse (:body t-resp)) "Telemetria deve respeitar schema Malli")

    (println (format "2. Inventário Pace: status %d | Itens totais: %d"
                     (:status inv-resp) (get-in inv-resp [:body :total_items])))
    (assert (= 200 (:status inv-resp)) "Inventário deve retornar 200")
    (assert (schemas/valid? schemas/InventoryResponse (:body inv-resp)) "Inventário deve respeitar schema Malli")

    (println (format "3. Waypoints: status %d | Total cadastrado: %d"
                     (:status wp-resp) (count (:body wp-resp))))
    (assert (= 200 (:status wp-resp)) "Waypoints deve retornar 200")

    (println (format "4. Redirect /map: status %d | Location: %s"
                     (:status map-resp) (get-in map-resp [:headers "location"])))
    (assert (= 301 (:status map-resp)) "Map deve retornar 301")

    (println (format "5. Skin Pace: status %d | Content-Type: %s"
                     (:status skin-resp) (get-in skin-resp [:headers "content-type"])))
    (assert (= 200 (:status skin-resp)) "Skin deve retornar 200")

    (println "\n✅ SMOKE TESTS READ-ONLY PASSARAM COM SUCESSO!\n")
    (System/exit 0)))
