#!/usr/bin/env bash
# Imports Material Symbols (Rounded, 24dp) as vector drawables named ic_symbol_<name>.xml.
# The icons are tinted at the use site (Compose Icon uses LocalContentColor), so the tint attribute is removed.
#
# Usage: scripts/import_material_symbols.sh <name> [<name>...]
# Names are the snake_case icon names from https://fonts.google.com/icons, e.g. "bookmark" or "arrow_back".
# Append ":filled" to import the filled variant as ic_symbol_<name>_filled.xml, e.g. "bookmark:filled".
set -euo pipefail

cd "$(dirname "$0")/.."

base="https://raw.githubusercontent.com/google/material-design-icons/master/symbols/android"

for arg in "$@"; do
    name="${arg%%:*}"

    if [[ "$arg" == *":filled" ]]; then
        target="src/main/res/drawable/ic_symbol_${name}_filled.xml"
        styles=("materialsymbolsrounded/${name}_fill1_24px.xml")
    else
        target="src/main/res/drawable/ic_symbol_${name}.xml"
        styles=("materialsymbolsrounded/${name}_24px.xml" "materialsymbolsrounded/${name}_fill1_24px.xml")
    fi

    if [[ -f "$target" ]]; then
        continue
    fi

    for style in "${styles[@]}"; do
        if curl -sf "$base/$name/$style" -o "$target.tmp"; then
            break
        fi
    done

    if [[ ! -s "$target.tmp" ]]; then
        rm -f "$target.tmp"
        echo "Icon not found: $arg" >&2
        exit 1
    fi

    perl -0pe 's/\s*android:tint="[^"]*"//' "$target.tmp" > "$target"
    rm "$target.tmp"
    echo "Imported $arg"
done
