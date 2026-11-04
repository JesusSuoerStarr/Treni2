#!/bin/bash
# Добавляет в сгенерированный Android-проект шагомер, разрешения и номер версии.
set -e

PKG_DIR=android/app/src/main/java/com/mytrain/log
mkdir -p "$PKG_DIR"
cp StepsPlugin.java MainActivity.java "$PKG_DIR/"

MANIFEST=android/app/src/main/AndroidManifest.xml
if ! grep -q ACTIVITY_RECOGNITION "$MANIFEST"; then
  sed -i 's#<application#<uses-permission android:name="android.permission.ACTIVITY_RECOGNITION" />\n    <application#' "$MANIFEST"
fi

# Каждая сборка получает новый versionCode, чтобы ставиться поверх предыдущей
sed -i "s/versionCode 1/versionCode ${GITHUB_RUN_NUMBER:-1}/" android/app/build.gradle

# Иконка приложения: icon.png (квадрат) и icon-foreground.png (гантель на прозрачном фоне)
if [ -f icon.png ] && [ -f icon-foreground.png ]; then
  command -v convert >/dev/null || sudo apt-get install -y imagemagick >/dev/null
  RES=android/app/src/main/res
  gen() { # плотность размер_значка размер_переднего_плана
    local D=$1 S=$2 F=$3 C=$(( $2 / 2 ))
    convert icon.png -resize ${S}x${S} "$RES/mipmap-$D/ic_launcher.png"
    convert -size ${S}x${S} xc:black -fill white -draw "circle $C,$C $C,0" /tmp/mask.png
    convert "$RES/mipmap-$D/ic_launcher.png" /tmp/mask.png -alpha off -compose CopyOpacity -composite "$RES/mipmap-$D/ic_launcher_round.png"
    convert icon-foreground.png -resize ${F}x${F} "$RES/mipmap-$D/ic_launcher_foreground.png"
  }
  gen mdpi 48 108; gen hdpi 72 162; gen xhdpi 96 216; gen xxhdpi 144 324; gen xxxhdpi 192 432
  sed -i 's/#FFFFFF/#B85C3C/' "$RES/values/ic_launcher_background.xml"
  echo "Icon applied"
fi

echo "Native files applied"
