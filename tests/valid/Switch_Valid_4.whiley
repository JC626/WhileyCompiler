int Red = 1
int Blue = 2
int Green = 3

type RGB is (int x) where x == Red || x == Blue || x == Green

function f(RGB c) -> int:
    switch c:
        case Red:
            return 123
        case Switch_Valid_4::Blue:
            return 234
        default:
            return 456

public export method test() :
    assume f(Red) == 123
    assume f(Green) == 456
    assume f(Blue) == 234
