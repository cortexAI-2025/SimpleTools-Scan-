#!/usr/bin/env bash
# Fails when user-facing text appears to be hard-coded in Kotlin sources.
# All UI copy must live in res/values/strings.xml (EN) + res/values-fr/strings.xml (FR).
# Android Lint already covers XML layouts; this check covers Compose code.
# Files are scanned as a whole, so a literal on the line after `Text(` is caught too.
set -euo pipefail

cd "$(dirname "$0")/.."

mapfile -t files < <(find . -path '*/src/main/*' -name '*.kt' -not -path '*/build/*' -not -path './build-logic/*' | sort)
[[ ${#files[@]} -eq 0 ]] && { echo "No Kotlin sources found."; exit 0; }

matches=$(perl -0777 -ne '
    while (/(\bText\(\s*"|\b(?:text|title|label|summary|message|contentDescription|placeholder|actionLabel)\s*=\s*"[^"\n]*[A-Za-z][^"\n]*")/g) {
        my $line = 1 + (substr($_, 0, $-[0]) =~ tr/\n//);
        (my $snippet = $1) =~ s/\s+/ /g;
        print "$ARGV:$line: $snippet\n";
    }
' "${files[@]}")

if [[ -n "$matches" ]]; then
  echo "Hard-coded UI text found. Move it to string resources (EN + FR):"
  echo "$matches"
  exit 1
fi
echo "No hard-coded UI text found."
