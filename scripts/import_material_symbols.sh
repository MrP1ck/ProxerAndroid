#!/usr/bin/env bash
# Imports Material Symbols (Rounded, 24dp) as vector drawables named ic_symbol_<name>.xml.
# The icons are tinted at the use site (Compose Icon uses LocalContentColor), so the tint attribute is removed.
#
# Usage: scripts/import_material_symbols.sh <name> [<name>...]
# Names are the snake_case icon names from https://fonts.google.com/icons, e.g. "bookmark" or "arrow_back".
set -euo pipefail

cd "$(dirname "$0")/.."

base="https://raw.githubusercontent.com/google/material-design-icons/master/symbols/android"

for name in "$@"; do
    target="src/main/res/drawable/ic_symbol_${name}.xml"

    if [[ -f "$target" ]]; then
        continue
    fi

    for style in "materialsymbolsrounded/${name}_24px.xml" "materialsymbolsrounded/${name}_fill1_24px.xml"; do
        if curl -sf "$base/$name/$style" -o "$target.tmp"; then
            break
        fi
    done

    if [[ ! -s "$target.tmp" ]]; then
        rm -f "$target.tmp"
        echo "Icon not found: $name" >&2
        exit 1
    fi

    perl -0pe 's/\s*android:tint="[^"]*"//' "$target.tmp" > "$target"
    rm "$target.tmp"
    echo "Imported $name"
done
