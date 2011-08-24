import whiley.lang.*:*

void f([int] x) requires |x| > 0:
    y = x[0]
    z = x[0]
    assert y == z

void ::main(System sys,[string] args):
    arr = [1,2,3]
    f(arr)
    sys.out.println(str(arr[0]))
