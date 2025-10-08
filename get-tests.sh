#!/bin/bash

if [[ -z $1 ]]; then
	echo "Usage: $0 <dir-with-.java-tests>"
	exit 1
fi

readarray -t tests < spf-tests.txt
indir=$1
outdir="spf-tests"
mkdir -p "$outdir"

files=()

for file in "$indir"/*.java; do
  found=false
  basename=$(basename "$file" .java)
  for test in "${tests[@]}"; do
    if [[ "$test" == "$basename"* ]]; then
      found=true
      break
    fi
  done
  if [ "$found" = true ]; then
    files+=("$file")
  fi
done

for file in "${files[@]}"; do
  cp "$file" "$outdir/"
done

