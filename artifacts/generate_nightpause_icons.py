#!/usr/bin/env python3
"""Draw a four-option Night Pause icon contact sheet using only Python stdlib."""

from math import ceil, hypot
from pathlib import Path
import struct
import zlib

SCALE = 3
ICON = 256
GAP = 22
MARGIN = 18
SHEET = MARGIN * 2 + ICON * 2 + GAP
OUT = Path(__file__).with_name("nightpause-icon-options.png")


def rgb(hex_color):
    value = hex_color.lstrip("#")
    return tuple(int(value[i:i + 2], 16) for i in (0, 2, 4))


class Canvas:
    def __init__(self, width, height, color):
        self.w, self.h = width * SCALE, height * SCALE
        self.data = bytearray(rgb(color) * (self.w * self.h))

    def dot(self, x, y, color):
        x, y = int(x), int(y)
        if 0 <= x < self.w and 0 <= y < self.h:
            i = (y * self.w + x) * 3
            self.data[i:i + 3] = bytes(color)

    def rect(self, x0, y0, x1, y1, color):
        self.round_rect(x0, y0, x1, y1, 0, color)

    def round_rect(self, x0, y0, x1, y1, radius, color):
        c = rgb(color) if isinstance(color, str) else color
        left, top = max(0, int(x0 * SCALE)), max(0, int(y0 * SCALE))
        right, bottom = min(self.w, int(x1 * SCALE)), min(self.h, int(y1 * SCALE))
        r = radius * SCALE
        for py in range(top, bottom):
            y = py + 0.5
            cy = min(max(y, top + r), bottom - r)
            for px in range(left, right):
                x = px + 0.5
                cx = min(max(x, left + r), right - r)
                if (x - cx) ** 2 + (y - cy) ** 2 <= r * r:
                    self.dot(px, py, c)

    def circle(self, cx, cy, radius, color):
        c = rgb(color) if isinstance(color, str) else color
        r = radius * SCALE
        left, right = max(0, int((cx - radius) * SCALE)), min(self.w, int((cx + radius) * SCALE) + 1)
        top, bottom = max(0, int((cy - radius) * SCALE)), min(self.h, int((cy + radius) * SCALE) + 1)
        r2 = r * r
        for py in range(top, bottom):
            dy = (py + 0.5) - cy * SCALE
            for px in range(left, right):
                dx = (px + 0.5) - cx * SCALE
                if dx * dx + dy * dy <= r2:
                    self.dot(px, py, c)

    def stroke(self, points, width, color):
        r = width / 2
        for (x0, y0), (x1, y1) in zip(points, points[1:]):
            length = hypot(x1 - x0, y1 - y0)
            steps = max(1, ceil(length * SCALE / (r * SCALE)))
            for i in range(steps + 1):
                t = i / steps
                self.circle(x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, r, color)

    def polygon(self, points, color):
        c = rgb(color) if isinstance(color, str) else color
        left = max(0, int(min(p[0] for p in points) * SCALE))
        right = min(self.w, int(max(p[0] for p in points) * SCALE) + 1)
        top = max(0, int(min(p[1] for p in points) * SCALE))
        bottom = min(self.h, int(max(p[1] for p in points) * SCALE) + 1)
        scaled = [(x * SCALE, y * SCALE) for x, y in points]
        for py in range(top, bottom):
            y = py + 0.5
            for px in range(left, right):
                x = px + 0.5
                inside = False
                j = len(scaled) - 1
                for i, (xi, yi) in enumerate(scaled):
                    xj, yj = scaled[j]
                    if (yi > y) != (yj > y) and x < (xj - xi) * (y - yi) / (yj - yi) + xi:
                        inside = not inside
                    j = i
                if inside:
                    self.dot(px, py, c)

    def downsample(self):
        nw, nh = self.w // SCALE, self.h // SCALE
        out = bytearray(nw * nh * 3)
        area = SCALE * SCALE
        for y in range(nh):
            for x in range(nw):
                sums = [0, 0, 0]
                for sy in range(SCALE):
                    for sx in range(SCALE):
                        i = (((y * SCALE + sy) * self.w) + x * SCALE + sx) * 3
                        sums[0] += self.data[i]
                        sums[1] += self.data[i + 1]
                        sums[2] += self.data[i + 2]
                j = (y * nw + x) * 3
                out[j:j + 3] = bytes(v // area for v in sums)
        return nw, nh, out


def crescent(canvas, cx, cy, radius, color, cutout, dx, dy, cut_radius=None):
    canvas.circle(cx, cy, radius, color)
    canvas.circle(cx + dx, cy + dy, cut_radius or radius * 0.88, cutout)


def icon_moon_pause(c, ox, oy):
    bg, moon = "#171E50", "#DCD5FF"
    c.round_rect(ox, oy, ox + ICON, oy + ICON, 42, bg)
    crescent(c, ox + 106, oy + 116, 76, moon, bg, 31, -27, 67)
    c.round_rect(ox + 127, oy + 85, ox + 146, oy + 151, 9, moon)
    c.round_rect(ox + 157, oy + 85, ox + 176, oy + 151, 9, moon)
    c.circle(ox + 191, oy + 58, 4, "#AAA0EE")


def icon_moon_podcast(c, ox, oy):
    bg, mint = "#0C4143", "#B8F4D5"
    c.round_rect(ox, oy, ox + ICON, oy + ICON, 42, bg)
    crescent(c, ox + 122, oy + 84, 55, mint, bg, 24, -18, 49)
    # Podcast/sound wave: a calm, symmetric rhythm rather than a busy waveform.
    bars = [(ox + 57, 18), (ox + 81, 34), (ox + 105, 51), (ox + 129, 63),
            (ox + 153, 51), (ox + 177, 34), (ox + 201, 18)]
    for x, height in bars:
        c.round_rect(x - 5, oy + 203 - height, x + 5, oy + 203 + height, 5, mint)


def icon_pillow_pause(c, ox, oy):
    bg, cream, ink = "#382341", "#FFF0CC", "#4B2C4F"
    c.round_rect(ox, oy, ox + ICON, oy + ICON, 42, bg)
    # Soft pillow silhouette with a quiet inset seam.
    c.round_rect(ox + 35, oy + 66, ox + 221, oy + 195, 34, cream)
    c.stroke([(ox + 52, oy + 84), (ox + 43, oy + 115), (ox + 52, oy + 177)], 3, "#E4D2AE")
    c.stroke([(ox + 204, oy + 84), (ox + 213, oy + 115), (ox + 204, oy + 177)], 3, "#E4D2AE")
    crescent(c, ox + 111, oy + 130, 42, ink, cream, 18, -15, 37)
    c.round_rect(ox + 133, oy + 111, ox + 143, oy + 151, 5, ink)
    c.round_rect(ox + 150, oy + 111, ox + 160, oy + 151, 5, ink)
    c.circle(ox + 128, oy + 48, 4, cream)


def icon_watch_moon(c, ox, oy):
    bg, blue = "#0E2B58", "#B6C8FF"
    c.round_rect(ox, oy, ox + ICON, oy + ICON, 42, bg)
    # Bands sit behind a clear, round watch case.
    c.round_rect(ox + 91, oy + 16, ox + 165, oy + 81, 18, blue)
    c.round_rect(ox + 91, oy + 175, ox + 165, oy + 240, 18, blue)
    c.round_rect(ox + 57, oy + 57, ox + 199, oy + 199, 47, blue)
    c.round_rect(ox + 68, oy + 68, ox + 188, oy + 188, 39, "#173B75")
    crescent(c, ox + 113, oy + 127, 44, blue, "#173B75", 19, -15, 38)
    c.round_rect(ox + 127, oy + 109, ox + 138, oy + 148, 5, blue)
    c.round_rect(ox + 145, oy + 109, ox + 156, oy + 148, 5, blue)
    c.round_rect(ox + 198, oy + 108, ox + 211, oy + 148, 6, blue)


def png_bytes(width, height, pixels):
    def chunk(kind, payload):
        body = kind + payload
        return struct.pack(">I", len(payload)) + body + struct.pack(">I", zlib.crc32(body) & 0xFFFFFFFF)

    rows = bytearray()
    stride = width * 3
    for y in range(height):
        rows.append(0)
        rows.extend(pixels[y * stride:(y + 1) * stride])
    return (b"\x89PNG\r\n\x1a\n"
            + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 2, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(bytes(rows), 9))
            + chunk(b"IEND", b""))


def main():
    c = Canvas(SHEET, SHEET, "#10131B")
    positions = [(MARGIN, MARGIN), (MARGIN + ICON + GAP, MARGIN),
                 (MARGIN, MARGIN + ICON + GAP), (MARGIN + ICON + GAP, MARGIN + ICON + GAP)]
    for draw, (x, y) in zip((icon_moon_pause, icon_moon_podcast, icon_pillow_pause, icon_watch_moon), positions):
        draw(c, x, y)
    width, height, pixels = c.downsample()
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_bytes(png_bytes(width, height, pixels))
    print(f"Wrote {OUT} ({width}×{height})")


if __name__ == "__main__":
    main()
