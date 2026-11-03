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

echo "Native files applied"
