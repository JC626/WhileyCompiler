
type tac2ta is ({int f1, int f2} r) where r.f1 < r.f2

type tac2tb is ({int f1, int f2} r) where (r.f1 + 1) < r.f2

function f(tac2tb y) -> tac2tb:
    return y

public export method test() :
    tac2ta x = {f1: 1, f2: 3}
    x.f1 = 2
    f(x)
