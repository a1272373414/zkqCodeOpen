package com.coc.zkqcode.jar.code.universal.recognizer

/**
 * Pixel font library for digits, harvested from the CURRENT game build.
 *
 * Entry format (note: the legacy header comment says "宽,高" but the data is "高,宽"):
 *
 *     字符|高,宽|64进制点阵串
 *
 * - 64-base alphabet: 0-9 A-Z a-z &# (6 bits per char)
 * - The bitmap is stored row-major, most significant bit first, 高*宽 bits in total
 * - Only the LAST 高*宽 bits are used (leading bits are padding)
 * - bit = 1 means a foreground (ink) pixel
 *
 * Source of the glyphs: the resource-number rows of real 1280x720 screenshots whose
 * filenames carry the exact values, binarized with exactly the criterion PixelFontOcr uses:
 * luminance >= 200 AND saturation (max-min channel) <= 50. The saturation term is required
 * because the numbers are white text on strongly coloured bars, otherwise the bar itself
 * becomes ink and neighbouring digits merge. Regenerate with:
 *   python tools/harvest_pixel_font.py
 *
 * Contains digits 0-9; '.' and '/' still come from the legacy library (see below).
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
/|14,9|m70u30S1WE0m70S3WE0m7
/|13,8|30O60mC1WO30m61WC1
/|14,8|C30O60mC1WO30m61WC3
/|14,10|3W70S0u3W70S0u3W70S0u3W7
/|14,10|3WF0S1u3WF0S1u3WF0U1u3m7
/|14,9|m70O3WC1m70u3WS1mE0u7
/|14,10|3WE0S1m3WE0S1m3WE0S1m3WF
/|14,10|3WE0y1m7WE0y1m7WF0y1u7WF
/|13,9|70O3WC1m60u30S1WE0u3
/|13,8|30mE1WS30u61mC3WO3
/|13,10|E0y1m7WE0y1m7WE0y1m3WF
/|13,9|70y3WS1mE0u7WS3mE0u7
/|13,8|30m61WC30O70mE1WS3
/|11,7|S63WmO631mOE3
/|11,7|S630mOE31WOC7
/|9,6|mGO8C4623
.|2,4|3&
.|3,4|&&
/|11,8|C1WO30m61WC30O7
/|11,7|O630mO630mO61
/|16,11|3W7070E0E0S0S0u0u1u1m3m3W3W707
/|16,11|3W7W70E0E0S0y0u1u1u1m3m3W7W707
/|16,10|E0y1m70E0u1m70E0y1m7WE0y1m3
/|16,11|3W7WF0E0U0S0y0u1u1u3m3m3W7W70F
/|16,10|E0O1m30E0O1m70E0O1m3WE0S1m3
/|16,10|E0O1m70E0u1m70E0u1m7WE0S1m3
/|16,10|E0O1m30E0u1m70E0u1m7WE0S1m2
/|16,12|y0U0U0F0F0707W3m3m1u1u0y0S0U0E0F
/|16,11|3W7WF0F0U0S0y0y1u1u1m3m3W7W70F
/|16,10|E0O1m30E0u1m30E0S1m3WE0S1m3
/|15,9|60u30S1WE0m70O3WC1m70O3
/|15,10|m3W60S0m3W60S0m3W60S0u3W7
/|15,10|m3W60S0m3W60S0m3W60S0u1W7
/|16,10|E0O1m30E0O1m30E0O1m3WE0S1m2
/|15,9|60u30O1WC0m70O3WC1m60u2
/|15,9|60u30O1WC0m70O3WC1m70O3
/|15,9|60u30O1WC0m70O3WC1m70O1
/|15,9|60u30O1WC0m70O3WC1m70O2
/|16,10|E0u1m70E0u1m70E0u1m7WE0S1m2
/|16,10|E0u1m70E0u1m70E0u1m7WE0S1m3
/|16,10|E0O1m30E0u1m30E0S1m3WE0S1m2
/|16,10|E0O1m30E0u1m70E0u1m3WE0S1m2
/|16,10|E0O1m70E0u1m70E0u1m7WE0S1m2
/|16,10|E0O1m70E0u1m70E0u1m3WE0S1m2
/|16,10|E0O1m30E0O1m30E0O1m3WE0S1m3
/|14,9|m30O3WC1m60u30S1W60u3
/|14,9|m30O1WC1m60u30C1W60u3
/|15,11|7W70F0E0U0S0y0u1u1m3m3W7W7W6
/|15,10|y1m7WE0y1m7WE0y1m7WE0y1m3
/|14,10|3W70S0u3W70S0u3W70U0u1m7
/|14,9|m70u3WS1mE0u70S3mE0u7
/|15,9|60u70S3WE1m70u3WU1m70u3
/|14,9|m70O3WC1m60u30S1mE0u3
/|14,9|m70O3WC1m60u30S1mE0u7
/|14,9|m70u3WS1mE0u70S1mE0u7
/|14,9|m70O3WC1mE0u70S1mE0u7
/|14,10|3m70S0u3W70S0u3m70U0u1m7
/|15,10|y1m70E0u1m70E0y1m7WE0S1m3
/|15,9|60u70O3WC1mE0u3WS1mE0u3
/|16,10|C0u3W70S0u1W60C0u1W70C0u1m3
/|16,10|E0O1W30C0O1W30C0u1m70E0u1m3
/|16,9|m60u30O1WE0m70y3WS1m60O3
/|16,10|E0O1W30C0O1W70C0y1m70E0u1m3
/|16,10|C0m3W70S0u3W70S1u3W70C0u1W7
/|16,10|C0u3W70S0u3W60C0u1W70C0u1W7
/|16,10|C0m3W60S0u3W60S1u3WF0S0m1W7
/|12,8|mC1WO30m60mC1WO3
/|12,8|m61WC30O60mC1WS3
/|12,7|mO630WOC31WOE3
/|12,7|mO630mO63Wm863
/|12,7|mOC31WOC31WOE3
/|12,8|mC1WS30O60mC1WS3
/|12,8|m61WC30O20m41W83
/|12,8|m410O20m61WC10O3
/|12,7|mO630mO630mOE3
/|12,8|WC10O20m41WC10O3
/|12,7|mO630mO431WOE3
/|12,8|m61W830m61WC10O3
/|10,6|mGGO8C4621
/|12,8|m60WC10O20m61WC3
/|12,7|GC63WmS63WuV#&
/|12,7|WO430WO430mO61
/|11,7|GC21WGC20WO43
/|12,8|mC1WO30G60mC1WS3
"""
