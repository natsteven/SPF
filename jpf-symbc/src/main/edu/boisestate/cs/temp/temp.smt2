
(declare-const var_1_1 String)

(assert (not (= var_1_1 "Hello, World!")))


(check-sat)
(get-model)
