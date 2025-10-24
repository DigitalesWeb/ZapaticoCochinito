#!/bin/bash
# Create simple placeholder PNG icons using ImageMagick (if available) or a fallback
for dir in app/src/main/res/mipmap-{m,h,xh,xxh,xxxh}dpi; do
    # For now, we'll create a reference file that indicates icons should be generated
    touch "$dir/ic_launcher.png"
    touch "$dir/ic_launcher_round.png"
done
