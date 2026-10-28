(ns craft.test-runner
  "Test suite runner executing all tests in craft.*"
  (:require [clojure.test :as test]
            [craft.core-player-world-test]
            [craft.inventory-chests-contract-test]
            [craft.builds-safety-contract-test]
            [craft.blueprint-http-contract-test]
            [craft.ui-assets-test]))

(defn -main [& _args]
  (println "\n==================================================")
  (println "🧪 Executando Suíte Completa de Testes Clojure (Fases 1 & 2)")
  (println "==================================================")
  (let [results (test/run-tests 'craft.core-player-world-test
                                'craft.inventory-chests-contract-test
                                'craft.builds-safety-contract-test
                                'craft.blueprint-http-contract-test
                                'craft.ui-assets-test)]
    (println "\n--------------------------------------------------")
    (println (format "Total: %d testes | Falhas: %d | Erros: %d"
                     (:test results 0)
                     (:fail results 0)
                     (:error results 0)))
    (println "--------------------------------------------------")
    (if (and (zero? (:fail results 0)) (zero? (:error results 0)))
      (do (println "✅ TODOS OS TESTES PASSARAM COM SUCESSO!\n")
          (System/exit 0))
      (do (println "❌ OCORRERAM FALHAS NOS TESTES!\n")
          (System/exit 1)))))
