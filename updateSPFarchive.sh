#!/bin/bash

./gradlew buildJars

ARCHIVE="/home/nat/Repos/bench-defs/archives/spf-verify.zip"
SPF_PARENT="/home/nat/Repos"
SPF="SPF"

rm -f "$ARCHIVE"
cd "$SPF_PARENT" || exit 1
zip -r "$ARCHIVE" "$SPF"
