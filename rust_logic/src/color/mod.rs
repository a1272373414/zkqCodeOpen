pub mod multi_colors;
pub mod multi_colors_raw;

#[inline(always)]
pub fn is_color_match(pixel: u32, target_color: u32, threshold: i32) -> bool {
    // pixel (RGBA little endian 0xAABBGGRR)
    // targetColor (Java ARGB: 0xAARRGGBB)

    let pr = (pixel & 0xFF) as i32;
    let pg = ((pixel >> 8) & 0xFF) as i32;
    let pb = ((pixel >> 16) & 0xFF) as i32;

    let tr = ((target_color >> 16) & 0xFF) as i32;
    let tg = ((target_color >> 8) & 0xFF) as i32;
    let tb = (target_color & 0xFF) as i32;

    (pr - tr).abs() <= threshold
        && (pg - tg).abs() <= threshold
        && (pb - tb).abs() <= threshold
}

pub fn find_multi_colors_internal<F>(
    width: i32,
    height: i32,
    mut x1: i32,
    mut y1: i32,
    mut x2: i32,
    mut y2: i32,
    main_color: u32,
    threshold: i32,
    offsets_arr: &[i32],
    direction: i32,
    get_pixel: F,
) -> Option<(i32, i32)>
where
    F: Fn(i32, i32) -> u32,
{
    // Boundary check for search area
    x1 = x1.max(0);
    y1 = y1.max(0);
    x2 = x2.min(width - 1);
    y2 = y2.min(height - 1);

    // 方向语义对齐源 `函数24a/26a` 的 触动精灵 findMultiColor（degree）：
    // 0=左上→右下(默认, x↑y↑) / 1=左下→右上(x↑y↓) / 2=右上→左下(x↓y↑) /
    // 3=右下→左上(x↓y↓) / 4=中心向四周 / 5=四周向中心。
    // 早期实现只支持 0/1 且 dir=1 误作「右下→左上」，此处修正并补全 2/3/4/5。
    match direction {
        // 1 = 左下→右上（x 递增，y 递减）
        1 => {
            for y in (y1..=y2).rev() {
                for x in x1..=x2 {
                    if try_match(x, y, width, height, main_color, threshold, offsets_arr, &get_pixel) {
                        return Some((x, y));
                    }
                }
            }
        }
        // 2 = 右上→左下（x 递减，y 递增）
        2 => {
            for y in y1..=y2 {
                for x in (x1..=x2).rev() {
                    if try_match(x, y, width, height, main_color, threshold, offsets_arr, &get_pixel) {
                        return Some((x, y));
                    }
                }
            }
        }
        // 3 = 右下→左上（x 递减，y 递减）
        3 => {
            for y in (y1..=y2).rev() {
                for x in (x1..=x2).rev() {
                    if try_match(x, y, width, height, main_color, threshold, offsets_arr, &get_pixel) {
                        return Some((x, y));
                    }
                }
            }
        }
        // 4 = 中心向四周；5 = 四周向中心
        4 | 5 => {
            let cx = (x1 + x2) / 2;
            let cy = (y1 + y2) / 2;
            let mut pts: Vec<(i32, i32)> =
                Vec::with_capacity(((x2 - x1 + 1) * (y2 - y1 + 1)) as usize);
            for y in y1..=y2 {
                for x in x1..=x2 {
                    pts.push((x, y));
                }
            }
            pts.sort_by_key(|&(x, y)| {
                let d = (x - cx) * (x - cx) + (y - cy) * (y - cy);
                if direction == 5 { -d } else { d }
            });
            for (x, y) in pts {
                if try_match(x, y, width, height, main_color, threshold, offsets_arr, &get_pixel) {
                    return Some((x, y));
                }
            }
        }
        // 0 及其它 → 左上→右下（默认）
        _ => {
            for y in y1..=y2 {
                for x in x1..=x2 {
                    if try_match(x, y, width, height, main_color, threshold, offsets_arr, &get_pixel) {
                        return Some((x, y));
                    }
                }
            }
        }
    }

    None
}

#[inline(always)]
fn check_offsets<F>(
    x: i32,
    y: i32,
    width: i32,
    height: i32,
    threshold: i32,
    offsets_arr: &[i32],
    get_pixel: &F,
) -> bool
where
    F: Fn(i32, i32) -> u32,
{
    let mut i = 0;
    while i < offsets_arr.len() {
        let dx = offsets_arr[i];
        let dy = offsets_arr[i + 1];
        let color = offsets_arr[i + 2] as u32;

        let tx = x + dx;
        let ty = y + dy;

        if tx < 0 || tx >= width || ty < 0 || ty >= height {
            return false;
        }

        let offset_pixel = get_pixel(tx, ty);
        if !is_color_match(offset_pixel, color, threshold) {
            return false;
        }
        i += 3;
    }
    true
}

#[inline(always)]
fn try_match<F: Fn(i32, i32) -> u32>(
    x: i32,
    y: i32,
    width: i32,
    height: i32,
    main_color: u32,
    threshold: i32,
    offsets_arr: &[i32],
    get_pixel: &F,
) -> bool {
    is_color_match(get_pixel(x, y), main_color, threshold)
        && check_offsets(x, y, width, height, threshold, offsets_arr, get_pixel)
}
