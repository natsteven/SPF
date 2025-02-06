
(declare-const string_1 String)


(assert (not (= (str.at string_1 0) "p")))

(check-sat)
(get-model)
