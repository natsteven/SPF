
(declare-const var_1_1 String)

(assert (= var_1_1 "Hello, World!"))


(check-sat)
(get-model)
