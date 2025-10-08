#!/bin/bash
#
if [[ -z $1 ]]; then
	echo "Usage: $0 <log-directory>"
	echo "Script output filenames that contains $$$$, and therefore seem to call a string solver"
	exit 1
fi

logdir=$1

shopt -s nullglob
for log in "$logdir"/*MAS.log; do
  if grep -q '\$\$\$\$' "$log"; then
    file=$(basename "$log")
    echo "${file//___MAS.log/}"
  fi
done
shopt -u nullglob