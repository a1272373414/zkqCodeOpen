package com.coc.zkqcode.jar.code.universal.recognizer

/**
 * Pixel font library for in-game numbers (digits and separators).
 *
 * Entry format: "char|height,width|base-64 bitmap" (row-major, MSB first, bit=1=ink;
 * only the last h*w bits are used). Digits are harvested from the resource rows of
 * labeled 1280x720 screenshots (tools/harvest_pixel_font.py); the separators
 * ("/", ".") come from tools/harvest_slash.py so they match the CURRENT build font
 * (the old-font legacy "/" shapes scored ~0.6 and made "used/total" read as "340?340").
 */
internal const val PIXEL_FONT_DIGITS: String = """
1|15,6|2##UUUUUUUUUUUU
8|15,13|3#n#z#&&VVFldZ#W#u#&&V&7#3##x#y#y
9|15,12|FyV&#&#&&UyV######7#0V0&F&VyVW
4|15,13|0V0FuFyF#7ldtppxvzy&######u3m1u0y
2|15,10|Vx###my3mV3y#ly#3uFW#####
0|15,14|F#7#x#&###X#uV&7#X#uV&7#X###V#d#mFm
1|15,6|6###VVVVUUUUUUU
3|15,11|3&F#V&1y3u7m&3&7&0&1y7x#l#Vy
9|15,12|7uV&V&#&yUyU######3#0U0&F&VyVW
4|15,13|0F0FuFy7&7ldtppxvzy&######u3m1u0y
0|15,14|F#7#x#&###X#uV&7#X#uV&7#X#z&V#d#m70
4|15,13|0E0FuFy7&7l7dZppvvy&######m3m1u0y
0|14,14|3#n#y##lxxuV&7#X#uV&7#X#uV#Vd#v#y
5|16,11|0Vt#l#VWy1u3y7#l#W#0&3u#t#7u80
2|15,10|Fx###Wy3mV3y#ly#3uFW#####
3|15,11|3&7#V&1y3u7W&3&3&0&1y3v#l#Vu
7|15,10|#####1y7m&3uV1yFW&3mV1u7W
5|16,11|0Vt#l#VWy1u3y7#l#W#0&3y#t#7u80
6|15,12|7yV&V&#0&0&0#y####yFyFyF###&Fy
8|15,13|1#n#u#&&VUFldZ#W#u#&&F&7#3#px#y#y
2|15,10|Fx###0y3mV3y#dy#3uFW#####
1|15,6|2##VUUUUUUUUUUE
3|16,11|1#7#l#0&1y3uVX#3#WV0&3z#t#l&80
1|15,6|6##VVVUUUUUUUUU
6|15,12|7&V&V&#0&0&0#y####&VyFyF###&Fy
5|16,11|0Ft#l#V0y1u3y7#l#W#0&3y#t#7u80
6|15,12|3yV&V&#0&0&0#y####yFyFyF###&Fy
9|15,12|7mVyV&#&yUyU#&#&##0&0U0U7&VyFW
0|14,14|3#n#y##lpxuU&7#X#uV&7#X#uV&Vd#v#y
8|15,13|1#n#y#&&VUFldZ#W#u#&&V&7#3#tx#y#y
3|16,11|1#7#l#0&1y3uVX#3#WV0&3##t#l&80
6|15,12|7&V&V&#W&0&0#y####&VyF&F###&Fy
5|16,11|0#t#l#Vmy1u3y7#l#X#0&3z#t#7u80
6|15,12|3yV&V&V0&0&0#u####yFyFyF###&Fy
9|15,12|7uVyV&#&yUyU#&####1#0U0U7&VyVW
0|14,14|3#n#y##lpxuV&7#X#uV&7#X#uV#Vd#v#y
1|15,6|2##VUUUUUUUUUUU
0|14,14|3#n#y##lx#uV&7#X#uV&7#X#uV#Vd#v#y
2|15,10|Fx###0y3mF3y#dy#3uFW#####
5|16,11|0#t#l#Vu&1u3&7#l#X#0&3z#t#luC0
1|15,6|2##UUUUUUUUUUUE
9|15,12|7uVyV&#&yUyU######3#0V0U7&VyFW
5|16,11|0Ft#l#VWy1u3y7#l#W#0&3v#t#7u80
6|15,12|7&V&V&#0&0&0#y####yFyFyF###&Fy
9|15,12|7uVyV&#&yUyU#&####1#0U0UF&VyFW
0|15,14|F#7#p#&#FlX#uV&7#X#uV&7#X#z&V#d#m10
5|16,11|0Vt#l#V0y1u3y7#l#W#0&3u#t#7u80
9|15,12|7mVyV&#&yUyU#&####1#0U0U7&VyVW
0|15,14|F#7#p#&#FlX#uV&7#X#uV&7#X#z&V#d#m20
1|15,6|6###VVVUUUUUUUU
4|15,13|0E0FuFy7&7l7dZppvzy&######m3m1u0y
1|14,5|F#UzxtlUzxtd
0|15,14|F#7#p#&#llX#uV&7#X#uV&7#X#z&V#d#m10
8|15,13|1#n#u#&&VUFldZ#W#u#&yF&7#3#px#y#y
8|15,13|1#n#z#&&VUFldZ#W#u#&&V&7#3#px#y#y
8|15,13|1#n#u#&&VUFldZ#W#u#&&V&7#3#px#y#y
7|15,10|V####1y7m&3uV1yFW&3mV1u7W
0|15,14|F#7#p#&#FlX#uV&7#X#uV&7#X#z&V#d#m30
8|15,13|0#X#u#&&VUFldZ#W#u#&yF&7#3#px#y#y
.|2,4|3&
.|3,4|&&
/|13,9|071uE3mS7XuF3mU3Wy30
/|13,9|0F1mU7WyF1uU3mS7Wu60
/|14,10|03mE1u70y7WU3mF1u7WS3mE0
"""
