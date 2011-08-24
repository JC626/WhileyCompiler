import whiley.lang.*:*

define expr as [int]|int
define tup as {expr lhs, int p}

string f(tup t):
    if t.lhs is [int] && |t.lhs| > 0 && t.lhs[0] == 0:
        return "MATCH" + str(t.lhs)
    else:
        return "NO MATCH"

void ::main(System sys,[string] args):
    sys.out.println(f({lhs:[0],p:0}))
    sys.out.println(f({lhs:[1],p:0}))
    sys.out.println(f({lhs:[],p:0}))
