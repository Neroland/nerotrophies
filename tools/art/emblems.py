"""The 24 mod emblems of the Trophy Book rail: 12x12 glyphs, X body, s shade, a accent."""
from palette import NERO_ALLOY, PLASMA, TEXT_2, TROPHY_ACCENT

EMBLEMS = [
    ("all", TROPHY_ACCENT, """
............
.aaaa..aaaa.
.aXXa..aXXa.
.aXXa..aXXa.
.aaaa..aaaa.
............
............
.aaaa..aaaa.
.aXXa..aXXa.
.aXXa..aXXa.
.aaaa..aaaa.
............"""),
    ("collections", TROPHY_ACCENT, """
....aaaa....
...a....a...
.XXXXXXXXXX.
.X........X.
.X.ss..ss.X.
.X.ss..ss.X.
.XXXXXXXXXX.
.X........X.
.X.ss..ss.X.
.X.ss..ss.X.
.XXXXXXXXXX.
.s........s."""),
    ("nerolandcore", NERO_ALLOY, """
............
....XXXX....
..XXaaaaXX..
.XaaaaaaaaX.
.XaaaXXaaaX.
.XaaXssXaaX.
.XaaXssXaaX.
.XaaaXXaaaX.
.XaaaaaaaaX.
..XXaaaaXX..
....XXXX....
............"""),
    ("nerospace", 0x60D4E8, """
.....XX.....
....XXXX....
....XaaX....
....XaaX....
....XXXX....
....XXXX....
...sXXXXs...
..ssXXXXss..
..s.XXXX.s..
.....aa.....
....a..a....
............"""),
    ("nerotech", 0xFFB347, """
....X..X....
.X.XXXXXX.X.
..XXXXXXXX..
.XXXaaaaXXX.
XXXa....aXXX
.XXa....aXX.
.XXa....aXX.
XXXa....aXXX
.XXXaaaaXXX.
..XXXXXXXX..
.X.XXXXXX.X.
....X..X...."""),
    ("neropower", 0xFFB347, """
......aaaa..
.....aaaX...
....aaaX....
...aaaX.....
..aaaaaaaa..
.....aaaX...
....aaaX....
...aaaX.....
..aaaX......
..aaX.......
.aX.........
............"""),
    ("nerologistics", 0xFFB347, """
............
..XXXXXXXX..
..XssssssX..
..XsXXXXsX..
..XsXaaXsX..
..XsXXXXsX..
..XssssssX..
..XXXXXXXX..
............
aaaaaaaaaaaa
.s..s..s..s.
............"""),
    ("neroeconomy", 0xFFD54A, """
...aaaaaa...
..aaXXXXaa..
.aaXaaaaXaa.
.aXaaXXaaXa.
.aXaXaaaaXa.
.aXaaXXaaXa.
.aXaaaaXaXa.
.aXaaXXaaXa.
.aaXaaaaXaa.
..aaXXXXaa..
...aaaaaa...
............"""),
    ("nerofactions", 0xD6424A, """
.XX.........
.XXaaaaaaaa.
.XXaaaaaaaa.
.XXaaXXaaaa.
.XXaaXXaaaa.
.XXaaaaaaaa.
.XXaaaaaaa..
.XXaaaa.a...
.XXaa.......
.XX.........
.XX.........
.ss........."""),
    ("nerocolonies", 0xE48C42, """
.....X.aa...
.....Xaaa...
.....X.aa...
....XXX.....
...XXXXX....
..XXXXXXX...
.XXXXXXXXX..
..XsssssX...
..XsXXXsX...
..XsXaXsX...
..XsXaXsX...
..XXXXXXX..."""),
    ("nerosecurity", 0xD6424A, """
.XXXXXXXXXX.
.XaaaaaaaaX.
.XaaaXXaaaX.
.XaaXXXXaaX.
.XaaaXXaaaX.
.XaaaXXaaaX.
.XaaaXXaaaX.
..XaaaaaaX..
..XaaaaaaX..
...XaaaaX...
....XaaX....
.....XX....."""),
    ("neroevents", 0xD6424A, """
..X......X..
.XXXXXXXXXX.
.XssssssssX.
.XXXXXXXXXX.
.X........X.
.X...aa...X.
.X.aaaaaa.X.
.X..aaaa..X.
.X..a..a..X.
.X........X.
.XXXXXXXXXX.
............"""),
    ("neroquests", 0xFFD54A, """
..XXXXXXXX..
.XssXXXXXXX.
.XXXXXXXXsX.
..XXXXXXXX..
..XaaaaaaX..
..XXXXXXXX..
..XaaaaaXX..
..XXXXXXXX..
..XaaaaaaX..
.XXXXXXXXX..
.XsXXXXXXXX.
..XXXXXXXX.."""),
    ("nerocreatures", 0x78D678, """
............
..aa....aa..
..aa.aa.aa..
.....aa.....
.aa......aa.
.aa.XXXX.aa.
...XXXXXX...
..XXXXXXXX..
..XXXXXXXX..
..XXXssXXX..
...XX..XX...
............"""),
    ("neroagriculture", 0x78D678, """
............
.aa......aa.
.aaa....aaa.
.aaaa..aaaa.
..aaaaaaaa..
...aaXXaa...
.....XX.....
.....XX.....
.....XX.....
..ssssssss..
.ssssssssss.
............"""),
    ("nerocompanion", 0x56CCC4, """
XXXX....XXXX
.ss......ss.
.ss......ss.
..XXXXXXXX..
.XXXXXXXXXX.
.XXXaaaaXXX.
.XXaaXXaaXX.
.XXaaXXaaXX.
.XXXaaaaXXX.
..XXXXXXXX..
...ss..ss...
............"""),
    ("neroruins", 0xC89858, """
...XXXXXX...
..XXXXXXXa..
.XXXss..XXa.
.XXs.....a..
.XXs........
.XXs......a.
.XXs.....XX.
.XXs.....XX.
.XXs.....XX.
.XXs....sXX.
XXXXs..sXXXX
aaaa....aaaa"""),
    ("nerodecor", 0xE8788E, """
.XXXXXXXXX..
.aaaaaaaaaX.
.aaaaaaaaaX.
.aaaaaaaaaX.
.XXXXXXXXX..
.........X..
....XXXXXX..
....X.......
....ss......
....ss......
....ss......
....ss......"""),
    ("neronotes", 0xFF3FA4, """
.....aaaaaa.
.....aaaaaa.
.....X....X.
.....X....X.
.....X....X.
.....X....X.
.....X....X.
..XXXX.XXXX.
.XXXXX.XXXX.
.XXXXX.XXX..
..XXX.......
............"""),
    ("neroarcade", 0xFF3FA4, """
.....aa.....
....aaaa....
....aaaa....
.....aa.....
.....XX.....
.....XX.....
.....XX.....
..XXXXXXXX..
.XXXXXXXaaX.
.XssssssssX.
.XXXXXXXXXX.
............"""),
    ("nerolink", 0xE8788E, """
............
.XXXXXX.....
X......X....
X...aaaaaa..
X..a...X..a.
X..a...X..a.
X..a...X..a.
.XXaXXX...a.
...a......a.
....aaaaaa..
............
............"""),
    ("neromaps", PLASMA, """
............
.XX..XXX..X.
.XXXXXXXXXX.
.XsXXaXXsXX.
.XsXaXXXsXX.
.XsXXaXXsXX.
.XsXXXaXsXX.
.XsXXaXXsXX.
.XsXaaXXsXX.
.XXXXXXXXXX.
.X..XXX..XX.
............"""),
    ("nerotrophies", TROPHY_ACCENT, """
....aaaa....
...aaaaaa...
...aaaaaa...
....aaaa....
............
...XXXXXX...
..XXXXXXXX..
.XXXXXXXXXX.
.XssssssssX.
.XsaaaaaasX.
.XXXXXXXXXX.
............"""),
    ("other", TEXT_2, """
............
...XXXXXX...
..XXXXXXXX..
.XXXXXXXXXX.
.XXXXXXXXXX.
.XXXXXXXXXX.
.XXXXXXXXXX.
.XXssssssXX.
..XssssssX..
...ssssss...
............
............"""),
]

for _name, _acc, _art in EMBLEMS:
    _rows = _art.strip("\n").split("\n")
    assert len(_rows) == 12 and all(len(r) == 12 for r in _rows), _name
